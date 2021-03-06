package hopshackle.simulation;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class HopshackleUtilities {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");

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
	
	public static <T> List<T> listFromInstance(T instance) {
		List<T> retList = new ArrayList<T>();
		if (instance != null) retList.add(instance);
		return retList;
	}

	@SafeVarargs
	public static <T> List<T> listFromInstances(T... instances) {
		List<T> retList = new ArrayList<T>();
		for (T i : instances) {
			if (i != null && !retList.contains(i)) retList.add(i);
		}
		return retList;
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
			if (s.equals("")) continue;
			Integer i = Integer.valueOf(s);
			retValue.add(i);
		}
		return retValue;
	}

	public static <T> List<T> convertArrayToList(T[] array) {
		List<T> retValue = new ArrayList<>();
		for (T item : array) {
			retValue.add(item);
		}
		return retValue;
	}

	public static <T> List<T> convertSetToList(Set<T> set) {
		List<T> retValue = new ArrayList<>();
		for (T item : set) {
			retValue.add(item);
		}
		return retValue;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> loadEnums(List<String> classFullNameList) {
		@SuppressWarnings("rawtypes")
		Class aeClass = null;
		List<Object> retList = new ArrayList<Object>();
		for (String aeString : classFullNameList) {
			try {
				aeClass = Class.forName(aeString);
			} catch (ClassNotFoundException e) {
				logger.severe(e.toString() + " in HopshackleUtilities.loadEnums");
			}
			if (aeClass != null)
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

	public static String formatArray(double[] array, String delimiter, String format) {
		StringBuilder retValue = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			if (i > 0) retValue.append(delimiter);
			retValue.append(String.format(format, array[i]));
		}
		return retValue.toString();
	}

	public interface Formatter {
		public String format(Object o);
	}

	public static String formatList(List<?> inputList, String delimiter, Formatter prettifier) {
		StringBuilder retValue = new StringBuilder();
		if (inputList != null) {
			boolean firstItemProcessed = false;
			for (Object o : inputList) {
				if (firstItemProcessed) {
					retValue.append(delimiter);
				} else {
					firstItemProcessed = true;
				}
				if (prettifier == null) {
					retValue.append(o.toString());
				} else {
					retValue.append(prettifier.format(o));
				}
			}
		}
		return retValue.toString();
	}

	@SuppressWarnings("unchecked")
	public static <A, B> List<B> convertList(List<A> input) {
		List<B> retValue = new ArrayList<B>();
		if (input == null) return retValue;
		for (A item : input) {
			retValue.add((B) item);
		}
		return retValue;
	}
}
