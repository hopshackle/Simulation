package hopshackle.simulation;

import java.util.*;

public class ArgParser {

	private Map<String, String> argMap = new HashMap<String, String>();

	public ArgParser(String[] argArray) {
		for (int i = 0; i < argArray.length; i += 2) {
			argMap.put(argArray[i], argArray[i + 1]);
		}
	}

	public boolean getBoolean(String arg, boolean def) {
		if (argMap.containsKey(arg))
			return argMap.get(arg).toLowerCase() == "true";
		return def;
	}

	public String getString(String arg, String def) {
		if (argMap.containsKey(arg))
			return argMap.get(arg);
		return def;
	}
	
	public int getInt(String arg, int def) {
		if (argMap.containsKey(arg))
			return Integer.valueOf(argMap.get(arg));
		return def;
	}
}
