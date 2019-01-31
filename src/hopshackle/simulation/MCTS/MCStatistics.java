package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;

import java.util.*;
import java.util.stream.Collectors;

import org.javatuples.*;

public class MCStatistics<P extends Agent> {

    private List<ActionWithRef<P>> allActions = new ArrayList<>();
    private MonteCarloTree<P> tree;
    private Map<ActionWithRef<P>, MCData> map = new HashMap<>();
    private Map<ActionWithRef<P>, MCData> RAVE = new HashMap<>();
    private Map<ActionWithRef<P>, Map<String, Integer>> successorStatesByAction = new HashMap<>();
    private Map<ActionWithRef<P>, MCStatistics<P>> successorNodesByAction = new HashMap<>();
    private Map<ActionWithRef<P>, Integer> visitValidity = new HashMap<>();
    private int totalVisits = 0;
    private int RAVEVisits = 0;
    private boolean useBaseValue, offlineHeuristicOnExpansion, offlineHeuristicOnSelection;
    private boolean robustMC, simpleMC, interpolateExploration, parentalVisitcount;
    private String interpolationMethod;
    private double C, heuristicWeight;
    private double[] baseValue;
    private String UCTType;
    private int minVisitsForQ, minVisitsForV;
    private State<P> state;
    private boolean openLoop;

    // test method only
    public MCStatistics(DeciderProperties properties, int players, State<P> state) {
        this(new TranspositionTableMCTree<>(properties, players), state);
    }

    public MCStatistics(MonteCarloTree<P> tree, State<P> state) {
        this.state = state;
        this.tree = tree;
        if (tree instanceof OpenLoopMCTree) openLoop = true;
        // TODO: At some point I should refactor to split this out to a separate class
        refresh();
    }

    public void refresh() {
        C = tree.properties.getPropertyAsDouble("MonteCarloUCTC", "1.0");
        UCTType = tree.properties.getProperty("MonteCarloUCTType", "MC");
        heuristicWeight = tree.properties.getPropertyAsDouble("MonteCarloHeuristicWeighting", "0");
        interpolationMethod = tree.properties.getProperty("MonteCarloHeuristicInterpolationMethod", "default");
        interpolateExploration = tree.properties.getProperty("MonteCarloInterpolateExploration", "false").equals("true");
        minVisitsForQ = tree.properties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForQType", "0");
        minVisitsForV = tree.properties.getPropertyAsInteger("MonteCarloMinVisitsOnActionForVType", "0");
        offlineHeuristicOnExpansion = tree.properties.getProperty("MonteCarloHeuristicOnExpansion", "false").equals("true");
        offlineHeuristicOnSelection = tree.properties.getProperty("MonteCarloHeuristicOnSelection", "false").equals("true");
        double base = tree.properties.getPropertyAsDouble("MonteCarloRLBaseValue", "0.0");
        useBaseValue = tree.properties.getProperty("MonteCarloRL", "false").equals("true");
        robustMC = tree.properties.getProperty("MonteCarloChoice", "default").equals("robust");
        simpleMC = tree.properties.getProperty("MonteCarloChoice", "default").equals("simple");
        parentalVisitcount = tree.properties.getProperty("MonteCarloParentalVisitValidity", "false").equals("true");
        if (!useBaseValue) base = 0.0;
        baseValue = new double[tree.maxActors];
        for (int i = 0; i < tree.maxActors; i++) baseValue[i] = base;
    }

    public MCStatistics<P> update(ActionEnum<P> action, double[] reward, int agentRef) {
        return update(action, null, reward, agentRef);
    }

    // only public for testing
    public MCStatistics<P> update(ActionEnum<P> action, State<P> nextState, double[] reward, int agentRef) {
        double[] V = reward;
        double[] Q = reward;

        ActionWithRef<P> actionRef = new ActionWithRef<>(action, agentRef);
        addAction(actionRef);
        MCStatistics<P> nextNode = nextNodeFrom(actionRef, nextState);

        // if this is a leaf node we cannot update its value
        if (nextNode != null) {
            V = nextNode.getV(agentRef);
            Q = nextNode.getQ(agentRef);
            if (V.length == 0) V = reward;
            if (Q.length == 0) Q = reward;
        }
        if (map.containsKey(actionRef)) {
            MCData old = map.get(actionRef);
            if (tree.debug)
                tree.log(String.format("\tInitial Action values MC:%.2f\tV:%.2f\tQ:%.2f", old.mean[agentRef], old.V[agentRef], old.Q[agentRef]));
            if (tree.debug)
                tree.log(String.format("\tAction update         MC:%.2f\tV:%.2f\tQ:%.2f", reward[agentRef], V[agentRef], Q[agentRef]));
            MCData newMCD = new MCData(old, reward, V, Q);
            if (tree.debug)
                tree.log(String.format("\tNew Action values     MC:%.2f\tV:%.2f\tQ:%.2f", newMCD.mean[agentRef], newMCD.V[agentRef], newMCD.Q[agentRef]));
            map.put(actionRef, newMCD);
        } else {
            map.put(actionRef, new MCData(actionRef.toString(), reward, tree.properties));
        }
        totalVisits++;
        return nextNode;
    }

    protected void updateRAVE(ActionEnum<P> action, double[] reward, int agentRef) {
        ActionWithRef<P> key = new ActionWithRef<>(action, agentRef);
        if (RAVE.containsKey(key)) {
            MCData old = RAVE.get(key);
            MCData newMCD = new MCData(old, reward);
            RAVE.put(key, newMCD);
        } else {
            RAVE.put(key, new MCData(key.toString(), reward, tree.properties));
        }
        RAVEVisits++;
    }

    public double getRAVEValue(ActionEnum<P> action, double exploreC, int player) {
        return getRAVEValue(new ActionWithRef<>(action, player), exploreC);
    }

    public double getRAVEValue(ActionWithRef key, double exploreC) {
        MCData data = RAVE.get(key);
        if (data == null) return 0.0;
        double retValue = data.mean[key.agentRef - 1];
        retValue += exploreC * Math.sqrt(Math.log(RAVEVisits) / data.visits);
        return retValue;
    }

    private void addAction(ActionWithRef<P> actionRef) {
        if (!allActions.contains(actionRef)) {
            allActions.add(actionRef);
            if (map.containsKey(actionRef))
                throw new AssertionError("MCData already exists");
            map.put(actionRef, new MCData(actionRef.toString(), tree.properties, tree.maxActors));
            visitValidity.put(actionRef, 0);
        }
    }

    private MCStatistics<P> nextNodeFrom(ActionWithRef<P> actionRef, State<P> nextState) {
        MCStatistics<P> nextNode = null;
        if (openLoop) {
            nextNode = successorNodesByAction.get(actionRef);
            if (nextNode == null && tree.updatesLeft() > 0) {
                nextNode = new MCStatistics<>(tree, null);
                successorNodesByAction.put(actionRef, nextNode);
                tree.setUpdatesLeft(tree.updatesLeft() - 1);
            }
        } else if (nextState != null) {
            Map<String, Integer> currentStates = successorStatesByAction.computeIfAbsent(actionRef, k -> new HashMap<>());
            if (tree.withinTree(nextState)) {
                String nextStateAsString = nextState.getAsString();
                if (!currentStates.containsKey(nextStateAsString)) {
                    currentStates.put(nextStateAsString, 1);
                } else {
                    currentStates.put(nextStateAsString, currentStates.get(nextStateAsString) + 1);
                }
            }
            nextNode = tree.getStatisticsFor(nextState);
        }
        return nextNode;
    }

    public List<ActionWithRef<P>> getPossibleActions() {
        return allActions;
    }

    public List<ActionEnum<P>> getPossibleActions(int agent) {
        return allActions.stream()
                .filter(ar -> ar.agentRef == agent)
                .map(ar -> ar.actionTaken)
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getSuccessorStatesFrom(ActionWithRef<P> actionRef) {
        return successorStatesByAction.getOrDefault(actionRef, new HashMap());
    }

    public Set<String> getSuccessorStates() {
        Set<String> successors = new HashSet<>();
        for (Map<String, Integer> states : successorStatesByAction.values()) {
            successors.addAll(states.keySet());
        }
        return successors;
    }

    public MCStatistics<P> getSuccessorNode(ActionWithRef<P> actionRef) {
        return successorNodesByAction.getOrDefault(actionRef, null);
    }

    public int getVisits() {
        return totalVisits;
    }

    public int getVisits(ActionWithRef<P> key) {
        return map.containsKey(key) ? map.get(key).visits : 0;
    }

    public int getVisits(ActionEnum<P> action) {
        return map.keySet().stream()
                .filter(ar -> ar.actionTaken.equals(action))
                .mapToInt(ar -> map.get(ar).visits)
                .sum();
    }

    public double[] getMean(ActionEnum<P> action, int agentRef) {
        ActionWithRef<P> key = new ActionWithRef<>(action, agentRef);
        if (map.containsKey(key)) {
            return map.get(key).mean;
        } else {
            return new double[tree.maxActors];
        }
    }

    public double[] getV(int agentRef) {
        if (useBaseValue) {
            if (totalVisits == 0) return baseValue;
        } else {
            if (totalVisits < minVisitsForV || totalVisits == 0) return new double[0];
        }
        double[] V = new double[tree.maxActors];
        int effectiveVisits = 0;
        for (ActionWithRef actionKey : map.keySet()) {
            if (actionKey.agentRef != agentRef)
                continue;
            MCData data = map.get(actionKey);
            effectiveVisits += data.visits;
            for (int i = 0; i < tree.maxActors; i++) V[i] += data.V[i] * data.visits;
        }
        if (effectiveVisits < minVisitsForV || effectiveVisits == 0) return new double[0];
        for (int i = 0; i < tree.maxActors; i++) V[i] = V[i] / (double) effectiveVisits;
        return V;
    }

    public double[] getQ(int agentRef) {
        int minVisits = minVisitsForQ;
        if (useBaseValue) {
            if (totalVisits == 0) return baseValue;
            minVisitsForQ = 0;
        } else {
            if (totalVisits == 0) return new double[0];
        }
        double Q[] = new double[tree.maxActors];
        for (ActionWithRef key : map.keySet()) {
            if (key.agentRef != agentRef)
                continue;
            MCData data = map.get(key);
            if (data.visits < minVisits) minVisits = data.visits;
            for (int player = 0; player < tree.maxActors; player++) {
                if (data.Q[player] > Q[player]) Q[player] = data.Q[player];
            }
        }
        if (minVisits < minVisitsForQ) Q = new double[0];
        return Q;
    }

    private boolean hasActionBeenTried(ActionEnum<P> action, int player) {
        ActionWithRef<P> key = new ActionWithRef<>(action, player);
        if (!map.containsKey(key)) {
            addAction(key);
            return false;
        } else return map.get(key).visits != 0;
    }

    public boolean hasUntriedAction(List<ActionEnum<P>> availableActions, int player) {
        for (ActionEnum<P> action : availableActions) {
            if (!hasActionBeenTried(action, player)) return true;
        }
        return false;
    }

    private void updateParentalVisits(List<ActionEnum<P>> actions, int decidingAgent) {
        actions.forEach(a -> {
            ActionWithRef<P> actionWithRef = new ActionWithRef<>(a, decidingAgent);
            visitValidity.put(actionWithRef, visitValidity.getOrDefault(actionWithRef, 0) + 1);
        });
    }

    public ActionEnum<P> getRandomUntriedAction(List<ActionEnum<P>> availableActions, int decidingAgent) {
        BaseStateDecider<P> heuristic = tree == null ? new noHeuristic<P>() : tree.getOfflineHeuristic();
        return getRandomUntriedAction(availableActions, heuristic, decidingAgent);
    }

    public ActionEnum<P> getRandomUntriedAction(List<ActionEnum<P>> availableActions, BaseStateDecider<P> heuristic, int decidingAgent) {
        List<ActionEnum<P>> untried = new ArrayList<ActionEnum<P>>();
        for (ActionEnum<P> action : availableActions) {
            if (!hasActionBeenTried(action, decidingAgent)) untried.add(action);
        }
        if (untried.isEmpty())
            throw new AssertionError("Cannot call getRandomUntriedAction if there aren't any");
        if (offlineHeuristicOnExpansion) {
            List<Double> values = heuristic.valueOptions(untried, state, decidingAgent);
            double maxValue = Collections.max(values);
            List<ActionEnum<P>> copy = untried;
            untried = new ArrayList<>();
            for (int i = 0; i < copy.size(); i++) {
                if (values.get(i) > maxValue - 0.001) untried.add(copy.get(i));
            }
        }
        int diceRoll = Dice.roll(1, untried.size());
        return untried.get(diceRoll - 1);
    }

    public ActionEnum<P> getNextAction(List<ActionEnum<P>> availableActions, int player) {
        ActionEnum<P> retValue = hasUntriedAction(availableActions, player)
                ? getRandomUntriedAction(availableActions, player)
                : getUCTAction(availableActions, player);

        if (parentalVisitcount) updateParentalVisits(availableActions, player);
        return retValue;
    }

    public ActionEnum<P> getUCTAction(List<ActionEnum<P>> availableActions, int player) {
        return getUCTAction(availableActions, tree != null ? tree.getOfflineHeuristic() : new noHeuristic<>(), player);
    }

    public ActionEnum<P> getUCTAction(List<ActionEnum<P>> availableActions, BaseStateDecider<P> heuristic, int player) {
        if (hasUntriedAction(availableActions, player))
            throw new AssertionError("Should not be looking for UCT action while there are still untried actions");
        return getAction(availableActions, heuristic, false, player);
    }

    // returns array of baseActionScore, explorationTerm, n, N
    public double[] getUCTValue(ActionEnum<P> action, int player) {
        double[] retValue = new double[4];
        ActionWithRef<P> key = new ActionWithRef<>(action, player);
        MCData data = map.get(key);
        if (data == null) {
            addAction(key);
            data = map.get(key);
        }
        double actionScore = score(data, player);
        double visits = (double) data.visits;
        double totalEffectiveVisits = parentalVisitcount ? visitValidity.get(key) : totalVisits;
        double coreExplorationTerm = visits == 0 ? Double.MAX_VALUE : C * Math.sqrt(Math.log(totalEffectiveVisits > 0 ? totalEffectiveVisits : 1) / visits);
        retValue[0] = actionScore;
        retValue[1] = coreExplorationTerm;
        retValue[2] = visits;
        retValue[3] = totalEffectiveVisits;
        return retValue;
    }

    private ActionEnum<P> getAction(List<ActionEnum<P>> availableActions, BaseStateDecider<P> heuristic, boolean bestAction, int player) {
        double best = Double.NEGATIVE_INFINITY;
        ActionEnum<P> retValue = null;
        List<Double> heuristicValues = heuristic.valueOptions(availableActions, state, player);
        boolean sqrtInterpolation = interpolationMethod.equals("RAVE");
        for (ActionEnum<P> action : availableActions) {
            double[] UCTValues = getUCTValue(action, player);
            double actionScore = UCTValues[0];
            double coreExplorationTerm = bestAction ? 0.0 : UCTValues[1];
            double score = actionScore + coreExplorationTerm; // the vanilla result without heuristics
            if (offlineHeuristicOnSelection) {
                int i = availableActions.indexOf(action);
                // we weight the heuristic value as a number of equivalent visits
                double beta = heuristicWeight / (heuristicWeight + UCTValues[2]);
                if (sqrtInterpolation)
                    beta = Math.sqrt(heuristicWeight / (3 * UCTValues[3] + heuristicWeight));

                if (interpolateExploration)
                    score = beta * heuristicValues.get(i) + (1.0 - beta) * score;
                else
                    score = beta * heuristicValues.get(i) + (1.0 - beta) * actionScore + coreExplorationTerm;
            }
            if (score > best) {
                best = score;
                retValue = action;
            }
        }
        if (retValue == null)
            return getRandomUntriedAction(availableActions, player);
        return retValue;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean verbose) {
        double[] V = getV(getMostCommonActor());
        double[] Q = getQ(getMostCommonActor());
        StringBuilder retValue = new StringBuilder(String.format("MC Statistics\tVisits: %d\tV|Q", totalVisits));
        for (int i = 0; i < V.length; i++)
            retValue.append(String.format("\t[%.2f|%.2f]", V[i], Q[i]));
        retValue.append("\n");
        for (ActionWithRef<P> k : keysInVisitOrder()) {
            String output = "";
            if (heuristicWeight > 0.0) {
                double[] heuristicValues = new double[tree.maxActors];
                for (int i = 0; i < tree.maxActors; i++) {
                    heuristicValues[i] = tree.getOfflineHeuristic().valueOption(k.actionTaken, state, i + 1);
                }
                String heuristicString = HopshackleUtilities.formatArray(heuristicValues, "|", "%.2f");
                output = String.format("\t%s\t%s\t(H:%s)\n", k.toString(), map.get(k).toString(), heuristicString);
                //        output = String.format("\t%s\t%s\t(AV:%.2f | %d)\n", k, map.get(k).toString(), tree.getActionValue(k, actingAgent+1), tree.getActionCount(k, actingAgent+1));
            } else {
                output = String.format("\t%-35s\t%s\n", k.toString(), map.get(k).toString());
            }
            retValue.append(output);
            if (verbose) {
                Map<String, Integer> successors = successorStatesByAction.getOrDefault(k, new HashMap<String, Integer>());
                for (String succKey : successors.keySet()) {
                    if (!tree.stateRef(succKey).equals("0")) {
                        retValue.append(String.format("\t\tState %s transitioned to %d times\n", tree.stateRef(succKey), successors.get(succKey)));
                    }
                }
            }
        }
        return retValue.toString();
    }

    private List<ActionWithRef<P>> keysInVisitOrder() {
        Map<String, ActionWithRef<P>> reverseKey = new HashMap<>();
        for (ActionWithRef<P> key : map.keySet()) {
            reverseKey.put(map.get(key).key, key);
        }
        List<ActionWithRef<P>> retValue = new ArrayList<>();
        List<MCData> sortedByVisit = new ArrayList<>();
        sortedByVisit.addAll(map.values());
        Collections.sort(sortedByVisit);
        Collections.reverse(sortedByVisit);
        for (MCData mcd : sortedByVisit) {
            retValue.add(reverseKey.get(mcd.key));
        }
        return retValue;
    }

    private double score(MCData data, int forAgent) {
        double baseValue = data.mean[forAgent - 1];
        if (UCTType.equals("Q")) baseValue = data.Q[forAgent - 1];
        if (UCTType.equals("V")) baseValue = data.V[forAgent - 1];
        return baseValue;
    }

    public int getActorRef() {
        if (state == null || state.getActorRef() == -1) {
            Integer[] actors = actorsFrom().toArray(new Integer[1]);
            if (actors.length > 1) {
                throw new AssertionError("Only expect one possible actor from each node");
            }
            return actors[0];
        }
        return state.getActorRef();
    }

    public State<P> getReferenceState() {
        return state;
    }

    public Set<Integer> actorsFrom() {
        Set<Integer> retValue = new HashSet<>();
        for (ActionWithRef key : map.keySet()) {
            retValue.add(key.agentRef);
        }
        return retValue;
    }

    public int getMostCommonActor() {
        int[] count = new int[tree.maxActors];
        for (ActionWithRef key : map.keySet()) {
            MCData data = map.get(key);
            count[key.agentRef - 1] += data.visits;
        }
        int maxVisit = 0;
        int maxVisitor = 0;
        for (int i = 0; i < tree.maxActors; i++) {
            if (count[i] > maxVisit) {
                maxVisit = count[i];
                maxVisitor = i + 1;
            }
        }
        return maxVisitor;
    }

    public ActionEnum<P> getBestAction(List<ActionEnum<P>> availableActions, int player) {
        if (robustMC || simpleMC) {
            ActionEnum<P> retValue = null;
            double score = Double.NEGATIVE_INFINITY;
            for (ActionEnum<P> action : availableActions) {
                ActionWithRef<P> key = new ActionWithRef<>(action, player);
                if (map.containsKey(key)) {
                    MCData data = map.get(key);
                    double actionScore = robustMC ? data.visits : score(data, player);
                    // Robust MC uses the number of visits, simple MC uses just the final score
                    // with no heuristics at all
                    if (actionScore > score) {
                        score = actionScore;
                        retValue = action;
                    }
                }
            }
            if (retValue == null)
                return getRandomUntriedAction(availableActions, player);
            return retValue;
        } else {
            // otherwise, we use all the standard heuristics, but without any C
            return getAction(availableActions, tree.getOfflineHeuristic(), true, player);
        }
    }
}

class MCData implements Comparable<MCData> {

    private double alpha;
    private double[] baseValue;
    private boolean useBaseValue;
    private int visitLimit;
    int maxActors;
    double[] mean, Q, V;
    int visits;
    int limit = visitLimit;
    String key;


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


    public MCData(String key, DeciderProperties properties, int players) {
        this(key, 0, new double[players], properties);
    }

    public MCData(String key, double[] r, DeciderProperties properties) {
        this(key, 1, r, properties);
    }

    private MCData(String key, int n, double[] r, DeciderProperties properties) {
        maxActors = r.length;
        refresh(properties);
        mean = new double[maxActors];
        Q = new double[maxActors];
        V = new double[maxActors];
        if (useBaseValue && n == 0) r = baseValue;
        for (int i = 0; i < maxActors; i++) {
            mean[i] = r[i];
            Q[i] = r[i];
            V[i] = r[i];
        }
        this.key = key;
        visits = n;
    }

    public MCData(MCData old, double[] r) {
        this(old, r, r, r);
    }

    public MCData(MCData old, double[] r, double[] V, double[] Q) {
        useBaseValue = old.useBaseValue;
        maxActors = old.maxActors;
        alpha = old.alpha;
        visitLimit = old.visitLimit;
        baseValue = old.baseValue;
        this.key = old.key;
        limit = old.limit;
        visits = old.visits + 1;
        double effectiveVisits = (visitLimit > visits) ? visits : visitLimit;
        mean = new double[maxActors];
        this.Q = new double[maxActors];
        this.V = new double[maxActors];

        for (int i = 0; i < maxActors; i++) {
            mean[i] = old.mean[i] + (r[i] - old.mean[i]) / effectiveVisits;
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
        StringBuilder retValue = new StringBuilder();
        retValue.append(String.format("Visits:%d\tMC|V|Q", visits));
        for (int i = 0; i < maxActors; i++) {
            retValue.append(String.format("\t[%.2f|%.2f|%.2f]", mean[i], V[i], Q[i]));
        }
        return retValue.toString();
    }

    @Override
    public int compareTo(MCData other) {
        return (this.visits - other.visits);
    }
}
