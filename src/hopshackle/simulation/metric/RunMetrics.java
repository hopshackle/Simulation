package hopshackle.simulation.metric;

import hopshackle.simulation.SimProperties;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

public class RunMetrics {

	private File dir;
	private List<DataSetGroup> dsgArray;
	private List<Metric> metricArray;

	/**
	 * The first argument may optionally be a directory location to provide input.
	 * 
	 * If this is not specified, then the default C:/Simulations/Metrics is used instead.
	 * 
	 * This directory must contain at least one file of each of types:
	 * 			*.sql
	 * 			*.ds
	 * 
	 * Each sql file is used to generate a Metric object, with a name equal to the file name (excluding the sql suffix)
	 * The sql file should contain a valid MySQL query that returns a DOUBLE (and only a DOUBLE) that is the metric we wish to track
	 * This sql may have one variable in the query, which should be references as "&Table"
	 * when actually executed, this string will be replaced by the relevant entry in the ds file
	 * 
	 * Each ds file is used to generate a DataSetGroup, with name equal to the file name.
	 * The ds file should contain a number of Strings, one on each line of a plan .txt file.
	 * These strings will be used to replace the "&Table" references in the sql files defining the various metrics
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new RunMetrics(args);
	}

	public RunMetrics(String[] args) {
		
		String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
		File defaultDir = new File(baseDir + "\\Metrics");
		if (args.length > 0 && args[0] != null) {
			dir = new File(args[0]);
			System.out.println("Using " + dir + " to define datasets");
		}

		if (dir == null) dir = defaultDir;

		ArrayList<File> sqlFiles = new ArrayList<File>();

		dsgArray = DataSetGroup.getDataSets(dir);
		metricArray = new ArrayList<Metric>();

		if (args.length > 1 && args[1] != null) {
			dir = new File(args[1]);
		}
		if (!dir.isDirectory()) {
			dir = defaultDir;
		}
		System.out.println("Using " + dir + " for .sql files");
		File[] allFiles = dir.listFiles();
		for (int n=0; n<allFiles.length; n++) 
			if (allFiles[n].getName().endsWith(".sql"))
				sqlFiles.add(allFiles[n]);

		for (File sqlFile : sqlFiles) {
			String metricName = sqlFile.getName().substring(0, sqlFile.getName().length()-4);
			String sqlQuery = extractSQLFromFile(sqlFile);
			metricArray.add(new MySQLMetric(metricName, sqlQuery));
		}

		/* 
		 *  Now we have the Metrics and the DataSetGroups
		 */

		int nMetric = metricArray.size();
		int nDataSetGroup = dsgArray.size();

		ArrayList<String[]> dsNames = new ArrayList<String[]>();
		String[] metricName = new String[nMetric];
		ArrayList<double[][]> dsValues = new ArrayList<double[][]>();

		for (int loop = 0; loop<nDataSetGroup; loop++) {
			DataSetGroup currentDSG = dsgArray.get(loop);
			DataSet[] dsArray = getArrayFromListDS(currentDSG.getArrayList());
			int nDataSet = dsArray.length;

			String[] dsName = new String[nDataSet];
			double[][] metricValue = new double[nDataSet][nMetric];

			for (int loop1 = 0; loop1<nDataSet; loop1++) {
				dsName[loop1] = dsArray[loop1].toString();
				for (int loop2 = 0; loop2<nMetric; loop2++) {
					metricName[loop2] = metricArray.get(loop2).toString();
					metricValue[loop1][loop2] = metricArray.get(loop2).getResult(dsArray[loop1]);
				}
			}

			dsNames.add(dsName);
			dsValues.add(metricValue);
		}

		System.out.println("Extracted " + dsValues.size() + " names of dataset groups");
		/*
		 * Have now obtained all the values for the current DataSetGroup
		 *  We pass on to a Persistence object to write them away
		 */
		try {
			new MySQLResultSet(null, getArrayFromListDSG(dsgArray),
					getArrayFromListString(dsNames), metricName, 
					getArrayFromListDouble(dsValues));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static String extractSQLFromFile(File file) {
		StringBuffer sqlQuery = new StringBuffer();
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);

			String t = null;
			do {
				t = br.readLine();
				if (t != null)
					sqlQuery.append(t + " ");
			} while (t != null);

			br.close();
			fr.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return sqlQuery.toString();
	}

	private DataSet[] getArrayFromListDS(ArrayList<DataSet> al) {
		DataSet[] retValue = new DataSet[al.size()];
		for (int n=0; n<retValue.length; n++) 
			retValue[n]=al.get(n);

		return retValue;
	}
	private String[] getArrayFromListDSG(List<DataSetGroup> al) {
		String[] retValue = new String[al.size()];
		for (int n=0; n<retValue.length; n++) 
			retValue[n]=al.get(n).toString();

		return retValue;
	}
	private double[][][] getArrayFromListDouble(List<double[][]> al) {
		double[][][] retValue = new double[al.size()][][];
		for (int n=0; n<retValue.length; n++) 
			retValue[n]=al.get(n);

		return retValue;
	}
	private String[][] getArrayFromListString(List<String[]> al) {
		String[][] retValue = new String[al.size()][];
		for (int n=0; n<retValue.length; n++) 
			retValue[n]=al.get(n);

		return retValue;
	}

}
