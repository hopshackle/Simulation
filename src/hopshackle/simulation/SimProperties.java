package hopshackle.simulation;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class SimProperties {

	private static Properties geneticProperties;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private static boolean initialised = false;
	private static String fileLocation = "C:\\Users\\James\\Google Drive\\Simulations\\Genomes\\GeneticProperties.txt";
	private static Map<String, DeciderProperties> deciderPropertiesMap = new HashMap<String, DeciderProperties>();
	
	public static void clear() {
		initialised = false;
		initialiseProperties();
	}
	
	public static void setFileLocation(String newLocation) {
		fileLocation = newLocation;
	}
	
	public static void initialiseProperties(Properties baseline) {
		geneticProperties = baseline;
		initialised = true;
		deciderPropertiesMap = new HashMap<String, DeciderProperties>();
		initialiseProperties();
	}
	
	private static void initialiseProperties() {
		if (!initialised) {
			geneticProperties = new Properties();
			File f = new File(fileLocation);
			try {
				FileInputStream fis = new FileInputStream(f);
				geneticProperties.load(fis);
				fis.close();
				initialised = true;
			} catch (IOException e) {
				logger.warning("Error loading genetic properties: " + e.toString());
			}		
		}
		Set<String> deciderNames = extractDeciderNames(geneticProperties);
		for (String decider : deciderNames) {
			DeciderProperties dp = DeciderProperties.factory(decider, geneticProperties);
			deciderPropertiesMap.put(decider, dp);
		}
		deciderPropertiesMap.put("GLOBAL", new DeciderProperties("GLOBAL", geneticProperties));
	}
	
	public static Set<String> extractDeciderNames(Properties prop) {
		Set<String> retValue = new HashSet<String>();
		Enumeration<?> allProperties = prop.propertyNames();
		while (allProperties.hasMoreElements()) {
			String pName = (String) allProperties.nextElement();
			String[] parse = pName.split("\\.");
			if (parse.length > 1) {
				retValue.add(parse[0]);
			}
		}
		return retValue;
	}

	public static Set<String> allDeciderNames() {
		if (!initialised) initialiseProperties();
		return deciderPropertiesMap.keySet();
	}
	
	public static DeciderProperties getDeciderProperties(String type) {
		if (!initialised) initialiseProperties();
		return deciderPropertiesMap.get(type);
	}

	public static void setProperty(String propertyName, String value) {
		if (!initialised) initialiseProperties();
		geneticProperties.setProperty(propertyName, value);
	}

	public static String getProperty(String propertyName, String defaultValue) {
		if (!initialised) initialiseProperties();
		if (!geneticProperties.containsKey(propertyName)) {
			logger.warning(propertyName + " not found in Properties. Using default " + defaultValue +".");
			setProperty(propertyName, defaultValue);
		}
		return geneticProperties.getProperty(propertyName, defaultValue);
	}

	public static double getPropertyAsDouble(String name, String defaultValue) {
		if (!initialised) initialiseProperties();
		if (!geneticProperties.containsKey(name)) { 
			logger.warning(name + " not found in Properties. Using default " + defaultValue +".");
			setProperty(name, defaultValue);
		}
		String temp = getProperty(name, defaultValue);
		return Double.valueOf(temp);
	}

	public static int getPropertyAsInteger(String name, String defaultValue) {
		if (!initialised) initialiseProperties();
		if (!geneticProperties.containsKey(name)) { 
			logger.warning(name + " not found in Properties. Using default " + defaultValue +".");
			setProperty(name, defaultValue);
		}
		String temp = getProperty(name, defaultValue);
		return Integer.valueOf(temp);
	}
}
