package hopshackle.simulation;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MonteCarloTree<P extends Agent> {

	private String UCTType;

	private Map<String, Integer> stateRefs = new HashMap<String, Integer>();
	private Map<String, MCStatistics<P>> tree;
	private int updatesLeft;
	private Map<String, MCData> actionValues;
	private static AtomicLong idFountain = new AtomicLong(1);
	private int nextRef = 1;
	private long id;
	private EntityLog entityLogger;
	protected boolean debug = false;
	protected DeciderProperties properties;

	public MonteCarloTree(DeciderProperties properties) {
		tree = new HashMap<String, MCStatistics<P>>();
		actionValues = new HashMap<String, MCData>();
		this.properties = properties;
		UCTType = properties.getProperty("MonteCarloUCTType", "MC");
		id = idFountain.getAndIncrement();
	}

	public void reset() {
		tree.clear();
		stateRefs.clear();
		if (entityLogger != null) {
			entityLogger.close();
			entityLogger = null;
		}
		id = idFountain.getAndIncrement();
		nextRef = 1;
		// leave actionValues unchanged
	}

	public boolean containsState(String stateAsString) {
		return tree.containsKey(stateAsString);
	}
	public boolean containsState(State<?> state) {
		String stateAsString = state.getAsString();
		return containsState(stateAsString);
	}

	public void setUpdatesLeft(int n) {
		updatesLeft = n;
	}
	public int updatesLeft() {
		return updatesLeft;
	}
	public void insertState(State<P> state, List<ActionEnum<P>> actions) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString))
			return;
		tree.put(stateAsString, new MCStatistics<P>(actions, this));
		stateRefs.put(stateAsString, nextRef);
		nextRef++;
		updatesLeft--;
	}

	public void sweep() {
		// we run through each state, a random action from that state, and a random successor state for that action
		// and then run an update (with no reward)
		if (UCTType.equals("MC")) return;
		String[] allStates = tree.keySet().toArray(new String[0]);
		for (int i = 0; i < allStates.length; i++) {
			String startState = allStates[i];
			MCStatistics<P> startStateStats = tree.get(startState);
			for (ActionEnum<P> action : startStateStats.getPossibleActions()) {
				Map<String, Integer> successorMap = startStateStats.getSuccessorStatesFrom(action);
				double target = 0.0;
				for (String successorStateString : successorMap.keySet()) {
					double stateVisits = successorMap.get(successorStateString);
					MCStatistics<P> successorStats = tree.get(successorStateString);
					if (successorStats == null) continue;
					if (UCTType.equals("Q")) 
						target += stateVisits * successorStats.getQ();
					if (UCTType.equals("V")) 
						target += stateVisits * successorStats.getV();
					target = target / (double) successorStats.getVisits();
				}
				startStateStats.updateAsSweep(action, target);
			}
		}
	}

	public void updateState(State<P> state, ActionEnum<P> action, State<P> nextState, double reward) {
		String stateAsString = state.getAsString();
		if (debug) log(String.format("Updating State %s to State %s with Action %s and reward %.2f", stateRef(stateAsString), stateRef(nextState.getAsString()), action.toString(), reward));
		if (tree.containsKey(stateAsString)) {
			MCStatistics<P> stats = tree.get(stateAsString);
			if (debug) log(String.format("Before update: MC:%.2f\tV:%.2f\tQ:%.2f", stats.getMean(action), stats.getV(), stats.getQ()));
			stats.update(action, nextState, reward);
			if (debug) log(String.format("After update: MC:%.2f\tV:%.2f\tQ:%.2f", stats.getMean(action), stats.getV(), stats.getQ()));
			if (debug) log("");
		} else {
			if (debug) log("State not yet in tree");
		}
		String actionAsString = action.toString();
		if (actionValues.containsKey(actionAsString)) {
			actionValues.put(actionAsString, new MCData(actionValues.get(actionAsString), reward));
		} else {
			actionValues.put(actionAsString, new MCData(actionAsString, reward, properties));
		}
	}

	public ActionEnum<P> getNextAction(State<P> state, List<ActionEnum<P>> possibleActions) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString)) {
			MCStatistics<P> stats = tree.get(stateAsString);
			if (stats.hasUntriedAction(possibleActions)) {
				return stats.getRandomUntriedAction(possibleActions);
			} else {
				return stats.getUCTAction(possibleActions);
			}
		} else {
			throw new AssertionError(stateAsString + " not found in MonteCarloTree to choose action");
		}
	}
	public MCStatistics<P> getStatisticsFor(State<P> state) {
		return tree.get(state.getAsString());
	}
	public ActionEnum<P> getBestAction(State<P> state, List<ActionEnum<P>> possibleActions) {
		return tree.get(state.getAsString()).getBestAction(possibleActions);
	}
	public int numberOfStates() {
		return tree.size();
	}
	public double getActionValue(String k) {
		if (actionValues.containsKey(k)) {
			return actionValues.get(k).mean;
		} 
		return 0.0;
	}

	public int getActionCount(String k) {
		if (actionValues.containsKey(k)) {
			return actionValues.get(k).visits;
		} 
		return 0;
	}

	public void pruneTree(String newRoot) {
		/*
		- Create a new, empty tree as in refresh()
		- statesToCopy = empty Queue
		- statesToCopy.push(startState)
		- While statesToCopy not empty
			Pop state from statesToCopy
			Add all child states of state to statesToCopy
			Add state to new Tree
		 */
		Map<String, MCStatistics<P>> newTree = new HashMap<String, MCStatistics<P>>();
		Queue<String> q = new LinkedBlockingQueue<String>();
		Set<String> processed = new HashSet<String>();
		q.add(newRoot);
		do {
			String state = q.poll();
			processed.add(state);
			MCStatistics<P> toCopy = tree.get(state);
			if (toCopy != null) {
				newTree.put(state, toCopy);
				for (String successor : toCopy.getSuccessorStates())
					if (!processed.contains(successor))
						q.add(successor);
			}
		} while (!q.isEmpty());

		tree = newTree;
	}

	@Override
	public String toString() {
		return toString(false);
	}
	public String toString(boolean full) {
		StringBuffer retValue = new StringBuffer("Monte Carlo Tree ");
		retValue.append("with " + tree.size() + " states.\n");
		for (String s : tree.keySet()) {
			MCStatistics<P> stats = tree.get(s);
			retValue.append("\t" + s + "\t" + stats.getVisits() + " visits\n");
			if (full) {
				retValue.append("------------------\n");
				retValue.append(stats.toString(debug));
				retValue.append("------------------\n");
			}
		}
		return retValue.toString();
	}

	protected String stateRef(String stateDescription) {
		return stateRefs.getOrDefault(stateDescription, 0).toString();
	}

	public void log(String s) {
		if (entityLogger == null) {
			entityLogger = new EntityLog("MCTree_" + id, null);
		}
		entityLogger.log(s);
	}

	public void exportToFile(String fileName, String fromRoot) {
		File logFile = new File(EntityLog.logDir + File.separator + fileName + ".txt");
		try {
			FileWriter logWriter = new FileWriter(logFile, true);
			logWriter.write("Monte Carlo Tree with " + tree.size() + " states.\n");

			Queue<String> q = new LinkedBlockingQueue<String>();

			Set<String> processed = new HashSet<String>();
			q.add(fromRoot);
			do {
				String state = q.poll();
				processed.add(state);
				MCStatistics<P> toCopy = tree.get(state);
				if (toCopy != null) {
					for (String successor : toCopy.getSuccessorStates())
						if (tree.containsKey(successor) && !processed.contains(successor) && !q.contains(successor)) {
							q.add(successor);
						}
					logWriter.write("--------------------------------------" + EntityLog.newline);
					logWriter.write("State Reference: " + stateRef(state) + EntityLog.newline);
					logWriter.write(toCopy.toString() + EntityLog.newline);
				}
			} while (!q.isEmpty());
			logWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
		}
	}
}
