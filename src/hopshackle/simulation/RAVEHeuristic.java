package hopshackle.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by james on 04/07/2017.
 */
public class RAVEHeuristic<A extends Agent> extends BaseStateDecider<A> {

    private MonteCarloTree<A> tree;
    private double C;

    public RAVEHeuristic(MonteCarloTree<A> mcTree, double exploreC) {
        super(null);
        tree = mcTree;
        this.C = exploreC;
    }

    @Override
    public double valueOption(ActionEnum<A> option, State<A> state) {
        MCStatistics<A> stats = tree.getStatisticsFor(state);
        return stats.getRAVEValue(option, C);
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state) {
        MCStatistics<A> stats = tree.getStatisticsFor(state);
        List<Double> retValue = new ArrayList<Double>(options.size());
        for (ActionEnum<A> option : options)
            retValue.add(stats.getRAVEValue(option, C));
        return retValue;
    }

    @Override
    public void learnFrom(ExperienceRecord<A> exp, double maxResult) {}
}
