package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.BasicNeuralData;

public class NeuralLookaheadDecider<A extends Agent> extends NeuralDecider<A> {

    private boolean simpleEndStateValuation;

    public NeuralLookaheadDecider(StateFactory<A> stateFactory, double scaleFactor) {
        super(stateFactory, scaleFactor);
    }

    @Override
    public void injectProperties(DeciderProperties dp) {
        super.injectProperties(dp);
        simpleEndStateValuation = dp.getProperty("NeuralLookaheadSimpleEndStateValuation", "false").equals("true");
        brain = BrainFactory.initialiseBrain(stateFactory.getVariables().size(), 1, decProp);
    }

    @Override
    protected void logResult(ExperienceRecord<A> exp, double[] target) {
        double startValue = value(exp.getStartState(useLookahead)); // projection
        double endValue = value(exp.getEndState()); // projection
        String message = String.format("State Value: %.2f -> %.2f", startValue, endValue);
        log(message);
        exp.getAgent().log(message);
        super.logResult(exp, target);
    }

    public double value(State<A> state) {
        BasicNeuralData inputData = new BasicNeuralData(state.getAsArray());
        return brain.compute(inputData).getData(0) * scaleFactor;
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> currentState) {
        List<Double> retValue = new ArrayList<Double>(options.size());
        for (int i = 0; i < options.size(); i++) {
            ActionEnum<A> option = options.get(i);
            double value = valueOption(option, currentState);
            retValue.add(value);
        }
        return retValue;
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<A>> options, A decidingAgent) {
        List<Double> retValue = valueOptions(options, stateFactory.getCurrentState(decidingAgent));
        if (localDebug) {
            for (int i = 0; i < options.size(); i++) {
                ActionEnum<A> option = options.get(i);
                String message = "Option " + option.toString() + " has base Value of " + retValue.get(i); //+
                //	" with state representation of: \n \t" + Arrays.toString(futureState.getAsArray());
                decidingAgent.log(message);
                log(message);
            }
        }
        return retValue;
    }

    @Override
    public double valueOption(ActionEnum<A> option, State<A> state) {
        State<A> futureState = state.apply(option);
        return value(futureState);
    }

    @Override
    protected int getActionIndex(ActionEnum<A> option) {
        return 0;
    }

    /*
    @Override
    protected double[] getTarget(ExperienceRecord<A> exp) {

        double discountPeriod = exp.getDiscountPeriod();
        double target = exp.getMonteCarloReward()[exp.getAgentNumber()] / scaleFactor;
        if (!monteCarlo) {
            target = exp.getReward()[exp.getAgentNumber()] / scaleFactor;
            BasicNeuralData inputDataForEnd = new BasicNeuralData(exp.getEndStateAsArray());
            double endStateValue = 0.0;
            if (!exp.isInFinalState()) {
                double[] prediction = brain.compute(inputDataForEnd).getData();
                endStateValue = prediction[0];
            }
            target = target + Math.pow(gamma, discountPeriod) * endStateValue;
        }
        if (target > 1.0) target = 1.0;
        if (target < -1.0) target = -1.0;

        double[] retValue = new double[1];
        retValue[0] = target;
        return retValue;
    }
*/

    @Override
    protected <S extends State<A>> double valueOfBestAction(ExperienceRecord<A> exp) {
        // TODO: Ultimately I want to get rid of this override. It is purely in place to test the change
        // TODO: to proper On/Off-Policy QLearning from the previous simple end state valuation.
        if (exp.isInFinalState() || monteCarlo)
            return 0.0;
        if (simpleEndStateValuation) return value(exp.getEndState());
        return super.valueOfBestAction(exp);
    }

}



