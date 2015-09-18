package hopshackle.simulation.metric;

import java.io.*;
import java.util.ArrayList;

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

	public static ArrayList<DataSetGroup> getDataSets(File dir) {
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
						temp.add(new MySQLDataSet("NSP", dsName, "root", "Metternich", ""));
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
