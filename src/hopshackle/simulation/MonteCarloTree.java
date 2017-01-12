package hopshackle.simulation;

import java.util.*;

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

	public boolean containsState(State<?> state) {
		String stateAsString = state.getAsString();
		return tree.containsKey(stateAsString);
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
			throw new AssertionError(stateAsString + " already included in MonteCarloTree");
		tree.put(stateAsString, new MCStatistics<P>(actions, this));
		updatesLeft--;
	}
	public void updateState(State<P> state, ActionEnum<P> action, double reward) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString)) {
			MCStatistics<P> stats = tree.get(stateAsString);
			stats.update(action, reward);
		}
		String actionAsString = action.toString();
		if (actionValues.containsKey(actionAsString)) {
			actionValues.put(actionAsString, new MCData(actionValues.get(actionAsString), reward));
		} else {
			actionValues.put(actionAsString, new MCData(actionAsString, 1, reward));
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
