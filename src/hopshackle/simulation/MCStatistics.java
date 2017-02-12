package hopshackle.simulation;

import java.util.*;

public class MCStatistics<P extends Agent> {

	private List<ActionEnum<P>> allActions;
	private MonteCarloTree<P> tree;
	private Map<String, MCData> map = new HashMap<String, MCData>();
	private Map<String, Map<String, Integer>> successorStatesByAction = new HashMap<String, Map<String, Integer>>();
	private int totalVisits = 0;

	private static double C = SimProperties.getPropertyAsDouble("MonteCarloUCTC", "1.0");
	private static double actionWeight = SimProperties.getPropertyAsDouble("MonteCarloPriorActionWeightingForBestAction", "0");
	private static String UCTType = SimProperties.getProperty("MonteCarloUCTType", "MC");
	private static int minVisitsForQ = SimProperties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForQType", "0");
	private static int minVisitsForV = SimProperties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForVType", "0");

	public MCStatistics(List<ActionEnum<P>> possibleActions) {
		this(possibleActions, new MonteCarloTree<P>());
	}
	public MCStatistics(List<ActionEnum<P>> possibleActions, MonteCarloTree<P> tree) {
		allActions = HopshackleUtilities.cloneList(possibleActions);
		for (ActionEnum<P> a : possibleActions) {
			map.put(a.toString(), new MCData(a.toString()));
		}
		this.tree = tree;
	}
	public static void refresh() {
		actionWeight = SimProperties.getPropertyAsDouble("MonteCarloPriorActionWeightingForBestAction", "0");
		minVisitsForQ = SimProperties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForQType", "0");
		minVisitsForV = SimProperties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForVType", "0");
		MCData.refresh();
	}

	public void updateExcludingVisits(ActionEnum<P> action, double reward) {
		update(action, null, reward, false);
	}

	public void update(ActionEnum<P> action, double reward) {
		update(action, null, reward, true);
	}
	
	public void update(ActionEnum<P> action, State<P> nextState, double reward) {
		update(action, nextState, reward, true);
	}

	private void update(ActionEnum<P> action, State<P> nextState, double reward, boolean incrementVisits) {
		double V = reward;
		double Q = reward;
		if (!allActions.contains(action)) {
			addAction(action);
		}
		String key = action.toString();
		Map<String, Integer> currentStates = successorStatesByAction.get(key);
		if (currentStates == null) {
			currentStates = new HashMap<String, Integer>();
			successorStatesByAction.put(key, currentStates);
		}
		if (nextState != null) {
			boolean nextStateInTree = tree.containsState(nextState);
			String nextStateAsString = nextState.getAsString();
			if (!currentStates.containsKey(nextStateAsString)) {
				currentStates.put(nextStateAsString, 1);
			} else {
				currentStates.put(nextStateAsString, currentStates.get(nextStateAsString) + 1);
			}
			if (nextStateInTree) {
				MCStatistics<P> nextStateStats = tree.getStatisticsFor(nextState);
				V = nextStateStats.getV();
				Q = nextStateStats.getQ();
				if (V < 0.001) V = reward;
				if (Q < 0.001) Q = reward;
			}
		}
		if (map.containsKey(key)) {
			MCData old = map.get(key);
			MCData newMCD =  new MCData(old, reward, V, Q);
			if (!incrementVisits) newMCD.visits -= 1;
			map.put(key, newMCD);
		} else if (incrementVisits) {
			map.put(key, new MCData(key, reward));
		}
		if (incrementVisits) totalVisits++;
	}
	private void addAction(ActionEnum<P> newAction) {
		if (!allActions.contains(newAction))
			allActions.add(newAction);
	}
	public List<ActionEnum<P>> getPossibleActions() {
		return allActions;
	}

	public Map<String, Integer> getSuccessorStatesFrom(ActionEnum<P> action) {
		return successorStatesByAction.getOrDefault(action.toString(), new HashMap<String, Integer>());
	}

	public Set<String> getSuccessorStates() {
		Set<String> successors = new HashSet<String>();
		for (Map<String, Integer> states : successorStatesByAction.values()) {
			successors.addAll(states.keySet());
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
		if (MCData.useBaseValue) {
			if (totalVisits == 0) return MCData.baseValue;
		} else {
			if (totalVisits < minVisitsForV || totalVisits == 0) return 0.0;
		}
		double V = 0.0;
		for (String actionKey : map.keySet()) {
			MCData data = map.get(actionKey);
			V += data.V * data.visits;
		}
		return V / (double) totalVisits;
	}
	public double getQ() {
		int minVisits = minVisitsForQ;
		if (MCData.useBaseValue) {
			if (totalVisits == 0) return MCData.baseValue;
			minVisitsForQ = 0;
		} else {
			if (totalVisits == 0) return 0.0;
		}
		double Q = 0.0;
		for (String actionKey : map.keySet()) {
			MCData data = map.get(actionKey);
			if (data.Q > Q) Q = data.Q;
			if (data.visits < minVisits) minVisits = data.visits;
		}
		if (minVisits < minVisitsForQ) Q = 0.0;
		return Q;
	}

	private boolean hasActionBeenTried(ActionEnum<P> action) {
		if (!map.containsKey(action.toString())) {
			addAction(action);
			return false;
		} else {
			if (map.get(action.toString()).visits == 0)
				return false;
		}
		return true;
	}

	public boolean hasUntriedAction(List<ActionEnum<P>> availableActions) {
		for (ActionEnum<P> action : availableActions) {
			if (!hasActionBeenTried(action)) return true;
		}
		return false;
	}

	public ActionEnum<P> getRandomUntriedAction(List<ActionEnum<P>> availableActions) {
		List<ActionEnum<P>> untried = new ArrayList<ActionEnum<P>>();
		for (ActionEnum<P> action : availableActions) {
			if (!hasActionBeenTried(action)) untried.add(action);
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
			Map<String, Integer> successors = successorStatesByAction.getOrDefault(k, new HashMap<String, Integer>());
			for (String succKey : successors.keySet()) {
				if (tree.stateRef(succKey) != "") {
					retValue.append(String.format("\t\tState %s transitioned to %d times\n", tree.stateRef(succKey), successors.get(succKey)));
				}
			}
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
		if (UCTType.equals("Q")) baseValue = data.Q;
		if (UCTType.equals("V")) baseValue = data.V;
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

	private static double alpha = SimProperties.getPropertyAsDouble("Alpha", "0.05");
	protected static double baseValue = SimProperties.getPropertyAsDouble("MonteCarloRLBaseValue", "0.0");
	protected static boolean useBaseValue = SimProperties.getProperty("MonteCarloRL", "false").equals("true");
	private static int visitLimit = SimProperties.getPropertyAsInteger("MonteCarloActionVisitLimit", "0");
	static {
		if (visitLimit < 1) visitLimit = Integer.MAX_VALUE;
		if (!useBaseValue) baseValue = 0.0;
	}

	public static void refresh() {
		alpha = SimProperties.getPropertyAsDouble("Alpha", "0.05");
		baseValue = SimProperties.getPropertyAsDouble("MonteCarloRLBaseValue", "0.0");
		useBaseValue = SimProperties.getProperty("MonteCarloRL", "false").equals("true");
		visitLimit = SimProperties.getPropertyAsInteger("MonteCarloActionVisitLimit", "0");
	}

	double mean, Q, V;
	int visits;
	int limit = visitLimit;
	String key;

	public MCData(String key) {
		this(key, 0, baseValue);
	}

	public MCData(String key, double r) {
		this(key, 1, r);
	}

	private MCData(String key, int n, double r) {
		this.key = key;
		visits = n;
		mean = r;
		Q = r;
		V = r;
	}

	public MCData(MCData old, double r) {
		this(old, r, r, r);
	}

	public MCData(MCData old, double r, double V, double Q) {
		this.key = old.key;
		limit = old.limit;
		visits = old.visits + 1;
		double effectiveVisits = (visitLimit > visits) ? visits : visitLimit;
		mean = old.mean + (r - old.mean) / effectiveVisits;
		if (useBaseValue) {
			this.V = (1.0 - alpha) * old.V + alpha * V;
			this.Q = (1.0 - alpha) * old.Q + alpha * Q;
		} else {
			this.V = old.V + (V - old.V) / effectiveVisits;
			this.Q = old.Q + (Q - old.Q) / effectiveVisits;
		}
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
