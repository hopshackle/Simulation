package hopshackle.simulation.metric;

import hopshackle.simulation.*;

import java.io.*;
import java.util.*;

public class DataSetGroup {

	private DataSet[] dataSetArray;
	private String name;
	
	public DataSetGroup(String name, DataSet[] dsArray) {
		dataSetArray = dsArray;
		this.name = name;
	}
	
	public ArrayList<DataSet> getArrayList() {
		ArrayList<DataSet> retValue = new ArrayList<DataSet>();
		
		for (int n=0; n<dataSetArray.length; n++) 
			retValue.add(dataSetArray[n]);
		
		return retValue;
	}
	
	public String toString() {return name;}
	
	public static List<DataSetGroup> getDataSets(File dir) {
		if (dir.isDirectory()) {
			return getDataSetsFromDSFiles(dir);
		} else {
			return getDataSetsFromFile(dir);
		}
	}
	
	public static List<DataSetGroup> getDataSetsFromFile(File dir) {
		Map<String, DataSetGroup> dsgArray = new HashMap<String, DataSetGroup>();
		
		List<String> fileEntries = HopshackleUtilities.createListFromFile(dir);
		for (String entry : fileEntries) {
			String[] params = entry.split(" ");
			String datasetName = params[0];
			if (datasetName.startsWith("#")) continue;
			String dbStem = params[1];
			int startRef = Integer.valueOf(params[2]);
			int endRef = Integer.valueOf(params[3]);
			
			ArrayList<DataSet> temp = new ArrayList<DataSet>();
			for (int i = startRef; i <= endRef; i++) {
				temp.add(new MySQLDataSet(dbStem + i));
			}
			
			if (dsgArray.containsKey(datasetName)) {
				temp.addAll(dsgArray.get(datasetName).getArrayList());
				dsgArray.put(datasetName, new DataSetGroup(datasetName, getArrayFromArrayList(temp)));
			} else {
				dsgArray.put(datasetName, new DataSetGroup(datasetName, getArrayFromArrayList(temp)));
			}
		}

		List<DataSetGroup> retValue = new ArrayList<DataSetGroup>();
		for (DataSetGroup dsg : dsgArray.values()) {
			retValue.add(dsg);
		}
		return retValue;
	}

	public static List<DataSetGroup> getDataSetsFromDSFiles(File dir) {
		ArrayList<File> dsFiles = new ArrayList<File>();
		ArrayList<DataSetGroup> dsgArray = new ArrayList<DataSetGroup>();
		
		File[] allFiles = dir.listFiles();
		for (int n=0; n<allFiles.length; n++) {
			if (allFiles[n].getName().endsWith(".ds"))
				dsFiles.add(allFiles[n]);
		}

		for (File dsFile : dsFiles) {
			String dsgName = dsFile.getName().substring(0, dsFile.getName().length()-3);
			ArrayList<DataSet> temp = new ArrayList<DataSet>();
			String dsName = null;

			try {
				FileReader fr = new FileReader(dsFile);
				BufferedReader br = new BufferedReader(fr);

				do {
					dsName = br.readLine();

					if (dsName != null) 
						temp.add(new MySQLDataSet(dsName));
				} while (dsName != null);

				br.close();
				fr.close();

			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			dsgArray.add(new DataSetGroup(dsgName, getArrayFromArrayList(temp)));
		}
		
		return dsgArray;
	}
	
	private static DataSet[] getArrayFromArrayList(ArrayList<DataSet> al) {
		DataSet[] retValue = new DataSet[al.size()];
		for (int n=0; n<retValue.length; n++) 
			retValue[n]=al.get(n);

		return retValue;
	}
}
