package hopshackle.simulation;

import java.util.*;

public class MCStatistics<P extends Agent> {

	private List<ActionEnum<P>> allActions;
	private MonteCarloTree<P> tree;
	private Map<String, MCData> map = new HashMap<String, MCData>();
	private Map<String, Set<String>> successorStatesByAction = new HashMap<String, Set<String>>();
	private int totalVisits = 0;

	private static double C = SimProperties.getPropertyAsDouble("MonteCarloUCTC", "1.0");
	private static double actionWeight = SimProperties.getPropertyAsDouble("MonteCarloPriorActionWeightingForBestAction", "0");
	private static String UCTType = SimProperties.getProperty("MonteCarloUCTType", "MC");

	public MCStatistics(List<ActionEnum<P>> possibleActions) {
		this(possibleActions, new MonteCarloTree<P>());
	}
	public MCStatistics(List<ActionEnum<P>> possibleActions, MonteCarloTree<P> tree) {
		allActions = HopshackleUtilities.cloneList(possibleActions);
		this.tree = tree;
	}
	public static void refresh() {
		actionWeight = SimProperties.getPropertyAsDouble("MonteCarloPriorActionWeightingForBestAction", "0");
	}
	public void update(ActionEnum<P> action, double reward) {
		update(action, new State<P>() {
			public double[] getAsArray() {return new double[0];}
			public String getAsString() {return "UNKNOWN";}
			public State<P> apply(ActionEnum<P> proposedAction) {return this;}
			public State<P> clone() {return this;}
		}, reward);
	}
	public void update(ActionEnum<P> action, State<P> nextState, double reward) {
		boolean nextStateInTree = tree.containsState(nextState);
		double V = reward;
		double Q = reward;
		if (!allActions.contains(action)) {
			addAction(action);
		}
		String key = action.toString();
		Set<String> currentStates = successorStatesByAction.get(key);
		if (currentStates == null) {
			currentStates = new HashSet<String>();
			successorStatesByAction.put(key, currentStates);
		}
		if (!currentStates.contains(nextState)) {
			currentStates.add(nextState.getAsString());
		}
		if (nextStateInTree) {
			MCStatistics<P> nextStateStats = tree.getStatisticsFor(nextState);
			V = nextStateStats.getV();
			Q = nextStateStats.getQ();
		}
		if (map.containsKey(key)) {
			MCData old = map.get(key);
			map.put(key, new MCData(old, reward, V, Q));
		} else {
			map.put(key, new MCData(key, 1, reward));
		}
		totalVisits++;
	}
	private void addAction(ActionEnum<P> newAction) {
		if (!allActions.contains(newAction))
			allActions.add(newAction);
	}
	public List<ActionEnum<P>> getPossibleActions() {
		return allActions;
	}

	public Set<String> getSuccessorStates() {
		Set<String> successors = new HashSet<String>();
		for (Set<String> states : successorStatesByAction.values()) {
			successors.addAll(states);
		}
		return successors;
	}
	public int getVisits() {
		return totalVisits;
	}
	public int getVisits(ActionEnum<P> action) {
		String key = action.toString();
		if (map.containsKey(key)) {
			return map.get(key).visits;
		} else {
			return 0;
		}
	}
	public double getMean(ActionEnum<P> action) {
		String key = action.toString();
		if (map.containsKey(key)) {
			return map.get(key).mean;
		} else {
			return 0.0;
		}
	}
	public double getV() {
		if (totalVisits == 0) return 0.0;
		double V = 0.0;
		for (String actionKey : map.keySet()) {
			MCData data = map.get(actionKey);
			V += data.V * data.visits;
		}
		return V / (double) totalVisits;
	}
	public double getQ() {
		if (totalVisits == 0) return 0.0;
		double Q = 0.0;
		for (String actionKey : map.keySet()) {
			MCData data = map.get(actionKey);
			if (data.Q > Q) Q = data.Q;
		}
		return Q;
	}

	public boolean hasUntriedAction(List<ActionEnum<P>> availableActions) {
		for (ActionEnum<P> action : availableActions) {
			if (!map.containsKey(action.toString())) {
				addAction(action);
				return true;
			}
		}
		return false;
	}

	public ActionEnum<P> getRandomUntriedAction(List<ActionEnum<P>> availableActions) {
		List<ActionEnum<P>> untried = new ArrayList<ActionEnum<P>>();
		for (ActionEnum<P> action : availableActions) {
			if (!map.containsKey(action.toString())) {
				untried.add(action);
				addAction(action);
			}
		}
		if (untried.isEmpty())
			throw new AssertionError("Cannot call getRandomUntriedAction is there aren't any");
		int diceRoll = Dice.roll(1, untried.size());
		return untried.get(diceRoll-1);
	}

	public ActionEnum<P> getUCTAction(List<ActionEnum<P>> availableActions) {
		if (hasUntriedAction(availableActions)) 
			throw new AssertionError("Should not be looking for UCT action while there are still untried actions");
		double best = Double.NEGATIVE_INFINITY;
		ActionEnum<P> retValue = null;
		for (ActionEnum<P> action : availableActions) {
			String key = action.toString();
			MCData data = map.get(key);
			double actionScore = score(data, key);
			double score = actionScore + C * Math.sqrt(Math.log(totalVisits) / (double)data.visits);
			if (score > best) {
				best = score;
				retValue = action;
			}
		}
		return retValue;
	}

	@Override
	public String toString() {
		StringBuffer retValue = new StringBuffer(
				String.format("MC Statistics\tVisits: %d\tV:%.2f\tQ:%.2f\n", totalVisits, getV(), getQ())
				);
		for (String k : keysInVisitOrder()) {
			String output = "";
			if (actionWeight > 0.0) {
				output = String.format("\t%s\t%s\t(AV:%.2f | %d)\n", k, map.get(k).toString(), tree.getActionValue(k), tree.getActionCount(k));
			} else {
				output = String.format("\t%s\t%s\n", k, map.get(k).toString());
			}
			retValue.append(output);
		}
		for (ActionEnum<P> action : allActions) {
			String key = action.toString();
			if (map.containsKey(key)) {
				// already reported
			} else {
				retValue.append("\t" + key + "\t No Data\n");
			}
		}
		return retValue.toString();
	}

	private List<String> keysInVisitOrder() {
		List<String> retValue = new ArrayList<String>();
		List<MCData> sortedByVisit = new ArrayList<MCData>();
		sortedByVisit.addAll(map.values());
		Collections.sort(sortedByVisit);
		Collections.reverse(sortedByVisit);
		for (MCData mcd : sortedByVisit) {
			retValue.add(mcd.key);
		}
		return retValue;
	}

	private double score(MCData data, String actionKey) {
		double baseValue = data.mean;
		if (UCTType == "Q") baseValue = data.Q;
		if (UCTType == "V") baseValue = data.V;
		if (actionWeight > 0.0)
			return (baseValue * data.visits + tree.getActionValue(actionKey) * actionWeight) / (actionWeight + data.visits);
		return baseValue;
	}

	public ActionEnum<P> getBestAction(List<ActionEnum<P>> availableActions) {
		ActionEnum<P> retValue = null;
		double score = Double.NEGATIVE_INFINITY;
		for (ActionEnum<P> action : availableActions) {
			String key = action.toString();
			if (map.containsKey(key)) {
				MCData data = map.get(key);
				double actionScore = score(data, key);
				if (actionScore > score) {
					score = actionScore;
					retValue = action;
				}
			}
		}
		return retValue;
	}
}

class MCData implements Comparable<MCData> {
	double mean, Q, V;
	int visits;
	int limit;
	String key;

	public MCData(String key, int n, double r, int limit) {
		visits = n;
		mean = r;
		Q = r;
		V = r;
		this.key = key;
		this.limit = limit;
		if (this.limit < 1) 
			this.limit = Integer.MAX_VALUE;
	}

	public MCData(String key, int n, double r) {
		this(key, n, r, 0);
	}

	public MCData(MCData old, double r) {
		this(old, r, r, r);
	}

	public MCData(MCData old, double r, double V, double Q) {
		this.key = old.key;
		limit = old.limit;
		visits = old.visits + 1;
		if (visits > limit) visits = limit;
		mean = old.mean + (r - old.mean) / (double) visits;
		this.V = old.V + (V - old.V) / (double) visits;
		this.Q = old.Q + (Q - old.Q) / (double) visits;
	}

	@Override
	public String toString() {
		return String.format("Visits:%d \tMC:%.2f \tV:%.2f \tQ:%.2f", visits, mean, V, Q);
	}

	@Override
	public int compareTo(MCData other) {
		return (this.visits - other.visits);
	}
}
