package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/*
A wrapper class (at the moment) to hold data for each MCTS Decision that is made
 */
public class MCTSDecision {

    public final int iterations, N, nodesCreated, actions, player, turn, n, maxDepth;
    public final double entropy;
    public final List<ActionEnum> allActions;
    public final List<Integer> all_n;
    public final int[] depths;
    public final ActionEnum actionChosen;
    public final String descriptor;


    public MCTSDecision(MonteCarloTree tree, int iterations, ActionEnum actionChosen, String deciderName, int nodesCreated, long time) {
        MCStatistics<?> rootNode = tree.getRootStatistics();
        N = rootNode.getVisits();
        this.iterations = iterations;
        this.nodesCreated = nodesCreated;
        descriptor = deciderName;
        this.actionChosen = actionChosen;
        player = rootNode.getActorRef();
        turn = (int) time;
        actions = rootNode.getPossibleActions(player).size();
        n = rootNode.getVisits(new ActionWithRef(actionChosen, player));
        depths = tree.getDepths();
        maxDepth = maxDepth(depths);
        allActions = rootNode.getPossibleActions(player).stream()
                .map(a -> new ActionWithRef(a, player))
                .sorted(Comparator.comparingInt(awr -> -rootNode.getVisits(awr)))
                .map(a -> a.actionTaken)
                .collect(Collectors.toList());

        all_n = allActions.stream().map(rootNode::getVisits).collect(Collectors.toList());
        entropy = all_n.stream().mapToDouble(n -> n / (double) N).map(p -> p < 10e-5 ? 0.0 : -p * Math.log(p)).sum();
    }

    private int maxDepth(int[] depths) {
        for (int i = 0; i < 11; i++) {
            if (depths[i] == 0) return i - 1;
        }
        return 10;
    }
}
