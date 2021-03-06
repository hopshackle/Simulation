package hopshackle.simulation;

import java.util.*;

/* S indicates the State representation to use for start and end states
 *
 */
public class ExperienceRecord<A extends Agent> {

    public enum ERState {
        UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
    }

    private DeciderProperties properties;
    private boolean incrementalScoreAffectsReward, dbStorage;
    private static DatabaseWriter<ExperienceRecord<?>> writer;
    protected double lambda, gamma, traceCap, timePeriod;
    private State<A> startState, startStateWithLookahead, endState;
    protected double[] startStateAsArray, startStateWithLookaheadAsArray, endStateAsArray;
    protected double[] featureTrace;
    protected Action<A> actionTaken;
    protected List<ActionEnum<A>> possibleActionsFromStartState;
    protected double[] startScore, reward, monteCarloReward;
    protected boolean isFinalState;
    protected ERState expRecState = ERState.UNSEEN;
    private A actingAgent;
    private int actingAgentNumber;
    private ExperienceRecord<A> previousRecord, nextRecord;
    protected long timeOfDecision, timeOfResolution;

    public void refreshProperties() {
        lambda = properties.getPropertyAsDouble("QTraceLambda", "0.0");
        gamma = properties.getPropertyAsDouble("Gamma", "1.0");
        traceCap = properties.getPropertyAsDouble("QTraceMaximum", "10.0");
        incrementalScoreAffectsReward = properties.getProperty("IncrementalScoreReward", "true").equals("true");
        dbStorage = properties.getProperty("ExperienceRecordDBStorage", "false").equals("true");
        timePeriod = properties.getPropertyAsDouble("TimePeriodForGamma", "1000");
    }

    public ExperienceRecord(A a, State<A> state, Action<A> action, List<ActionEnum<A>> possibleActions, DeciderProperties properties) {
        this.properties = properties;
        refreshProperties();
        actionTaken = action;
        startStateWithLookahead = state.apply(action.getType());
        startState = state;
        startStateAsArray = startState.getAsArray();
        startStateWithLookaheadAsArray = startStateWithLookahead.getAsArray();

        featureTrace = startStateAsArray;
        possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
        setState(ERState.DECISION_TAKEN);
        startScore = state.getScore();
        reward = new double[startScore.length];
        monteCarloReward = null;
        actingAgent = a;
        if (a.getGame() != null)
            actingAgentNumber = a.getActorRef();
        timeOfDecision = a.getWorld().getCurrentTime();
        timeOfResolution = -1;
    }

    private void constructFeatureTrace(ExperienceRecord<A> previousER) {
        if (previousER.featureTrace.length != startStateAsArray.length || lambda == 0.0) {
            featureTrace = startStateAsArray;
        } else {
            featureTrace = new double[startStateAsArray.length];
            for (int i = 0; i < startStateAsArray.length; i++) {
                // We discount feature trace over time that has passed from the start of the last feature trace snapshot to now
                featureTrace[i] = Math.pow(gamma * lambda, previousER.getDiscountPeriod()) * previousER.featureTrace[i] + startStateAsArray[i];
                if (featureTrace[i] > traceCap) featureTrace[i] = traceCap;
            }
        }
    }

    public void updateWithResults(double reward) {
        this.updateWithResults(toArray(reward));
    }

    public void updateWithResults(double[] reward) {
//		endState = newState;
//		endStateAsArray = endState.getAsArray();
        this.reward = reward;
        // we will take account of change in score only when the nextER is added (or the state is marked as final)
        timeOfResolution = getAgent().getWorld().getCurrentTime();
        //		getAgent().log("ACTION_COMPLETED for " + this.actionTaken);
        setState(ERState.ACTION_COMPLETED);
    }

    public void updateNextActions(ExperienceRecord<A> nextER) {
        nextRecord = nextER;
        timeOfResolution = getAgent().getWorld().getCurrentTime();
        if (nextER != null) {
            nextER.previousRecord = this;
            if (nextER.actingAgent == this.actingAgent) {
                nextER.constructFeatureTrace(this);
//				endState = nextER.getStartState();
//				endStateAsArray = nextER.getStartStateAsArray();
            } else {
//				Decider<A> d = actingAgent.getDecider();
//				endState = d.getCurrentState(actingAgent);
//				endStateAsArray = endState.getAsArray();
            }
            if (incrementalScoreAffectsReward) {
                for (int i = 0; i < reward.length; i++)
                    reward[i] = reward[i] + getEndScore()[i] - startScore[i];
            }
            //			getAgent().log("Updated ER for Action " + actionTaken + " after new action " + nextER.getActionTaken());
        } else {
            double[] finalScores = new double[startScore.length];
            for (int i = 0; i < startScore.length; i++)
                finalScores[i] = startScore[i] + reward[i]; // Not ideal, but we have no information to go on
            updateWithFinalScores(finalScores);
        }
        setState(ERState.NEXT_ACTION_TAKEN);
    }

    public void updateWithFinalScores(double[] finalScores) {
        if (finalScores.length > reward.length) expandRewardTo(finalScores.length);
        timeOfResolution = getAgent().getWorld().getCurrentTime();
        Decider<A> d = actingAgent.getDecider();
        endState = d.getCurrentState(actingAgent);
        endStateAsArray = endState.getAsArray();
        monteCarloReward = new double[reward.length];
        for (int i = 0; i < finalScores.length; i++) {
            reward[i] = reward[i] + finalScores[i];
            if (incrementalScoreAffectsReward) reward[i] = reward[i] - startScore[i];
            monteCarloReward[i] = reward[i];
            //		System.out.println("Final: " + finalScores[i] + " - " + startScore[i] + " = " + this.reward[i]);

        }
        isFinalState = true;
        nextRecord = null;
        timeOfResolution = getAgent().getWorld().getCurrentTime();
        setState(ERState.NEXT_ACTION_TAKEN);
        updatePreviousMonteCarloReward(monteCarloReward);
    }

    private void updatePreviousMonteCarloReward(double[] score) {
        if (previousRecord != null) {
            double[] prevReward = previousRecord.getReward();
            double[] newScore = new double[Math.max(score.length, prevReward.length)];
            double discountFactor = gamma;
            // we discount over the elapsed time of this ER (i.e. from end, back to start..which is the end of the previous record)
            if (gamma < 1.0) discountFactor = Math.pow(gamma, getDiscountPeriod());
            for (int i = 0; i < newScore.length; i++) {
                double oldScore = i >= score.length ? 0.0 : score[i];
                double prevR = i >= prevReward.length ? 0.0 : prevReward[i];
                newScore[i] = oldScore * discountFactor + prevR;
            }
            previousRecord.monteCarloReward = newScore;
            //           previousRecord.startScore = new double[score.length];
            previousRecord.updatePreviousMonteCarloReward(newScore);
        } else {
            // end of the chain
        }
    }

    public State<A> getStartState(boolean lookahead) {
        if (lookahead) return startStateWithLookahead;
        return startState;
    }

    public double[] getStartStateAsArray(boolean lookahead) {
        if (lookahead) return startStateWithLookaheadAsArray;
        return startStateAsArray;
    }

    public State<A> getEndState() {
        if (nextRecord != null) return nextRecord.startState;
        return endState;
    }

    public double[] getEndStateAsArray() {
        if (nextRecord != null) return nextRecord.startStateAsArray;
        return endStateAsArray;
    }

    public double[] getFeatureTrace() {
        return featureTrace;
    }

    public Action<A> getActionTaken() {
        return actionTaken;
    }

    public ActionEnum<A> getActionTakenFromEndState() {
        if (nextRecord != null) return nextRecord.getActionTaken().getType();
        return null;
    }

    public List<ActionEnum<A>> getPossibleActionsFromEndState() {
        if (nextRecord != null) return nextRecord.getPossibleActionsFromStartState();
        return new ArrayList<ActionEnum<A>>();
    }

    public A nextAgentToAct() {
        if (nextRecord != null) return nextRecord.actingAgent;
        return null;
    }

    public List<ActionEnum<A>> getPossibleActionsFromStartState() {
        return possibleActionsFromStartState;
    }

    public double[] getReward() {
        return reward;
    }

    public double[] getMonteCarloReward() {
        if (monteCarloReward == null) return reward;
        return monteCarloReward;
    }

    public double[] getStartScore() {
        return startScore;
    }

    public double[] getEndScore() {
        if (nextRecord != null) return nextRecord.getStartScore();
        return new double[startScore.length];
    }

    public boolean isInFinalState() {
        return isFinalState;
    }

    public ERState getState() {
        return expRecState;
    }

    public A getAgent() {
        return actingAgent;
    }

    public int getAgentNumber() {
        return actingAgentNumber;
    }

    protected void setState(ExperienceRecord.ERState newState) {
        if (dbStorage && writer != null && newState == ERState.NEXT_ACTION_TAKEN && getState() != ERState.NEXT_ACTION_TAKEN) {
            writer.write(this, actingAgent.getWorld().toString());
        }
        if (dbStorage && writer == null) {
            throw new AssertionError("DatabaseWriter has not been initialised for ExperienceRecords");
        }
        expRecState = newState;
    }

    public static void setDBU(DatabaseAccessUtility dbu) {
        if (writer != null) {
            writer.writeBuffer();
        }
        writer = new DatabaseWriter<>(new ExpRecDAO(), dbu);
    }

    public double getDiscountPeriod() {
        if (timeOfResolution > -1)
            return (timeOfResolution - timeOfDecision) / timePeriod;
        else
            return 0.0;
    }

    private double[] toArray(double single) {
        double[] retValue = new double[1];
        retValue[0] = single;
        return retValue;
    }

    public ExperienceRecord<A> getPreviousRecord() {
        return previousRecord;
    }

    private void expandRewardTo(int newLength) {
        double[] oldR = reward;
        reward = new double[newLength];
        for (int i = 0; i < oldR.length; i++) {
            reward[i] = oldR[i];
        }
    }
}
