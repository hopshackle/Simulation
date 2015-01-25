package hopshackle.simulation;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class HopshackleUtilities {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");

	public static <T> List<T> cloneList(Collection<T> listToClone) {
		List<T> retValue = new ArrayList<T>();
		if (listToClone == null) return retValue;
		for (T item : listToClone) {
			retValue.add(item);
		}
		return retValue;
	}

	public static <T, V> Map<T, V> cloneMap(Map<T, V> mapToClone) {
		Map<T, V> retValue = new HashMap<T, V>();
		for (T key : mapToClone.keySet()) {
			retValue.put(key, mapToClone.get(key));
		}
		return retValue;
	}

	public static List<String> createListFromFile(File f) {
		List<String> retList = new ArrayList<String>();
		FileInputStream fis;
		BufferedReader br;
		try {
			fis = new FileInputStream(f);
			InputStreamReader isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);

			String nStr;
			nStr = br.readLine();
			while (nStr!=null) {
				retList.add(nStr);
				nStr = br.readLine();
			}

			br.close();
			fis.close();
		} catch (FileNotFoundException e) {
			logger.severe(e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}

		return retList;
	}

	public static List<Integer> convertToIntegers(List<String> integersAsStrings) {
		List<Integer> retValue = new ArrayList<Integer>();
		for (String s : integersAsStrings) {
			Integer i = Integer.valueOf(s);
			retValue.add(i);
		}
		return retValue;
	}


	@SuppressWarnings("unchecked")
	public static List<Object> loadEnums(List<String> classFullNameList) {
		Class aeClass = null;
		List<Object> retList = new ArrayList<Object>();
		for (String aeString : classFullNameList) {
			try {
				aeClass = Class.forName(aeString);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			for (Object ae : EnumSet.allOf(aeClass))  {
				retList.add(ae);
			}
		}
		return retList;
	}

	public static String getArgument(String[] args, int index, String defaultValue) {
		if (args == null || args.length <= index)
			return defaultValue;

		if (args[index] == null)
			return defaultValue;

		return args[index];
	}

	public static int getArgument(String[] args, int index, int defaultValue) {
		if (args == null || args.length <= index)
			return defaultValue;

		int retValue = defaultValue;
		try {
			retValue = Integer.valueOf(args[index]);
		} catch (NumberFormatException e) {
			retValue = defaultValue;
		}

		return retValue;
	}
}
