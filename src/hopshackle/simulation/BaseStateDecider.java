package hopshackle.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by james on 17/05/2017.
 */
public abstract class BaseStateDecider<A extends Agent> extends BaseDecider<A> {
    public BaseStateDecider(StateFactory<A> stateFactory) {
        super(stateFactory);
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<A>> optionList, A decidingAgent){
        return valueOptions(optionList, getCurrentState(decidingAgent));
    }

    @Override
    public double valueOption(ActionEnum<A> option, A decidingAgent) {
        return valueOption(option, getCurrentState(decidingAgent));
    }


    protected <S extends State<A>> double valueOfBestAction(ExperienceRecord<A> exp) {
        if (exp.isInFinalState() || monteCarlo)
            return 0.0;
        ActionEnum<A> bestAction = getBestActionFrom(exp.getPossibleActionsFromEndState(), exp.getEndState());
        return valueOption(bestAction, exp.getEndState());
    }

    protected <S extends State<A>> ActionEnum<A> getBestActionFrom(List<ActionEnum<A>> possActions, S state) {
        if (state == null) return null;
        double bestValue = -Double.MAX_VALUE;
        ActionEnum<A> bestAction = null;
        List<Double> valueOfOptions = valueOptions(possActions, state);
        for (int i = 0; i < valueOfOptions.size(); i++) {
            double value = valueOfOptions.get(i);
            if (value > bestValue) {
                bestValue = value;
                bestAction = possActions.get(i);
            }
        }
        return bestAction;
    }
}
