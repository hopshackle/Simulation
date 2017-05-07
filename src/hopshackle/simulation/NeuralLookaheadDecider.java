package hopshackle.simulation;

import java.util.*;

import org.encog.neural.data.basic.BasicNeuralData;

public class NeuralLookaheadDecider<A extends Agent> extends LookaheadDecider<A> {

    protected NeuralDecider<A> internalNeuralDecider;
    private boolean useLookahead;

    public NeuralLookaheadDecider(StateFactory<A> stateFactory, double scaleFactor) {
        super(stateFactory);
        internalNeuralDecider = new NeuralStateValuer<A>(stateFactory, scaleFactor);
    }

    @Override
    public void injectProperties(DeciderProperties dp) {
        super.injectProperties(dp);
        useLookahead = getProperty("LookaheadQLearning", "false").equals("true");
        internalNeuralDecider.injectProperties(dp);
    }

    @Override
    public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
        if (localDebug) logResult(exp);
        internalNeuralDecider.learnFrom(exp, maxResult);
    }

    @Override
    public void learnFromBatch(List<ExperienceRecord<A>> allExperience, double maxResult) {
        if (localDebug) {
            allExperience.get(0).getAgent().log("Now training decider " + this.toString());
            for (ExperienceRecord<A> er : allExperience)
                logResult(er);
        }
        internalNeuralDecider.learnFromBatch(allExperience, maxResult);
    }

    private void logResult(ExperienceRecord<A> baseER) {
        double startValue = value(baseER.getStartState(useLookahead)); // projection
        double endValue = value(baseER.getEndState()); // projection
        String message = String.format("Learning:\t%-20sScore: %.2f -> %.2f, State Valuation: %.2f -> %.2f, Target: %.2f, EndGame: %s",
                baseER.getActionTaken(), baseER.getStartScore()[baseER.getAgentNumber()], baseER.getEndScore()[baseER.getAgentNumber()],
                startValue, endValue, internalNeuralDecider.getTarget(baseER)[0], baseER.isInFinalState());
        log(message);
        baseER.getAgent().log(message);
        double[] startArray = baseER.getStartStateAsArray(useLookahead);
        double[] endArray = baseER.getEndStateAsArray();
        double[] featureTrace = baseER.getFeatureTrace();
        StringBuffer logMessage = new StringBuffer("StartState -> EndState (FeatureTrace) :" + newline);
        for (int i = 0; i < startArray.length; i++) {
            if (startArray[i] != 0.0 || endArray[i] != 0.0 || Math.abs(featureTrace[i]) >= 0.01)
                logMessage.append(String.format("\t%.2f -> %.2f (%.2f) %s %s", startArray[i], endArray[i], featureTrace[i], stateFactory.getVariables().get(i).toString(), newline));
        }
        message = logMessage.toString();
        log(message);
        baseER.getAgent().log(message);
    }

    @Override
    public double value(State<A> state) {
        BasicNeuralData inputData = new BasicNeuralData(state.getAsArray());
        return internalNeuralDecider.brain.compute(inputData).getData(0) * internalNeuralDecider.scaleFactor;
    }

    public void setInternalNeuralNetwork(NeuralDecider<A> newND) {
        if (internalNeuralDecider.isBrainCompatible(newND.brain)) {
            internalNeuralDecider = newND;
        } else {
            throw new AssertionError("New Network is not compatible with the old one.");
        }
    }

    public void saveToFile(String descriptor, String directory) {
        internalNeuralDecider.saveBrain(descriptor, directory);
    }

    @Override
    public void setName(String newName) {
        super.setName(newName);
        internalNeuralDecider.setName(newName + "_ND");
    }
}



