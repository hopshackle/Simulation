package hopshackle.simulation;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

public class SimProperties {

	private static Properties geneticProperties;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private static boolean initialised = false;
	private static String fileLocation = "C:\\Users\\James\\Google Drive\\Simulations\\Genomes\\GeneticProperties.txt";

	public static void clear() {
		initialised = false;
		initialiseProperties();
	}
	
	public static void setFileLocation(String newLocation) {
		fileLocation = newLocation;
	}
	
	private static void initialiseProperties() {
		if (initialised == false) {
			geneticProperties = new Properties();
			File f = new File(fileLocation);
			try {
				FileInputStream fis = new FileInputStream(f);
				geneticProperties.load(fis);
				fis.close();
				initialised = true;
			} catch (IOException e) {
				logger.warning("Error loading genetic properties: " + e.toString());
				// so we use some default values
				setProperty("MaxModifierChange", "0.05");
				setProperty("MaxPriorityChange", "0.05");
				setProperty("NewCodon", "0.005");
				setProperty("RemoveCodon", "0.0025");
				setProperty("SwapCodon", "0.01");
				setProperty("DuplicateCodon", "0.00125");
				setProperty("NewGeneticTerm", "0.005");
				setProperty("RemoveGeneticTerm", "0.00125");
				setProperty("ModfierShift", "0.003");
				setProperty("CodonRemovalProportionalToNumber", "false");

				// and write to File

				try {
					FileOutputStream fos = new FileOutputStream(f, false);
					geneticProperties.store(fos, "Genetic Properties for hopshackle.simulation");
					fos.close();
				} catch (IOException e2) {
					logger.warning("Error storing genetic properties: " + e2.toString());
				}			
			}		
		}
	}

	public static void setProperty(String propertyName, String value) {
		if (!initialised) initialiseProperties();
		geneticProperties.setProperty(propertyName, value);
	}

	public static String getProperty(String propertyName, String defaultValue) {
		if (!initialised) initialiseProperties();
		if (!geneticProperties.containsKey(propertyName)) { 
			logger.warning(propertyName + " not found in Properties. Using default.");
			setProperty(propertyName, defaultValue);
		}
		return geneticProperties.getProperty(propertyName, defaultValue);
	}

	public static double getPropertyAsDouble(String name, String defaultValue) {
		if (!initialised) initialiseProperties();
		if (!geneticProperties.containsKey(name)) { 
			logger.warning(name + " not found in Properties. Using default.");
			setProperty(name, defaultValue);
		}
		String temp = getProperty(name, defaultValue);
		double retValue = Double.valueOf(temp);
		return retValue;
	}
}
