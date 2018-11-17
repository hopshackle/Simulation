package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;

import java.util.*;

/**
 * Created by james on 04/07/2017.
 */
public class MASTHeuristic<A extends Agent> extends BaseStateDecider<A> {

    private MonteCarloTree<A> tree;

    public MASTHeuristic(MonteCarloTree<A> mcTree) {
        super(null);
        tree = mcTree;
    }

    @Override
    public double valueOption(ActionEnum<A> option, State<A> state, int decidingAgent) {
        return tree.getActionValue(option.toString(), decidingAgent);
    }

    @Override
    public double valueOption(ActionEnum<A> option, State<A> state) {
        return valueOption(option, state, state.getActorRef());
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state, int decidingAgent) {
        List<Double> retValue = new ArrayList<Double>(options.size());
        for (ActionEnum<A> option : options)
            retValue.add(tree.getActionValue(option.toString(), decidingAgent));
        return retValue;
    }

    @Override
    public void learnFrom(ExperienceRecord<A> exp, double maxResult) {}
}
