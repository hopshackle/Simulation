package hopshackle.simulation.metric;

import hopshackle.simulation.ConnectionFactory;

import java.sql.*;
import java.util.logging.Logger;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.stat.inference.TTestImpl;

public class MySQLResultSet {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	/**
	 * the content of dsGroup is the names of the DataSetGroups
	 * 
	 * The content of column1 is the names of the DataSets [DataSetGroup][DataSet]
	 * 
	 * The content of column2 is the names of the Metrics
	 * 
	 *  The content of values is the actual values for [DataSetGroup][DataSet][Metric]
	 * 
	 * This will populate the DataSetGroupAnalysis table in the SimAnalysis database, 
	 * using root user and Metternich as password
	 * 
	 * @param column1
	 * @param column2
	 * @param values
	 */
	public MySQLResultSet(String database, String[] dsGroup, String[][] column1, String[] column2, double[][][] values) throws SQLException {

		/*
		 * First we create table if it does not already exist.
		 * 
		 * Secondly we delete any lines on the table that were previously present for this
		 * DataSetGroup TODO: extend later to be specific to Datasets and Metrics
		 * 
		 * Thirdly, iterate over each DataSet, and insert values into table
		 *
		 */

		if (database == null || database == "") {
			database = "SimAnalysis";
		}
		Connection con = ConnectionFactory.getConnection(database, "root", "Metternich", "", false);

		try {

			String sqlQuery = "CREATE TABLE IF NOT EXISTS Datasetgroupanalysis " +
			" (	datasetgroup	VARCHAR(50)	NOT NULL,"	+
			" 	dataset			VARCHAR(50)	NOT NULL,"	+
			"	metric			VARCHAR(50) NOT NULL,"	+
			"	value			DOUBLE		NOT NULL"	+
			");";

			Statement st = con.createStatement();
			st.executeUpdate(sqlQuery);

			sqlQuery = "CREATE TABLE IF NOT EXISTS dsgSummaryanalysis " +
			" (	datasetgroup	VARCHAR(50)	NOT NULL,"	+
			" 	metric			VARCHAR(50)	NOT NULL,"	+
			"	mean			DOUBLE		NOT NULL,"	+
			"	variance		DOUBLE		NOT NULL,"	+
			" 	stddev			DOUBLE		NOT NULL"	+
			");";

			st = con.createStatement();
			st.executeUpdate(sqlQuery);

			sqlQuery = "CREATE TABLE IF NOT EXISTS sigAnalysis " +
			" (	datasetgroup1	VARCHAR(50)	NOT NULL,"	+
			" 	datasetgroup2	VARCHAR(50)	NOT NULL,"	+
			"	metric			VARCHAR(50)	NOT NULL,"	+
			"	significance	DOUBLE		NOT NULL"	+
			");";

			st = con.createStatement();
			st.executeUpdate(sqlQuery);

			for (int n=0; n<dsGroup.length; n++) {
				sqlQuery = "DELETE FROM dsgSummaryAnalysis WHERE datasetgroup = '" + dsGroup[n] + "';";
				st.executeUpdate(sqlQuery);
				sqlQuery = "DELETE FROM Datasetgroupanalysis WHERE datasetgroup = '" + dsGroup[n] + "';";
				st.executeUpdate(sqlQuery);
			}
			StringBuffer temp = new StringBuffer(" IN (");
			for (int n=0; n<dsGroup.length; n++) 
				temp.append("'" + dsGroup[n] + "',");
			temp.setCharAt(temp.length()-1, ')');

			sqlQuery = "DELETE FROM siganalysis WHERE datasetgroup1 " + temp.toString() +
			" AND datasetgroup2 " + temp.toString() +";";
			st.executeUpdate(sqlQuery);


			for (int metricLoop=0; metricLoop<column2.length; metricLoop++) {
				SummaryStatistics[] metricStatSummary = new SummaryStatistics[dsGroup.length];

				for (int dsgLoop=0; dsgLoop<dsGroup.length; dsgLoop++) {	
					metricStatSummary[dsgLoop] = new SummaryStatistics();
					temp = new StringBuffer("INSERT INTO datasetgroupanalysis (datasetgroup, dataset, metric, value) VALUES");

					for (int dsLoop=0; dsLoop<column1[dsgLoop].length; dsLoop++) {
						if (dsLoop>0) temp.append(",");
						temp.append(" ('" + dsGroup[dsgLoop] + "', '" + column1[dsgLoop][dsLoop] + "', '" + column2[metricLoop] + 
								"', " + values[dsgLoop][dsLoop][metricLoop] +")");
						metricStatSummary[dsgLoop].addValue(values[dsgLoop][dsLoop][metricLoop]);
					}
					temp.append(";");
					st.executeUpdate(temp.toString());

					String temp2 = "INSERT INTO dsgSummaryAnalysis (datasetgroup, metric, mean, variance, stddev) VALUES" +
					" ('" + dsGroup[dsgLoop] + "', '" + column2[metricLoop] + "', " + metricStatSummary[dsgLoop].getMean() +
					", " + metricStatSummary[dsgLoop].getVariance() + ", " + metricStatSummary[dsgLoop].getStandardDeviation() + ");";

					st.executeUpdate(temp2);
				}

				/*
				 *  Now we compare the results for this Metric between each DataSetGroup
				 */
				TTestImpl inferenceSummary = new TTestImpl();
				for (int n=0; n<dsGroup.length; n++) {
					for (int m=n+1; m<dsGroup.length; m++) {
						if (notEmpty(metricStatSummary[n]) || notEmpty(metricStatSummary[m])) {
							double sigLevel = inferenceSummary.tTest(metricStatSummary[n], metricStatSummary[m]);
							if (((Double)sigLevel).equals(Double.NaN)) sigLevel = 0;
							String temp3 = "INSERT INTO sigAnalysis (datasetgroup1, datasetgroup2, metric, significance) VALUES " +
							" ('" + dsGroup[n] + "', '" + dsGroup[m] + "', '" + column2[metricLoop] + "', " + sigLevel + ");";
							st.executeUpdate(temp3);
						}
					}
				}
			}

			st.close();
		} catch (SQLException e) {
			logger.severe(e.toString());
			throw e;
		} catch (MathException e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}

		/* ideally want to change this to the ideal constructor:
		 * dbName, user, password, tableName, columnNames[], columnTypes[], columnValues[][]
		 * 
		 * where columnValues is an array of Object with first index = column, second index = row
		 */

	}
	
	private boolean notEmpty(SummaryStatistics toCheck) {
		if (toCheck.getStandardDeviation() < 0.0000001)
			return false;
		
		return true;
	}
}
