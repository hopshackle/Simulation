package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class LoadBrains {
	
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	
	public static ArrayList<NeuralDecider> loadNN(File directory, FilenameFilter filter, World world) {
		
		ArrayList<NeuralDecider> retValue = new ArrayList<NeuralDecider>();
		if (directory == null || !directory.isDirectory()) {
			logger.severe("Error in loading brains, specified Directory isn't: " + directory);
			return new ArrayList<NeuralDecider>();
		}
			
		File files[] = directory.listFiles(filter);
		
		for (File f : files) {
			NeuralDecider brain = NeuralDecider.createNeuralDecider(f);
			if (brain != null) {
				retValue.add(brain);
			} else
				logger.severe("Error reading Neural Decider for file:" + f.toString());
		}
	
		return retValue;
	}
	
	public static FilenameFilter createBrainFilter(String searchText) {
		return new HopshackleFilter(searchText, "brain");
	}

}

