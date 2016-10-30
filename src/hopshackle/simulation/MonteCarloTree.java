package hopshackle.simulation;

import java.util.*;

public class MonteCarloTree<P extends Agent, A extends ActionEnum<P>> {
	
	private Map<String, MCStatistics<P, A>> tree;
	private int updatesLeft;
	
	public MonteCarloTree() {
		tree = new HashMap<String, MCStatistics<P, A>>();
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
	public void insertState(State<P> state, List<A> actions) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString))
			throw new AssertionError(stateAsString + " already included in MonteCarloTree");
		tree.put(stateAsString, new MCStatistics<P, A>(actions));
		updatesLeft--;
	}
	public void updateState(State<P> state, A action, double reward) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString)) {
			MCStatistics<P, A> stats = tree.get(stateAsString);
			stats.update(action, reward);
		}
	}
	public A getNextAction(State<P> state, List<A> possibleActions) {
		String stateAsString = state.getAsString();
		if (tree.containsKey(stateAsString)) {
			MCStatistics<P, A> stats = tree.get(stateAsString);
			if (stats.hasUntriedAction(possibleActions)) {
				return stats.getRandomUntriedAction(possibleActions);
			} else {
				return stats.getUCTAction(possibleActions);
			}
		} else {
			throw new AssertionError(stateAsString + " not found in MonteCarloTree to choose action");
		}
	}
	public MCStatistics<P, A> getStatisticsFor(State<P> state) {
		return tree.get(state.getAsString());
	}
	public A getBestAction(State<P> state, List<A> possibleActions) {
		return tree.get(state.getAsString()).getBestAction(possibleActions);
	}
	public int numberOfStates() {
		return tree.size();
	}
	@Override
	public String toString() {
		return toString(false);
	}
	public String toString(boolean full) {
		StringBuffer retValue = new StringBuffer("Monte Carlo Tree ");
		retValue.append("with " + tree.size() + " states.\n");
		for (String s : tree.keySet()) {
			MCStatistics<P, A> stats = tree.get(s);
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
