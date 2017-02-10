package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class MonteCarloTree<P extends Agent> {

	private Map<String, MCStatistics<P>> tree;
	private int updatesLeft;
	private Map<String, MCData> actionValues;

	public MonteCarloTree() {
		tree = new HashMap<String, MCStatistics<P>>();
		actionValues = new HashMap<String, MCData>();
	}

	public void reset() {
		tree.clear();
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
		updatesLeft--;
	}

	public void updateState(State<P> state, ActionEnum<P> action, State<P> nextState, double reward) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString)) {
			MCStatistics<P> stats = tree.get(stateAsString);
			stats.update(action, nextState, reward);
		}
		String actionAsString = action.toString();
		if (actionValues.containsKey(actionAsString)) {
			actionValues.put(actionAsString, new MCData(actionValues.get(actionAsString), reward));
		} else {
			actionValues.put(actionAsString, new MCData(actionAsString, reward));
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
				retValue.append(stats.toString());
				retValue.append("------------------\n");
			}
		}
		return retValue.toString();
	}
}
