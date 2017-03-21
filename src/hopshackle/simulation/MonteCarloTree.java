package hopshackle.simulation;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MonteCarloTree<P extends Agent> {

	private String UCTType;

	private Map<String, Integer> stateRefs = new HashMap<String, Integer>();
	private Map<String, MCStatistics<P>> tree;
	private int updatesLeft, maxActors;
	private List<Map<String, MCData>> actionValues;
	private static AtomicLong idFountain = new AtomicLong(1);
	private int nextRef = 1;
	private long id;
	private EntityLog entityLogger;
	protected boolean debug = false;
	protected DeciderProperties properties;

	public MonteCarloTree(DeciderProperties properties) {
		this (properties, 1);
	}

	public MonteCarloTree(DeciderProperties properties, int numberOfAgents) {
		maxActors = numberOfAgents;
		tree = new HashMap<String, MCStatistics<P>>();
		actionValues = new ArrayList<Map<String, MCData>>(numberOfAgents);
		for (int i = 0; i < numberOfAgents; i++)
			actionValues.add(i, new HashMap<String, MCData>());
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
		tree.put(stateAsString, new MCStatistics<P>(actions, this, maxActors, state.getActorRef()));
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
				double[] target = new double[maxActors];
				for (String successorStateString : successorMap.keySet()) {
					double stateVisits = successorMap.get(successorStateString);
					MCStatistics<P> successorStats = tree.get(successorStateString);
					if (successorStats == null) continue;
					if (UCTType.equals("Q")) {
						double[] Q = successorStats.getQ();
						for (int j =0; j < maxActors; j++) target[j] += stateVisits * Q[j];
					}
					if (UCTType.equals("V")) {
						double[] V = successorStats.getV();
						for (int j =0; j < maxActors; j++) target[j] += stateVisits * V[j];
					}
					for (int j =0; j < maxActors; j++) target[j] = target[j] / (double) successorStats.getVisits();
				}
				startStateStats.updateAsSweep(action, target);
			}
		}
	}

	public void updateState(State<P> state, ActionEnum<P> action, State<P> nextState, double reward) {
		this.updateState(state, action, nextState, toArray(reward));
	}

	public void updateState(State<P> state, ActionEnum<P> action, State<P> nextState, double[] reward) {
		String stateAsString = state.getAsString();
		if (debug) {
			String rewardString = "";
			for (int i = 0; i < reward.length; i++) rewardString = String.format("%s|%.2f", rewardString, reward[i]);
			log(String.format("Updating State %s to State %s with Action %s and reward %s", stateRef(stateAsString), stateRef(nextState.getAsString()), action.toString(), rewardString));
		}
		if (tree.containsKey(stateAsString)) {
			MCStatistics<P> stats = tree.get(stateAsString);
			//			if (debug) log(String.format("Before update: MC:%.2f\tV:%.2f\tQ:%.2f", stats.getMean(action), stats.getV(), stats.getQ()));
			stats.update(action, nextState, reward);
			if (debug) {
				String rewardString = "";
				for (int i = 0; i < reward.length; i++) rewardString = String.format("%s|%.2f", rewardString, stats.getMean(action)[i]);
				log("After update: " + rewardString);
				log("");
			}
		} else {
			if (debug) log("State not yet in tree");
		}
		String actionAsString = action.toString();
		int actingPlayer = state.getActorRef();
		double[] actionReward = new double[1];
		actionReward[0] = reward[actingPlayer];
		Map<String, MCData> av = actionValues.get(actingPlayer);
		if (av.containsKey(actionAsString)) {
			av.put(actionAsString, new MCData(av.get(actionAsString), actionReward));
		} else {
			av.put(actionAsString, new MCData(actionAsString, actionReward, properties));
		}
	}

	public void updateRAVE(State<P> state, ActionEnum<P> action, double[] reward) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString)) {
			if (debug) {
				String rewardString = "";
				for (int i = 0; i < reward.length; i++) rewardString = String.format("%s|%.2f", rewardString, reward[i]);
				log(String.format("Updating RAVE State %s for Action %s and reward %.2f", stateRef(stateAsString), action.toString(), reward));
			}
			MCStatistics<P> stats = tree.get(stateAsString);
			stats.updateRAVE(action, reward);
			if (debug) {
				String rewardString = "";
				for (int i = 0; i < reward.length; i++) rewardString = String.format("%s|%.2f", rewardString, reward[i]);
				log(String.format("After update: MC:%.2f\tV:%.2f\tQ:%.2f", stats.getMean(action), stats.getV(), stats.getQ()));
				log("");
			}
		} else {
			if (debug) log("State not yet in tree");
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
	public MCStatistics<P> getStatisticsFor(String state) {
		return tree.get(state);
	}
	public ActionEnum<P> getBestAction(State<P> state, List<ActionEnum<P>> possibleActions) {
		return tree.get(state.getAsString()).getBestAction(possibleActions);
	}
	public int numberOfStates() {
		return tree.size();
	}
	public List<String> getAllStatesWithMinVisits(int minV) {
		List<String> retValue = new ArrayList<String>();
		for (String key : tree.keySet()) {
			if (tree.get(key).getVisits() >= minV)
				retValue.add(key);
		}
		return retValue;
	}
	public double getActionValue(String k, int playerRef) {
		if (actionValues.get(playerRef-1).containsKey(k)) {
			return actionValues.get(playerRef-1).get(k).mean[0];
		} 
		return 0.0;
	}

	public int getActionCount(String k, int playerRef) {
		if (actionValues.get(playerRef-1).containsKey(k)) {
			return actionValues.get(playerRef-1).get(k).visits;
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

	public int[] getDepthsFrom(String root) {
		Map<String, Integer> depths = new HashMap<String, Integer>();
		double[] visitDepth = new double[10];
		int[] atDepth = new int[11];
		Queue<String> q = new LinkedBlockingQueue<String>();
		depths.put(root, 0);
		q.add(root);
		do {
			String state = q.poll();
			int currentDepth = depths.get(state);
			MCStatistics<P> toCopy = tree.get(state);
			if (toCopy != null) {
				for (String successor : toCopy.getSuccessorStates())
					if (!depths.containsKey(successor)) {
						depths.put(successor, currentDepth+1);
						int visits = tree.get(successor).getVisits();
						if (currentDepth < 10) {
							visitDepth[currentDepth] += visits;
							atDepth[currentDepth]++;
						}
						if (currentDepth+1 > atDepth[10]) atDepth[10] = currentDepth+1;
						q.add(successor);
					}
			}
		} while (!q.isEmpty());
		
		int[] retValue = new int[21];
		for (int i = 0; i < 11; i++) {
			retValue[i] = atDepth[i];
		}
		double totalVisits = tree.get(root).getVisits();
		for (int i = 0; i < 10; i++) {
			retValue[i+11] = (int) (100.0 * visitDepth[i] / totalVisits);
		}
		return retValue;
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

	private double[] toArray(double single) {
		double[] retValue = new double[1];
		retValue[0] = single;
		return retValue;
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
