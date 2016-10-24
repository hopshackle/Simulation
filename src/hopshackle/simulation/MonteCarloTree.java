package hopshackle.simulation;

import java.util.*;

public class MonteCarloTree<A extends ActionEnum<?>> {
	
	private Map<String, MCStatistics<A>> tree;
	private int updatesLeft;
	
	public MonteCarloTree() {
		tree = new HashMap<String, MCStatistics<A>>();
	}

	public boolean containsState(String state) {
		return tree.containsKey(state);
	}

	public void setUpdatesLeft(int n) {
		updatesLeft = n;
	}
	public int updatesLeft() {
		return updatesLeft;
	}
	public void insertState(String state, List<A> actions) {
		if (tree.containsKey(state))
			throw new AssertionError(state + " already included in MonteCarloTree");
		tree.put(state, new MCStatistics<A>(actions));
		updatesLeft--;
	}
	public void updateState(String state, A action, double reward) {
		if (tree.containsKey(state)) {
			MCStatistics<A> stats = tree.get(state);
			stats.update(action, reward);
		}
	}
	public A getNextAction(String state) {
		if (tree.containsKey(state)) {
			MCStatistics<A> stats = tree.get(state);
			if (stats.hasUntriedAction()) {
				return stats.getRandomUntriedAction();
			} else {
				return stats.getUCTAction();
			}
		} else {
			throw new AssertionError(state + " not found in MonteCarloTree to choose action");
		}
	}
}
