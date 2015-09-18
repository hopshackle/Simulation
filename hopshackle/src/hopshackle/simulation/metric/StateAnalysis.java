package hopshackle.simulation.metric;

import hopshackle.simulation.*;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class StateAnalysis {

	private int MIN_VISITED = 800;
	private String[] CLASS = new String[]{"EXP", "FTR", "CLR", "FTRCBT", "CLRCBT"};
	private int[][][] oneStepDifferences;
	private HashMap[] valueCounts;
	private ArrayList<Integer> excludedList;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	public static void main(String[] args) {
		new StateAnalysis(args);
	}

	public StateAnalysis(String[] args) {
		excludedList = new ArrayList<Integer>();
		ArrayList<DataSetGroup> dsgArray = DataSetGroup.getDataSets(new File(baseDir + "\\Metrics"));

		if (args.length > 0 && args[0] != null) {
			CLASS = new String[1];
			CLASS[0] = args[0];
			if (args.length > 1 && args[1] != null) 
				MIN_VISITED = Integer.valueOf(args[1]);
			if (args.length > 2) 
				for (int loop = 2; loop < args.length; loop++) 
					excludedList.add(Integer.valueOf(args[loop]));
		} else {
			System.out.println("No class and exclusion list specified for analysis");
		}

		String sqlQuery = null;
		Connection con = ConnectionFactory.getConnection("SimAnalysis", "root", "Metternich", "", false);
		try {
			sqlQuery = "CREATE TABLE IF NOT EXISTS Stateanalysis " +
			" (	datasetgroup	VARCHAR(50)	NOT NULL,"	+
			" 	dataset			VARCHAR(50)	NOT NULL,"	+
			"	class			VARCHAR(10)  NOT NULL,"	+
			"	boundary		INT			NOT NULL,"	+
			"	records			INT			NOT NULL,"	+
			"	attribute		INT			NOT NULL,"	+
			"	key_percentage	INT			NOT NULL,"	+
			"	entropy			DOUBLE		NOT NULL,"	+
			"	b1				INT			NOT NULL,"	+
			"	b2				INT			NOT NULL,"	+
			"	b3				INT			NOT NULL,"	+
			"	b4				INT			NOT NULL,"	+
			"	b5				INT			NOT NULL,"	+
			"	b6				INT			NOT NULL,"	+
			"	b7				INT			NOT NULL,"	+
			"	b8				INT			NOT NULL,"	+
			"	b9				INT			NOT NULL,"	+
			"	b10				INT			NOT NULL,"	+
			"	b11				INT			NOT NULL,"	+
			"	b12				INT			NOT NULL"	+
			");";

			Statement st = con.createStatement();
			st.executeUpdate(sqlQuery);
		} catch (SQLException e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}

		for (DataSetGroup dsg : dsgArray) {
			for (DataSet ds : dsg.getArrayList()) {
				String tableName = "states_" + ds.toString();
				MySQLDataSet dataset = new MySQLDataSet("NSP", tableName, "root", "Metternich", "");
				ResultSet rs = dataset.getResultSet("SELECT * FROM &Table where visited > " + MIN_VISITED + ";");

				for (String stateText : CLASS) {
					try {
						Statement st = con.createStatement();
						sqlQuery = String.format("DELETE FROM Stateanalysis WHERE datasetgroup = '%s' AND class = '%s';", dsg.toString(), stateText);
						st.executeUpdate(sqlQuery);
					} catch (SQLException e1) {
						logger.severe(e1.toString());
						e1.printStackTrace();
					}
					
					ArrayList<StateRow> stateArray = new ArrayList<StateRow>();
					try {
						for (rs.first(); !rs.isAfterLast(); rs.next()) {
							String text = rs.getString(1);
							int value = rs.getInt(2);
							if (text.startsWith(stateText+":")) 
								stateArray.add(new StateRow(text, value));
						}
					} catch (SQLException e) {
						System.out.println("Error : " + e.toString());
						e.printStackTrace();
					}
					
					if (stateArray.isEmpty()) {
						continue;
					}
						
					// Now we have stateArray set up (albeit not sorted)
					int totalAttributes = stateArray.get(0).stateAttributes.length;
					int[] score = new int[totalAttributes];
					int orphans = 0;

					compileOneStepDifferences(stateArray);

					for (StateRow state: stateArray) {
						int[] highestValue = new int[totalAttributes];
						int[] lowestValue = new int[totalAttributes];
						for (int loop=0; loop<totalAttributes; loop++) {
							highestValue[loop] = Integer.MIN_VALUE;
							lowestValue[loop] = Integer.MAX_VALUE;
						}

						for (StateRow comparison : stateArray) {
							// to be valid for comparison, we need all values to be the same, except one
							int key = comparison.oneStepDifferent(state);
							if (key > -1) {
								if (comparison.stateValue > highestValue[key])
									highestValue[key] = comparison.stateValue;
								if (comparison.stateValue < lowestValue[key])
									lowestValue[key] = comparison.stateValue;
							}
						}

						// we now have the highest and lowest values of the states that are one step different
						// in each of the attributes

						int hiValue = Integer.MIN_VALUE;
						int loValue = Integer.MAX_VALUE;
						int hiAtt = -1, loAtt = -1;
						for (int loop=0; loop<totalAttributes; loop++) {
							if (highestValue[loop] > hiValue) {
								hiValue = highestValue[loop];
								hiAtt = loop;
							}
							if (lowestValue[loop] < loValue) {
								loValue = lowestValue[loop];
								loAtt = loop;
							}
						}

						// and finally record the result in the running score
						if (hiAtt == -1) 
							orphans++;
						if (hiAtt > -1)
							score[hiAtt]++;
						if (loAtt > -1)
							score[loAtt]++;
					}

					double total = 0;
					for (int loop=0; loop<totalAttributes; loop++) total=total+score[loop];

					System.out.println(String.format("%s records: %5d, Orphans: %4d, Class: %s, Boundary: %5d", 
							tableName, stateArray.size(), orphans, stateText, MIN_VISITED));
					for (int loop=0; loop<totalAttributes-1; loop++) {
						String output = String.format("Attribute %2d: %4d %4d%%     :     S=%5.2f  [%3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d]", 
								loop+1, score[loop], (int)(score[loop] * 100 / total), entropy(valueCounts[loop]),
								getPercent((Integer)valueCounts[loop].get(1), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(2), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(3), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(4), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(5), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(6), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(7), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(8), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(9), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(10), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(11), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(12), stateArray.size())
						);
						System.out.println(output);

						sqlQuery = String.format("INSERT INTO StateAnalysis (datasetgroup, dataset, class, boundary, records, " +
								"attribute, key_percentage, entropy, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12) " +
								"VALUES ('%s', '%s', '%s', %d, %d, %d, %d, %.2f, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d)", 
								dsg.toString(), ds.toString(), stateText, MIN_VISITED, stateArray.size(), loop+1, (int)(score[loop] * 100 / total), entropy(valueCounts[loop]),
								getPercent((Integer)valueCounts[loop].get(1), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(2), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(3), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(4), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(5), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(6), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(7), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(8), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(9), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(10), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(11), stateArray.size()),
								getPercent((Integer)valueCounts[loop].get(12), stateArray.size()));

						try {
							Statement st = con.createStatement();
							st.executeUpdate(sqlQuery);
						} catch (SQLException e) {
							logger.severe(e.toString());
							e.printStackTrace();
						}

					}
					System.out.println();
				}

			} // dataset loop
		} // dataset group loop
	}

	private int getPercent(Integer integer, double total) {
		if (integer == null) return 0;
		return (int)Math.round((double)integer*100.0 / total);
	}

	@SuppressWarnings("unchecked")
	private double entropy(HashMap map) {
		Set<Integer> keys = map.keySet();
		double total = 0;

		for (Integer i : keys)
			total += (Integer) map.get(i);

		double entropy = 0;
		for (Integer i : keys) {
			double percentage = (double)((Integer)map.get(i)) / total;
			entropy += percentage * Math.log(percentage);
		}

		return -entropy;
	}

	private void compileOneStepDifferences(ArrayList<StateRow> stateArray) {

		int totalAttributes = stateArray.get(0).stateAttributes.length;
		oneStepDifferences = new int[totalAttributes][99][2];
		valueCounts = new HashMap[totalAttributes];
		for (int loop1 = 0; loop1 < totalAttributes; loop1++)
			for (int loop2 = 0; loop2 < 99; loop2++) {
				oneStepDifferences[loop1][loop2][0] = -1;
				oneStepDifferences[loop1][loop2][1] = -1;
			}
		// first dimension is attribute
		// second dimension is value
		// third dimension is lower or higher value

		for (int loop =0; loop < totalAttributes; loop++) {
			TreeSet<Integer> allValues = new TreeSet<Integer>();
			HashMap<Integer, Integer> valueCount = new HashMap<Integer, Integer>();
			for (StateRow state : stateArray) {
				if (valueCount.containsKey(state.stateAttributes[loop])) {
					Integer currentCount = valueCount.get(state.stateAttributes[loop]);
					currentCount++;
					valueCount.put(state.stateAttributes[loop], currentCount);
				} else {
					allValues.add(state.stateAttributes[loop]);
					valueCount.put(state.stateAttributes[loop], 1);
				}
			}

			valueCounts[loop] = valueCount;
			int previousValue = -1;
			Iterator<Integer> iter = allValues.iterator();
			while(iter.hasNext()) {
				int newValue = iter.next();
				oneStepDifferences[loop][newValue][0] = previousValue;
				if (previousValue > -1)
					oneStepDifferences[loop][previousValue][1] = newValue;
				previousValue = newValue;
			}
		}
	}

	class StateRow {

		String fullText;
		int[] stateAttributes;
		int stateValue;

		StateRow(String text, int stateValue) {
			fullText = text;
			this.stateValue = stateValue;
			String[] temp = fullText.split(":");
			int loop = 0;
			stateAttributes = new int[temp.length];
			for (String s : temp) {
				try {
					stateAttributes[loop] = Integer.valueOf(s);
					loop++;
				} catch (NumberFormatException e) {
					// invalid number
				}
			}

		}

		public int oneStepDifferent(StateRow state) {
			// if not one step apart, then we return -1
			// else we return the index of the attribute that is one step different

			// excluded attributes indicate such a difference in States that they are not comparable
			for (Integer excluded : excludedList) {
				if (this.stateAttributes[excluded-1] != state.stateAttributes[excluded-1])
					return -1;
			}

			int retValue = -1;
			boolean possiblyEqual = true;
			for (int loop = 0; loop < state.stateAttributes.length; loop++) {
				if (this.stateAttributes[loop] == state.stateAttributes[loop]) {
					// fine - continue straight on
				} else {
					// if we already have one difference, then not eligible, so exit
					if (!possiblyEqual) return -1;
					possiblyEqual = false;

					// if otherwise equal, and the values are one step apart, then store this index.
					// if more than two apart, then exit
					if ((oneStepDifferences[loop][this.stateAttributes[loop]][0] == state.stateAttributes[loop]) ||
							(oneStepDifferences[loop][this.stateAttributes[loop]][1] == state.stateAttributes[loop])) {
						retValue = loop;
					} else {
						return -1;
					}

				}
			}
			// if we reach here, then the two state are one step apart
			return retValue;
		}
	}
}
