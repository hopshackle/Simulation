package hopshackle.simulation;

import java.util.*;

public class MCStatistics<P extends Agent> {

	private List<ActionEnum<P>> allActions;
	private MonteCarloTree<P> tree;
	private Map<String, MCData> map = new HashMap<String, MCData>();
	private Map<String, Map<String, Integer>> successorStatesByAction = new HashMap<String, Map<String, Integer>>();
	private int totalVisits = 0;
	private int actingAgent = -1;
	private boolean useBaseValue;
	private double C, actionWeight;
	private double[] baseValue;
	private String UCTType;
	private int minVisitsForQ, minVisitsForV;
	private int maxActors;

	public MCStatistics(List<ActionEnum<P>> possibleActions, DeciderProperties properties, int players, int actor) {
		this(possibleActions, new MonteCarloTree<P>(properties, players), players, actor);
	}
	public MCStatistics(List<ActionEnum<P>> possibleActions, MonteCarloTree<P> tree, int players, int actor) {
		allActions = HopshackleUtilities.cloneList(possibleActions);
		for (ActionEnum<P> a : possibleActions) {
			map.put(a.toString(), new MCData(a.toString(), tree.properties, players));
		}
		this.tree = tree;
		maxActors = players;
		actingAgent = actor-1;
		refresh();
	}
	public void refresh() {
		C = tree.properties.getPropertyAsDouble("MonteCarloUCTC", "1.0");
		UCTType = tree.properties.getProperty("MonteCarloUCTType", "MC");
		actionWeight = tree.properties.getPropertyAsDouble("MonteCarloPriorActionWeightingForBestAction", "0");
		minVisitsForQ = tree.properties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForQType", "0");
		minVisitsForV = tree.properties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForVType", "0");
		double base = tree.properties.getPropertyAsDouble("MonteCarloRLBaseValue", "0.0");
		useBaseValue = tree.properties.getProperty("MonteCarloRL", "false").equals("true");
		if (!useBaseValue) base = 0.0;
		baseValue = new double[maxActors];
		for (int i = 0; i < maxActors; i++) baseValue[i] = base;
	}

	public void updateAsSweep(ActionEnum<P> action, double[] reward) {
		update(action, null, reward, true);
	}

	public void update(ActionEnum<P> action, double[] reward) {
		update(action, null, reward, false);
	}

	public void update(ActionEnum<P> action, State<P> nextState, double[] reward) {
		update(action, nextState, reward, false);
	}

	private void update(ActionEnum<P> action, State<P> nextState, double[] reward, boolean sweep) {
		double[] V = reward;
		double[] Q = reward;
		if (!allActions.contains(action)) {
			addAction(action);
		}
		String key = action.toString();
		Map<String, Integer> currentStates = successorStatesByAction.get(key);
		if (currentStates == null) {
			currentStates = new HashMap<String, Integer>();
			successorStatesByAction.put(key, currentStates);
		}
		if (sweep && currentStates.isEmpty())
			return;
		// this is a leaf node, so we cannot update its value
		if (nextState != null && tree.containsState(nextState)) {
			String nextStateAsString = nextState.getAsString();
			if (!currentStates.containsKey(nextStateAsString)) {
				currentStates.put(nextStateAsString, 1);
			} else {
				currentStates.put(nextStateAsString, currentStates.get(nextStateAsString) + 1);
			}
			MCStatistics<P> nextStateStats = tree.getStatisticsFor(nextState);
			V = nextStateStats.getV();
			Q = nextStateStats.getQ();
			if (Q.length == 0) V = reward;
			if (Q.length == 0) Q = reward;
		}
		if (map.containsKey(key)) {
			MCData old = map.get(key);
			if (tree.debug) tree.log(String.format("\tInitial Action values MC:%.2f\tV:%.2f\tQ:%.2f", old.mean, old.V, old.Q));
			if (tree.debug) tree.log(String.format("\tAction update         MC:%.2f\tV:%.2f\tQ:%.2f", reward, V, Q));
			MCData newMCD =  new MCData(old, reward, V, Q, sweep);
			if (tree.debug) tree.log(String.format("\tNew Action values     MC:%.2f\tV:%.2f\tQ:%.2f", newMCD.mean, newMCD.V, newMCD.Q));
			map.put(key, newMCD);
		} else if (!sweep) {
			map.put(key, new MCData(key, reward, tree.properties));
		}
		if (!sweep) totalVisits++;
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
	public double[] getMean(ActionEnum<P> action) {
		String key = action.toString();
		if (map.containsKey(key)) {
			return map.get(key).mean;
		} else {
			return new double[maxActors];
		}
	}
	public double[] getV() {
		if (useBaseValue) {
			if (totalVisits == 0) return baseValue;
		} else {
			if (totalVisits < minVisitsForV || totalVisits == 0) return new double[0];
		}
		double[] V = new double[maxActors];
		for (String actionKey : map.keySet()) {
			MCData data = map.get(actionKey);
			for (int i = 0; i < maxActors; i++) V[i] += data.V[i] * data.visits;
		}
		for (int i = 0; i < maxActors; i++) V[i] = V[i] / (double) totalVisits;
		return V;
	}
	public double[] getQ() {
		int minVisits = minVisitsForQ;
		if (useBaseValue) {
			if (totalVisits == 0) return baseValue;
			minVisitsForQ = 0;
		} else {
			if (totalVisits == 0) return new double[0];
		}
		double Q[] = new double[maxActors];
		for (String actionKey : map.keySet()) {
			MCData data = map.get(actionKey);
			if (data.Q[actingAgent] > Q[actingAgent]) Q = data.Q;
			if (data.visits < minVisits) minVisits = data.visits;
		}
		if (minVisits < minVisitsForQ) Q = new double[0];
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
		return toString(false);
	}

	public String toString(boolean verbose) {
		StringBuffer retValue = new StringBuffer(
				String.format("MC Statistics\tVisits: %d\tV:%.2f\tQ:%.2f\n", totalVisits, getV(), getQ())
				);
		for (String k : keysInVisitOrder()) {
			String output = "";
			if (actionWeight > 0.0) {
				output = String.format("\t%s\t%s\t(AV:%.2f | %d)\n", k, map.get(k).toString(), tree.getActionValue(k, actingAgent), tree.getActionCount(k, actingAgent));
			} else {
				output = String.format("\t%s\t%s\n", k, map.get(k).toString());
			}
			retValue.append(output);
			if (verbose) {
				Map<String, Integer> successors = successorStatesByAction.getOrDefault(k, new HashMap<String, Integer>());
				for (String succKey : successors.keySet()) {
					if (tree.stateRef(succKey) != "0") {
						retValue.append(String.format("\t\tState %s transitioned to %d times\n", tree.stateRef(succKey), successors.get(succKey)));
					}
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
		double baseValue = data.mean[actingAgent];
		if (UCTType.equals("Q")) baseValue = data.Q[actingAgent];
		if (UCTType.equals("V")) baseValue = data.V[actingAgent];
		if (actionWeight > 0.0)
			return (baseValue * data.visits + tree.getActionValue(actionKey, actingAgent) * actionWeight) / (actionWeight + data.visits);
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

	private double alpha;
	private double[] baseValue;
	protected boolean useBaseValue;
	private int visitLimit;

	public void refresh(DeciderProperties properties) {
		alpha = properties.getPropertyAsDouble("Alpha", "0.05");
		double base = properties.getPropertyAsDouble("MonteCarloRLBaseValue", "0.0");
		useBaseValue = properties.getProperty("MonteCarloRL", "false").equals("true");
		visitLimit = properties.getPropertyAsInteger("MonteCarloActionVisitLimit", "0");
		if (visitLimit < 1) visitLimit = Integer.MAX_VALUE;
		if (!useBaseValue) base = 0.0;
		baseValue = new double[maxActors];
		for (int i = 0; i < maxActors; i++) baseValue[i] = base;
	}

	int maxActors;
	double[] mean, Q, V;
	int visits;
	int limit = visitLimit;
	String key;

	public MCData(String key, DeciderProperties properties, int players) {
		this(key, 0, new double[players], properties);
	}

	public MCData(String key, double[] r, DeciderProperties properties) {
		this(key, 1, r, properties);
	}

	private MCData(String key, int n, double[] r, DeciderProperties properties) {
		refresh(properties);
		maxActors = r.length;
		mean = new double[maxActors];
		Q = new double[maxActors];
		V = new double[maxActors];
		if (useBaseValue && n == 0) r = baseValue;
		this.key = key;
		visits = n;
		mean = r;
		Q = r;
		V = r;
	}

	public MCData(MCData old, double[] r) {
		this(old, r, r, r);
	}

	public MCData(MCData old, double[] r, double[] V, double[] Q) {
		this(old, r, V, Q, false);
	}

	public MCData(MCData old, double[] r, double[] V, double[] Q, boolean sweep) {
		useBaseValue = old.useBaseValue;
		maxActors = old.maxActors;
		alpha = old.alpha;
		visitLimit = old.visitLimit;
		baseValue = old.baseValue;
		if (sweep && !useBaseValue) {
			throw new AssertionError("Sweeping only to be used with RL");
		}
		this.key = old.key;
		limit = old.limit;
		visits = sweep ? old.visits : old.visits + 1;
		double effectiveVisits = (visitLimit > visits) ? visits : visitLimit;
		if (sweep) {
			mean = old.mean;
		} else {
			for (int i = 0; i < maxActors; i++) {
				mean[i] = old.mean[i] + (r[i] - old.mean[i]) / effectiveVisits;
			}
		}
		if (useBaseValue) {
			for (int i = 0; i < maxActors; i++) {
				this.V[i] = (1.0 - alpha) * old.V[i] + alpha * V[i];
				this.Q[i] = (1.0 - alpha) * old.Q[i] + alpha * Q[i];
			}
		} else {
			for (int i = 0; i < maxActors; i++) {
				this.V[i] = old.V[i] + (V[i] - old.V[i]) / effectiveVisits;
				this.Q[i] = old.Q[i] + (Q[i] - old.Q[i]) / effectiveVisits;
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer retValue = new StringBuffer();
		for (int i = 0; i < maxActors; i++) {
			retValue.append(String.format("Player:%d\tVisits:%d \tMC:%.2f \tV:%.2f \tQ:%.2f", i+1, visits, mean[i], V[i], Q[i]));
		}
		return retValue.toString();
	}

	@Override
	public int compareTo(MCData other) {
		return (this.visits - other.visits);
	}
}
