package hopshackle.simulation;

import java.util.*;

/**
 * Created by james on 17/05/2017.
 */
public abstract class BaseAgentDecider<A extends Agent> extends BaseDecider<A> {

    public BaseAgentDecider(StateFactory<A> stateFactory) {
        super(stateFactory);
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<A>> optionList, A decidingAgent){
        List<Double> retValue = new ArrayList<Double>(optionList.size());
        for (int i = 0; i < optionList.size(); i++) retValue.add(0.0);
        for (int i = 0; i < optionList.size(); i++) {
            double optionValue = this.valueOption(optionList.get(i), decidingAgent);
            retValue.set(i, optionValue);
        }
        return retValue;
    }

    @Override
    public double valueOption(ActionEnum<A> option, State<A> state){
        throw new AssertionError("State evaluation not supported");
    }

}
