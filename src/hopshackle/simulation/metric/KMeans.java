package hopshackle.simulation.metric;

import java.io.File;

import hopshackle.simulation.*;

public class KMeans {
	
	private File inputFile;
	
	public static void main(String[] args) {
		String nameOfFile = HopshackleUtilities.getArgument(args, 0, "RawData.txt");
		File input = new File(nameOfFile);
		int clusters = Integer.valueOf(HopshackleUtilities.getArgument(args, 0, "3"));
		
		new KMeans(input, clusters);
	}
	
	public KMeans(File inputData, int maxClusters) {
		inputFile = inputData;

	}

}
