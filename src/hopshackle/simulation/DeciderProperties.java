package hopshackle.simulation;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

public class DeciderProperties implements Serializable {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");

	private static final long serialVersionUID = -5109702412266051768L;
	
	private Properties properties;
	private String name;
	
	public static DeciderProperties factory(String prefix, Properties masterProperties) {
		// first we extract the required properties from the master file
		// this consists of all entries with the correct prefix, plus all entries without a prefix
		Properties general = generalProperties(masterProperties);
		Properties specific = specificProperties(prefix, masterProperties);
		general.putAll(specific);
		return new DeciderProperties(prefix, general);
	}

	public static Properties specificProperties(String prefix, Properties masterProperties) {
		Properties subset = new Properties();
		Enumeration<?> allProperties = masterProperties.propertyNames();
		while (allProperties.hasMoreElements()) {
			String pName = (String) allProperties.nextElement();
			String[] parse = pName.split("\\.");
			if (parse[0].equals(prefix)) {
				String trimmedName = pName.substring(parse[0].length()+1);
				subset.setProperty(trimmedName, masterProperties.getProperty(pName));
			}
		}
		return subset;
	}

	public static Properties generalProperties(Properties masterProperties) {
		// returns all those with no prefix
		Properties subset = new Properties();
		Enumeration<?> allProperties = masterProperties.propertyNames();
		while (allProperties.hasMoreElements()) {
			String pName = (String) allProperties.nextElement();
			String[] parse = pName.split("\\.");
			if (parse.length == 1 && !subset.containsKey(pName)) {
				subset.setProperty(pName, masterProperties.getProperty(pName));
			}
		}
		return subset;
	}
	
	protected DeciderProperties(String name, Properties baseline) {
		this.name = name;
		properties = baseline;
	}

	public void putAll(Properties prop) {
		properties.putAll(prop);
	}

	public DeciderProperties clone() {
		return new DeciderProperties(name, (Properties) properties.clone());
	}

	public void setProperty(String propertyName, String value) {
		properties.setProperty(propertyName, value);
	}

	public String getProperty(String propertyName, String defaultValue) {
		if (!properties.containsKey(propertyName)) {
			logger.warning(propertyName + " not found in Properties " + this.name + ". Using default " + defaultValue +".");
			setProperty(propertyName, defaultValue);
		}
		return properties.getProperty(propertyName, defaultValue);
	}

	public double getPropertyAsDouble(String name, String defaultValue) {
		if (!properties.containsKey(name)) { 
			logger.warning(name + " not found in Properties " + this.name + ". Using default " + defaultValue +".");
			setProperty(name, defaultValue);
		}
		String temp = getProperty(name, defaultValue);
		return Double.valueOf(temp);
	}

	public int getPropertyAsInteger(String name, String defaultValue) {
		if (!properties.containsKey(name)) { 
			logger.warning(name + " not found in Properties " + this.name + ". Using default " + defaultValue +".");
			setProperty(name, defaultValue);
		}
		String temp = getProperty(name, defaultValue);
		return Integer.valueOf(temp);
	}
}
