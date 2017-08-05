package hopshackle.simulation;

import java.net.SocketException;
import java.sql.*;
import java.util.*;
import java.io.*;

import hopshackle.simulation.metric.*;
import jgpml.GaussianProcess;
import jgpml.covariancefunctions.*;
import Jama.*;
import org.apache.commons.math3.analysis.function.Gaussian;

/**
 * Created by james on 31/07/2017.
 */
public class ParameterSearch {

    private String name;
    private int count = 0;
    private Properties propertiesToSearch;
    private List<ParameterDetail> parameterConstraints = new ArrayList<>();
    private long startTime;
    private GaussianProcess gp;
    private double[] Xmean;
    private double Ymean;
    private Connection con;

    public ParameterSearch(String name) {
        this.name = name;
        propertiesToSearch = SimProperties.specificProperties("ParameterSearch");
        for (Object k : propertiesToSearch.keySet()) {
            String key = (String) k;
            String value = propertiesToSearch.getProperty(key);
            int hyphenIndex = value.indexOf("-"); // -1 if none
            int pipeIndex = value.indexOf("|");  // -1 if none
            if (hyphenIndex > -1) {
                double from = Double.valueOf(value.substring(0, hyphenIndex));
                double to = Double.valueOf(value.substring(hyphenIndex + 1));
                parameterConstraints.add(new ParameterDetail(key, from, to));
            }
            if (pipeIndex > -1) {
                String[] values = value.split("\\|");
                parameterConstraints.add(new ParameterDetail(key, Arrays.asList(values)));
            }
            if (pipeIndex > -1 && hyphenIndex > -1) {
                throw new AssertionError("Invalid format for parameter search: " + key + ", " + value);
            }
        }
        con = ConnectionFactory.getConnection("SimAnalysis", "root", "Metternich", "", false);
    }

    public void setParameterSearchValues() {
        // TODO: Currently just apply GP to continuous variables. Categorical ones to be added later.
        startTime = System.currentTimeMillis();
        count = getNumberOfPreviousRuns();
        if (count <= 20)
            generateParametersRandomly();
        else
            generateParametersFromGP();

        for (ParameterDetail pd : parameterConstraints) {
            String value = SimProperties.getProperty(pd.name, "");
            System.out.println(String.format("Parameter %s has value %s", pd.name, value));
        }
    }

    private void generateParametersRandomly() {
        double[] baseValues = randomParameterValues();
        for (int i = 0; i < baseValues.length; i++) {
            ParameterDetail pd = parameterConstraints.get(i);
            String stringRepresentation = "";
            if (pd.continuous) {
                stringRepresentation = parameterValueToString(baseValues[i], pd);
            } else {
                stringRepresentation = pd.categoricalValues.get((int) baseValues[i]);
            }
            SimProperties.setProperty(pd.name, stringRepresentation);
        }
    }

    /*
    categorical variables are an index from 0 to N-1
    continuous ones are on a logScale where appropriate
     */
    private double[] randomParameterValues() {
        double[] retValue = new double[parameterConstraints.size()];
        for (int i = 0; i < parameterConstraints.size(); i++) {
            ParameterDetail pd = parameterConstraints.get(i);
            if (pd.continuous) {
                retValue[i] = Math.random() * (pd.toValue - pd.fromValue) + pd.fromValue;
            } else {
                retValue[i] = Dice.roll(1, pd.categoricalValues.size()) - 1;
            }
        }
        return retValue;
    }

    private String parameterValueToString(double value, ParameterDetail pd) {
        if (pd.logScale) value = Math.exp(value);
        if (pd.integer) {
            int intValue = (int) (value + 0.5);
            return String.valueOf(intValue);
        } else {
            return String.format("%.4g", value);
        }
    }

    private void generateParametersFromGP() {
        trainGPFromDataInTable();
        double[] parameters = getNextPointToTry();
        for (int i = 0; i < parameters.length; i++) {
            ParameterDetail pd = parameterConstraints.get(i);
            SimProperties.setProperty(pd.name, parameterValueToString(parameters[i], pd));
        }
    }

    private int getNumberOfPreviousRuns() {
        int retValue = 0;

        try {
            String sqlQuery = "SELECT COUNT(*) FROM PS_" + name + ";";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sqlQuery);
            rs.next();
            retValue = rs.getInt(1);
            //        System.out.println(sqlQuery + " give " + retValue);
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            retValue = 0;
        }

        return retValue;
    }

    private void trainGPFromDataInTable() {
        // Form X and Y from database table
        List<List<Double>> protoX = new ArrayList<>();
        List<Double> protoY = new ArrayList<>();
        try {
            String sqlQuery = "SELECT * FROM PS_" + name + ";";

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sqlQuery);

            do {
                List<Double> rowData = new ArrayList<>();
                rs.next();
                for (ParameterDetail pd : parameterConstraints) {
                    if (!pd.continuous)
                        throw new AssertionError("GP not yet working for categorical parameters");
                    double value = pd.integer ? rs.getInt(pd.name) : rs.getDouble(pd.name);
                    rowData.add(value);
                }
                protoX.add(rowData);

                double score = rs.getDouble("score");
                protoY.add(score);
            } while (!rs.isLast());
            st.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new AssertionError("Invalid SQL");
        }
        // protoX and protoY now populated with raw data
        Xmean = new double[protoX.get(0).size()];
        Ymean = 0.0;
        double[][] X = new double[protoX.size()][protoX.get(0).size()];
        double[][] Y = new double[protoY.size()][1];
        for (int i = 0; i < Y.length; i++) {
            Y[i][0] = protoY.get(i);
            Ymean += Y[i][0];
            List<Double> temp = protoX.get(i);
            for (int j = 0; j < parameterConstraints.size(); j++) {
                X[i][j] = temp.get(j);
                Xmean[j] += X[i][j];
            }
        }
        Ymean /= protoY.size();
        for (int i = 0; i < Xmean.length; i++)
            Xmean[i] /= protoY.size();

        // zero mean all the data
        for (int i = 0; i < Y.length; i++) {
            Y[i][0] -= Ymean;
            for (int j = 0; j < parameterConstraints.size(); j++) {
                X[i][j] -= Xmean[j];
            }
        }

        CovSum kernel = null;
        if (count > 50) {
            kernel = new CovSum(parameterConstraints.size(), new CovSEard(parameterConstraints.size()), new CovLINard(parameterConstraints.size()), new CovNoise());
            System.out.println("Using full kernel");
        } else if (count % 2 == 0) {
            kernel = new CovSum(parameterConstraints.size(), new CovLINard(parameterConstraints.size()), new CovNoise());
            System.out.println("Using linear kernel");
        } else {
            kernel = new CovSum(parameterConstraints.size(), new CovSEard(parameterConstraints.size()), new CovNoise());
            System.out.println("Using squared exponential kernel");
        }
        int kernelParameters = kernel.numParameters();  // last is always noise level
        gp = new GaussianProcess(kernel);

        double[][] theta = new double[kernelParameters][1];
        // train GP
        gp.train(new Matrix(X), new Matrix(Y), new Matrix(theta), 20);
    }

    private double[] getNextPointToTry() {
        // we generate a load of random points in the space, and then evaluate them all
        int N = (int) Math.pow(50, parameterConstraints.size());
        double[][] xstar = new double[N][parameterConstraints.size()];
        for (int i = 0; i < N; i++) {
            double[] sample = randomParameterValues();
            for (int j = 0; j < sample.length; j++)
                sample[j] -= Xmean[j];
            xstar[i] = sample;
        }
        // noise parameter is always the last one given kernel construction
        double baseNoise = Math.pow(Math.exp(gp.logtheta.get(gp.logtheta.getRowDimension() - 1, 0)), 2);
        System.out.println(String.format("Base noise is %.3g (sd: %.3g)", baseNoise, Math.sqrt(baseNoise)));
        System.out.println("All params: " + HopshackleUtilities.formatArray(gp.logtheta.transpose().getArray()[0], ", ", "%.2g"));
        Matrix[] predictions = gp.predict(new Matrix(xstar));
        double bestScore = Double.NEGATIVE_INFINITY;
        double bestEstimate = 0.0;
        double[] nextSetting = new double[parameterConstraints.size()];
        int negNoise = 0;
        for (int i = 0; i < N; i++) {
            double value = predictions[0].get(i, 0);
            double latentNoise = predictions[1].get(i, 0) - baseNoise;
            if (latentNoise < 0.00) {
                latentNoise = 0.00;
                negNoise++;
            }
            double score = value + 2.0 * latentNoise;
            if (score > bestScore) {
                bestScore = score;
                bestEstimate = value;
                nextSetting = xstar[i];
            }
        }
        System.out.println(String.format("Best estimated mean is %.3f above objective mean of %.3f (%.3f with latent noise)", bestEstimate, Ymean, bestScore));
        if (negNoise > 0) System.out.println(negNoise + " samples had negative latent noise");
        for (int i = 0; i < nextSetting.length; i++)
            nextSetting[i] += Xmean[i];
        return nextSetting;
    }

    public void calculateAndWriteScore(List<String> tableNames) {
        double runningTotal = 0.0;
        double timeTaken = (System.currentTimeMillis() - startTime) / 1000.0;
        String sqlFile = SimProperties.getProperty("ParameterSearchObjectiveFunction", "");
        if (sqlFile.equals("")) throw new AssertionError("Need to specify ParameterSearchObjectiveFunction");
        String sql = RunMetrics.extractSQLFromFile(new File(sqlFile));
        System.out.println(sql);
        for (String t : tableNames) {
            MySQLMetric sqlMetric = new MySQLMetric("PS_" + t, sql);
            double value = sqlMetric.getResult(new MySQLDataSet(t));
            System.out.println(t + " : " + value);
            runningTotal += value;
        }
        double score = runningTotal / tableNames.size();
        // now to write to table with the parameter value used

        String columnNameCreation = "";
        String columnNameInsertion = "";
        String parameterValues = "";
        for (ParameterDetail pd : parameterConstraints) {
            String columnType = " VARCHAR(50) NOT NULL, ";
            if (pd.continuous) {
                if (pd.integer) columnType = " INTEGER NOT NULL,  ";
                else columnType = " DOUBLE NOT NULL, ";
            }
            columnNameCreation = columnNameCreation + pd.name + columnType;
            columnNameInsertion = columnNameInsertion + pd.name + ",";
            if (pd.continuous) {
                parameterValues = parameterValues + SimProperties.getPropertyAsDouble(pd.name, "0.00") + ",";
            } else {
                parameterValues = parameterValues + SimProperties.getProperty(pd.name, "") + ",";
            }
        }
        try {

            String sqlQuery = "CREATE TABLE IF NOT EXISTS PS_" + name +
                    " ( " + columnNameCreation +
                    " score	        DOUBLE  	NOT NULL, " +
                    " timeTaken     DOUBLE      NOT NULL" +
                    ");";

            Statement st = con.createStatement();
            st.executeUpdate(sqlQuery);

            sqlQuery = "INSERT INTO PS_" + name + " (" + columnNameInsertion + " score, timeTaken) VALUES " +
                    "( " + parameterValues + score + ", " + timeTaken + ")";

            st = con.createStatement();
            st.executeUpdate(sqlQuery);



        } catch (SQLException e) {
            e.printStackTrace();
            throw new AssertionError("Invalid SQL");
        }
    }

    public boolean complete() {
        return false;
    }

}

class ParameterDetail {

    String name;
    boolean continuous;
    boolean logScale;
    boolean integer;
    double fromValue, toValue;
    List<String> categoricalValues;

    public ParameterDetail(String parameter, double from, double to) {
        name = parameter;
        continuous = true;
        if (from - (int) from < 0.0001 && (to - (int) to) < 0.0001)
            integer = true;
        if (to / from > 10e2 || from / to > 10e2) {
            if (to < 0.0) throw new AssertionError("Negative values on logScale not yet supported");
            logScale = true;
            fromValue = Math.log(from);
            toValue = Math.log(to);
        } else {
            logScale = false;
            fromValue = from;
            toValue = to;
        }
    }

    public ParameterDetail(String parameter, List<String> values) {
        name = parameter;
        continuous = false;
        categoricalValues = HopshackleUtilities.cloneList(values);
    }
}