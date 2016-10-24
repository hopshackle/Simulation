package hopshackle.simulation;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class HopshackleState {

	/*  A State is responsible for keeping track of :
	 * 		- the Actions taken
	 * 		- the States they led to
	 * 		- the Rewards received for those Actions
	 * 
	 * 		- plus the current Value of this State
	 */

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	private static Hashtable<String, HopshackleState> allStates;
	private static Hashtable<String, ActionEnum> allActions;
	private static double gamma;
	private static double alpha;
	private static int memoryLimit;
	private static int stateBaseValue;
	private boolean debug = false;
	private static double debugProportion = 0.000;
	private File logFile;
	private FileWriter logWriter;
	public static String newline = System.getProperty("line.separator");
	private String representation;
	private Hashtable<ActionEnum, ActionStatistics> actionConsequences;
	private ArrayList<ActionEnum> actions;
	private double value;
	private int visited;

	static {
		allActions = new Hashtable<String, ActionEnum>();
		allStates = new Hashtable<String, HopshackleState>();
		
		String fileName = SimProperties.getProperty("ActionEnumClassFile", "");
		List<String> actionEnumClasses = HopshackleUtilities.createListFromFile(new File(fileName));
		List<Object> actionEnumList = HopshackleUtilities.loadEnums(actionEnumClasses);
		for (Object ae : actionEnumList)
			addActionEnum((ActionEnum) ae);
		clear();
	}

	private HopshackleState(String rep) {
		representation = rep;
		value = stateBaseValue;
		visited = 0;
		actionConsequences = new Hashtable<ActionEnum, ActionStatistics>();
		actions = new ArrayList<ActionEnum>();

		allStates.put(rep, this);

		if (Math.random() < debugProportion) {
			debug = true;
			initialiseLogFile();
		}
	}
	private void initialiseLogFile() {
		try {
			String logFileName = this.toString().replace(':', '_');
			logFileName = baseDir + "//logs//State_" + logFileName + ".log";
			logFile = new File(logFileName);
			logWriter = new FileWriter(logFile, true);
		} catch (IOException e) {
			e.printStackTrace();
			logger.severe(e.toString());
		}
	}
	public void addActionToState(ActionEnum ae) {
		if (ae == null) return;
		actions.add(ae);
		addActionEnum(ae);
	}

	public static void addActionEnum(ActionEnum ae) {
		if (!allActions.containsKey(ae.toString())) 
			allActions.put(ae.toString(), ae);
	}
	public static ActionEnum getActionEnum(String actionTitle) {
		if (allActions.containsKey(actionTitle))
			return allActions.get(actionTitle);

		logger.warning("Unknown Action: " + actionTitle);
		return null;
	}
	public static void clear() {
		alpha = SimProperties.getPropertyAsDouble("Alpha", "0.05");
		gamma = SimProperties.getPropertyAsDouble("Gamma", "0.95");
		memoryLimit = (int) SimProperties.getPropertyAsDouble("VisitMemoryLimit", "1000");
		stateBaseValue = (int) SimProperties.getPropertyAsDouble("StateBaseValue", "2000");
		allStates.clear();
	}

	public static HopshackleState getState(String rep) {
		if (allStates.containsKey(rep))  {
			HopshackleState retValue = allStates.get(rep);
			return retValue;
		}
		else
			return new HopshackleState(rep);
	}

	public double valueOption(ActionEnum option, double noiseLevel) {
		return getValueOfOption(option, noiseLevel * 2.0);
	}

	public double valueOptionWithoutExploration(ActionEnum option) {
		return getValueOfOption(option, 0.0);
	}
	private synchronized double getValueOfOption(ActionEnum option, double explorationPreference) {
		double newScore;
		double error;
		ActionStatistics statistics = getStatistics(option);
		newScore = statistics.getValueOfNextState();
		newScore = newScore + explorationPreference * statistics.getErrorOnValueOfNextState();
		// so that if something has not been sampled very often, we are optimistic

		error = statistics.getErrorOnReward();
		newScore = newScore + statistics.getReward() + explorationPreference * error;

		return newScore;
	}

	private ActionStatistics getStatistics(ActionEnum option) {
		ActionStatistics statistics = actionConsequences.get(option);
		if (statistics == null) {
			statistics = new ActionStatistics(this, option);
			actionConsequences.put(option, statistics);
			log("Adding ActionStatistics for " + option.toString());
		}
		return statistics;
	}
	public synchronized void addExperience(ActionEnum ae, HopshackleState newState, double reward) {
		if (ae == null) {
			logger.severe("Null ActionEnum in addExperience. " + newState);
			return;
		}
		ActionStatistics statistics = getStatistics(ae);
		statistics.addExperience(newState, reward);
		visited++;

		log("Has been visited " + visited + " times");
		if (visited > memoryLimit * 1.10) {
			pruneActionsToSaveMemory();
		}
	}

	public synchronized void updateStateValue(ActionEnum ae, HopshackleState newState, double reward) {
		double newStateValue = newState.getValue();
		value = value + alpha * (reward + gamma * newStateValue - value);
		if (debug) {
			String logMessage = String.format("Reward of %.0f, next state %s with value of %.0f. New value is %.2f", reward, newState.toString(), newStateValue, value);
			log(logMessage);
		}
	}

	private void pruneActionsToSaveMemory() {
		/*
		 *  Prune:
		 *  	iterate over each action
		 *  		iterate over each nextState
		 *  			reduce population by 10%
		 *  			remove 1 based on modulo of above
		 *  			reduce rewardSum and SquareSum in line with reduction
		 *  	update visited to be consistent	
		 */

		log("Now pruning memory");
		ActionStatistics asToPrune = null;
		int totalRemoved = 0;
		for (ActionEnum aeToPrune : actionConsequences.keySet()) {
			asToPrune = actionConsequences.get(aeToPrune);
			if (asToPrune == null) continue;
			int newTotalCount = 0;
			ArrayList<HopshackleState> statesToRemove = new ArrayList<HopshackleState>();
			for (HopshackleState ns : asToPrune.nextStates.keySet()) {
				int oldCount = asToPrune.nextStates.get(ns);
				double toRemove = ((double)oldCount) * 0.10;
				int removed = (int) toRemove;
				if (Math.random() < toRemove - removed) 
					removed++;
				totalRemoved += removed;
				asToPrune.nextStates.put(ns, oldCount - removed);
				newTotalCount += (oldCount - removed);
				if (oldCount - removed == 0) {
					statesToRemove.add(ns);
				}
			}
			for (HopshackleState s : statesToRemove)
				asToPrune.nextStates.remove(s);

			double reductionFactor = ((double)(newTotalCount+2))/(double)asToPrune.getCount();
			asToPrune.rewardSum = asToPrune.rewardSum * reductionFactor;
			asToPrune.rewardSquareSum = asToPrune.rewardSquareSum * reductionFactor;
			asToPrune.count = newTotalCount + 2;
		}
		visited = visited - totalRemoved;
	}

	public int getCountOfNextState(ActionEnum action, HopshackleState nextState) {
		if (actionConsequences.containsKey(action)) {
			ActionStatistics consequences = actionConsequences.get(action);
			return consequences.getCountOfNextState(nextState);
		}
		return 0;
	}

	public double getValue() {return value;}
	protected void setValue(double v) {value = v;}
	public String toString() {return representation;}
	public int getVisited() {return visited;}
	public List<ActionEnum> getActions() {
		List<ActionEnum> retValue = new ArrayList<ActionEnum>();
		for (ActionEnum ae : actions) {
			retValue.add(ae);
		}
		return retValue;
	}
	public void log(String logMessage) {
		if(!debug) return;
		try {
			logWriter.write(logMessage+newline);
		} catch (IOException e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}
	}
	private void closeLogFile() {
		if (!debug) return;
		try {
			logWriter.close();
			debug = false;
		} catch (IOException e) {
			logger.severe(e.toString());
			e.printStackTrace();
		}
	}

	public static void recordStates(String suffix) {
		closeAllLogFiles();

		System.out.println("Starting state recording");
		String stateTable, actionTable, transitionTable;
		DatabaseAccessUtility dbu = new DatabaseAccessUtility();
		Thread t = new Thread(dbu);
		t.start();

		stateTable = "states_" + suffix;
		actionTable = "state_actions_" + suffix;
		transitionTable = "transitions_" + suffix;

		createEmptyTable(stateTable, stateTableCreationSQL(stateTable), dbu);
		createEmptyTable(actionTable, actionTableCreationSQL(actionTable), dbu);
		createEmptyTable(transitionTable, transitionTableCreationSQL(transitionTable), dbu);

		String stateTableFieldInsert = "INSERT INTO " + stateTable + " (state, value, visited, Dimension01, Dimension02, Dimension03, " +
		"Dimension04, Dimension05, Dimension06, Dimension07, Dimension08, Dimension09, Dimension10) VALUES ";
		List<String> SQLFragments = new ArrayList<String>();
		for (HopshackleState s : allStates.values()) {
			SQLFragments.add(String.format(" ('%s', %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d) ",
					s.toString(), (int)s.getValue(), s.visited, s.getDimensionValue(1), s.getDimensionValue(2), s.getDimensionValue(3),
					s.getDimensionValue(4), s.getDimensionValue(5), s.getDimensionValue(6), s.getDimensionValue(7),
					s.getDimensionValue(8), s.getDimensionValue(9), s.getDimensionValue(10)));
		}
		insertDataIntoTable(stateTableFieldInsert, SQLFragments, dbu);

		System.out.println("Starting actions recording");
		String actionTableFieldInsert = "INSERT INTO " + actionTable + " (state, action, value, error, reward, visited) VALUES ";
		SQLFragments = new ArrayList<String>();
		for (HopshackleState s : allStates.values()) {
			for (ActionEnum ae : s.getActions()) {
				ActionStatistics as = s.getStatistics(ae);
				int value = (int) as.getValueOfNextState();
				int error = (int) as.getErrorOnValueOfNextState();
				int visited = as.getCount();
				int reward = (int) as.getReward();
				SQLFragments.add(String.format(" ('%s', '%s', %d, %d, %d, %d) ", s.toString(), ae.toString(), value, error, reward, visited));
			}
		}
		insertDataIntoTable(actionTableFieldInsert, SQLFragments, dbu);

		System.out.println("Starting transitions recording");
		String transitionTableFieldInsert = "INSERT INTO " + transitionTable + " (oldstate, newstate, action, count, reward, stddev) VALUES ";
		SQLFragments = new ArrayList<String>();
		for (HopshackleState s : allStates.values()) {
			for (ActionStatistics as : s.actionConsequences.values()) {
				for (HopshackleState ns : as.nextStates.keySet()) {
					SQLFragments.add(String.format(" ('%s', '%s', '%s', %d, %d, %.2f) ",
							s.toString(), ns.toString(), as.action.toString(),
							as.nextStates.get(ns), (int)as.getReward(), as.getDeviation()));
				}
			}
		}
		insertDataIntoTable(transitionTableFieldInsert, SQLFragments, dbu);
		System.out.println("Ending transitions recording");
		dbu.addUpdate("EXIT");
	}

	private static void createEmptyTable(String tableName, String tableCreationSQL, DatabaseAccessUtility dbu) {
		String sqlQuery = "DROP TABLE IF EXISTS " + tableName;
		dbu.addUpdate(sqlQuery);
		dbu.addUpdate(tableCreationSQL);
	}

	private static String stateTableCreationSQL(String tableName) {
		return "CREATE TABLE " + tableName +
		" ( state	VARCHAR(100)			PRIMARY KEY,"		+
		" value		INT			NOT NULL,"		+
		" visited	INT			NOT NULL," 		+
		" Dimension01 	INT 	NOT NULL, " +
		" Dimension02 	INT 	NOT NULL, " +
		" Dimension03 	INT 	NOT NULL, " +
		" Dimension04 	INT 	NOT NULL, " +
		" Dimension05 	INT 	NOT NULL, " +
		" Dimension06 	INT 	NOT NULL, " +
		" Dimension07 	INT 	NOT NULL, " +
		" Dimension08 	INT 	NOT NULL, " +
		" Dimension09 	INT 	NOT NULL, " +
		" Dimension10 	INT 	NOT NULL " +
		");";
	}
	private static String actionTableCreationSQL(String tableName) {
		return "CREATE TABLE " + tableName +
		" ( state	VARCHAR(100)	NOT NULL,"		+
		" action		VARCHAR(25)		NOT NULL,"		+
		" value				INT			NOT NULL," 		+
		" error				INT			NOT NULL," 		+
		" reward				INT			NOT NULL," 		+
		" visited				INT			NOT NULL" 		+
		");";
	}
	private static String transitionTableCreationSQL(String tableName) {
		return "CREATE TABLE " + tableName +
		" ( oldstate	VARCHAR(100)	NOT NULL,"		+
		" newstate		VARCHAR(100)	NOT NULL,"		+
		" action		VARCHAR(25)		NOT NULL,"		+
		" count				INT			NOT NULL," 		+
		" reward			INT			NOT NULL,"		+
		" stddev		FLOAT			NOT NULL"		+
		");";
	}
	
	private static void closeAllLogFiles() {
		for(HopshackleState s : allStates.values()) {
			s.closeLogFile();
		}
	}
	
	private static void insertDataIntoTable(String tableFieldInsert, List<String> SQLFragments, DatabaseAccessUtility dbu) {

		StringBuffer sqlBuilder = new StringBuffer(tableFieldInsert);
		int itemsInUpdate = 0;
		for (String s : SQLFragments) {
			if (itemsInUpdate > 0) sqlBuilder.append(", ");
			sqlBuilder.append(s);
			itemsInUpdate++;

			if (itemsInUpdate >= 500) {
				sqlBuilder.append(";");
				itemsInUpdate = 0;
				dbu.addUpdate(sqlBuilder.toString());
				sqlBuilder = new StringBuffer(tableFieldInsert);
			}
		}
		if (itemsInUpdate > 0) {
			sqlBuilder.append(";");
			dbu.addUpdate(sqlBuilder.toString());
		}
	}
	
	private int getDimensionValue(int i) {
		String[] dimensionArray = representation.split(":");
		if (i >= dimensionArray.length)
			return 0;
		try {
			return Integer.valueOf(dimensionArray[i]);
		} catch (NumberFormatException e) {
			//
		}
		return 0;
	}

	public static void loadStates(String suffix) {

		String stateTable, transitionTable;
		clear();
		logger.info("Loading StateSpace for " + suffix);

		Connection con = ConnectionFactory.getConnection();
		try {

			// State table
			stateTable = "states_" + suffix;
			transitionTable = "transitions_" + suffix;
			Statement st = con.createStatement();
			Statement stTransitions = con.createStatement();

			ResultSet rs;
			rs = st.executeQuery("SELECT * FROM " + stateTable);
			HopshackleState s;
			for (rs.first(); !rs.isAfterLast(); rs.next()) {
				/*		1 = state
				 * 		2 = value
				 * 		3 = visited
				 */

				s = HopshackleState.getState(rs.getString(1));
				s.setValue(rs.getDouble(2));
				s.visited = rs.getInt(3);
			}
			// Now we get the transitions for the states. 
			ResultSet rsTransitions = stTransitions.executeQuery("SELECT * FROM " + transitionTable);
			ActionStatistics as;
			for (rsTransitions.first(); !rsTransitions.isAfterLast(); rsTransitions.next()) {
				/*	1 = oldstate
				 *  2 = newstate
				 *  3 = action
				 *  4 = count
				 *  5 = reward
				 *  6 = std dev
				 */ 

				String actionTitle = rsTransitions.getString(3);
				ActionEnum ae = getActionEnum(actionTitle);

				s = HopshackleState.getState(rsTransitions.getString(1));
				as = s.actionConsequences.get(ae);
				if (as == null) {
					as = s.new ActionStatistics(s, ae);
					s.actionConsequences.put(ae, as);
				}

				as.addData(rsTransitions.getInt(4), getState(rsTransitions.getString(2)), 
						rsTransitions.getInt(5), rsTransitions.getDouble(6));
			}

			rsTransitions.close();
			stTransitions.close();
			st.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.severe(e.toString());
		}
	}

	class ActionStatistics {

		/* ActionStatistics contains the detail
		 *  of what happens if a given Action is taken from this State
		 *  
		 *  it holds the average reward, the number of times the action has been taken
		 *  and the states that came next after the action
		 */

		private ActionEnum action;
		private HopshackleState parentState;
		private double rewardSum, valueSum;
		private double rewardSquareSum, valueSquareSum;
		private int count;
		private Hashtable<HopshackleState, Integer> nextStates;

		ActionStatistics(HopshackleState parent, ActionEnum ae) {
			action = ae;
			parentState = parent;
			rewardSum = 0.0;
			rewardSquareSum = 80000.0;
			count = 2;
			// i.e. we assume two visits at rewards of +200 and -200
			// this ensures the std Error on the reward starts high (at c.70)
			// to avoid std error of 0 just because the first two visits have the same reward
			nextStates = new Hashtable<HopshackleState, Integer>();
			if (!parentState.actions.contains(ae)) {
				parentState.addActionToState(ae);
			}
		}

		int getCountOfNextState(HopshackleState nextState) {
			if (nextStates.containsKey(nextState))
				return nextStates.get(nextState);
			return 0;
		}
		double getReward() {
			return rewardSum / (double)count;
		}

		double getDeviation() {
			double retValue;
			retValue = rewardSquareSum - Math.pow(rewardSum,2.0) / (double)count;
			retValue = retValue / (double)count;
			retValue = Math.pow(retValue, 0.5);
			return retValue;
		}
		double getErrorOnReward() {
			double retValue = getDeviation() /  Math.pow(count, 0.5);
			return retValue;
		}
		int getCount() {return count;}

		double getValueOfNextState() {
			double retValue = 0.0;
			int number = 0;
			int total = 0;
			for (HopshackleState ns : nextStates.keySet()) {
				number = nextStates.get(ns);
				retValue = retValue + ns.getValue() * number;
				total = total + number;
			}
			if (total == 0) return Math.max(stateBaseValue, parentState.value);	// optimism in face of uncertainty

			retValue = retValue / (double) total;
			return retValue;
		}
		double getErrorOnValueOfNextState() {
			valueSum = 0.0;
			valueSquareSum = 0.0;
			double stateValue = 0.0;
			double stateCount = 0.0;
			int totalStates = 0;
			for (HopshackleState ns : nextStates.keySet()) {
				stateValue = ns.getValue();
				stateCount = nextStates.get(ns);
				valueSum += stateValue * stateCount;
				valueSquareSum += stateValue * stateValue * stateCount;
				totalStates += stateCount;
			}
			// then add in two spurious points
			valueSum = valueSum + 0.0 + parentState.getValue() * 2.0;
			valueSquareSum = valueSquareSum + 0.0 + (parentState.getValue()*2.0)*(parentState.getValue()*2.0);
			totalStates += 2;

			double retValue;
			retValue = valueSquareSum - Math.pow(valueSum,2.0) / (double) totalStates;
			retValue = retValue / (double)totalStates;
			retValue = Math.pow(retValue, 0.5);
			retValue = retValue /  Math.pow(totalStates, 0.5);
			return retValue;
		}

		void addExperience(HopshackleState newState, double r) {
			rewardSum = rewardSum + r;
			count++;
			rewardSquareSum = rewardSquareSum + r * r;
			if (nextStates.containsKey(newState)) {
				int number = nextStates.get(newState);
				number++;
				nextStates.put(newState, number);
			} else 
				nextStates.put(newState, 1);
		}

		void addData(int n, HopshackleState newState, double r, double stddev_r) {
			// to be used when loading in data from record
			rewardSum = rewardSum + r * n;
			double temp = stddev_r * stddev_r;
			temp = temp * n;
			temp = temp + (r * n) * (r * n) / (double)n;
			rewardSquareSum += temp;
			count += n;

			if (nextStates.containsKey(newState)) {
				int currentNumber = nextStates.get(newState);
				nextStates.put(newState, currentNumber + n);
			} else {
				nextStates.put(newState, n);
			}
		}
	}
}
