package hopshackle.simulation;

import java.util.*;

/* S indicates the State representation to use for start and end states
 * 
 */
public class ExperienceRecord<A extends Agent> implements Persistent {

	public enum ERState {
		UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
	}

	private DeciderProperties properties;
	private boolean incrementalScoreAffectsReward, dbStorage, monteCarlo, lookaheadQLearning;
	private static DatabaseWriter<ExperienceRecord<?>> writer = new DatabaseWriter<ExperienceRecord<?>>(new ExpRecDAO());
	protected double lambda, gamma, traceCap, timePeriod;
	protected State<A> startState, endState;
	protected double[] startStateAsArray, endStateAsArray;
	protected double[] featureTrace;
	protected Action<A> actionTaken, nextActionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double[] startScore, endScore, reward;
	protected boolean isFinalState;
	protected ERState expRecState = ERState.UNSEEN;
	private A agent;
	private ExperienceRecord<A> previousRecord;
	protected long timeOfDecision, timeOfResolution;

	public void refreshProperties() {
		lambda = properties.getPropertyAsDouble("QTraceLambda", "0.0");
		gamma = properties.getPropertyAsDouble("Gamma", "1.0");
		traceCap = properties.getPropertyAsDouble("QTraceMaximum", "10.0");
		incrementalScoreAffectsReward = properties.getProperty("IncrementalScoreReward", "true").equals("true");
		dbStorage = properties.getProperty("ExperienceRecordDBStorage", "false").equals("true");
		monteCarlo = properties.getProperty("MonteCarloReward", "false").equals("true");
		timePeriod = properties.getPropertyAsDouble("TimePeriodForGamma", "1000");
	}

	public ExperienceRecord(A a, State<A> state, Action<A> action, List<ActionEnum<A>> possibleActions, DeciderProperties properties) {
		this.properties = properties;
		refreshProperties();
		actionTaken = action;
		if (lookaheadQLearning) {
			startState = state.apply(action.getType());	
		} else {
			startState = state;
		}
		startStateAsArray = state.getAsArray();
		featureTrace = startStateAsArray;
		possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
		setState(ERState.DECISION_TAKEN);
		startScore = state.getScore();
		agent = a;
		timeOfDecision = a.getWorld().getCurrentTime();
		timeOfResolution = -1;
	}

	private void constructFeatureTrace(ExperienceRecord<A> previousER) {
		if (previousER.featureTrace.length != startStateAsArray.length) {
			featureTrace = startStateAsArray;
		} else {
			featureTrace = new double[startStateAsArray.length];
			for (int i = 0; i < startStateAsArray.length; i++) {
				// We discount feature trace over time that has passed
				featureTrace[i] = Math.pow(gamma * lambda, previousER.getDiscountPeriod()) * previousER.featureTrace[i] + startStateAsArray[i];
				if (featureTrace[i] > traceCap)	featureTrace[i] = traceCap;
			}
		}
	}

	public void updateWithResults(double reward, State<A> newState) {
		this.updateWithResults(toArray(reward), newState);
	}
	
	public void updateWithResults(double[] reward, State<A> newState) {
		endState = newState;
		endStateAsArray = endState.getAsArray();
		this.reward = reward;
		timeOfResolution = getAgent().getWorld().getCurrentTime();
		//		getAgent().log("ACTION_COMPLETED for " + this.actionTaken);
		setState(ERState.ACTION_COMPLETED);
	}

	public void updateNextActions(ExperienceRecord<A> nextER) {
		timeOfResolution = getAgent().getWorld().getCurrentTime();
		if (nextER != null) {
			nextER.previousRecord = this;
			nextER.constructFeatureTrace(this);
			possibleActionsFromEndState = nextER.getPossibleActionsFromStartState();
			nextActionTaken = nextER.actionTaken;
			endState = nextER.getStartState();
			endStateAsArray = endState.getAsArray();
			endScore = nextER.getStartScore();
			//			getAgent().log("Updated ER for Action " + actionTaken + " after new action " + nextER.getActionTaken());
		} else {
			possibleActionsFromEndState = new ArrayList<ActionEnum<A>>();
			for (int i = 0; i < startScore.length; i++) endScore[i] = startScore[i] + reward[i]; // Not ideal, but we have no information to go on
			setIsFinal();
		}
		setState(ERState.NEXT_ACTION_TAKEN);
	}
	
	public void updateWithFinalScores(double[] finalScores) {
		timeOfResolution = getAgent().getWorld().getCurrentTime();
		possibleActionsFromEndState = new ArrayList<ActionEnum<A>>();
		endScore = finalScores;
		setIsFinal();
	}


	public void setIsFinal() {
		isFinalState = true;
		timeOfResolution = getAgent().getWorld().getCurrentTime();
		setState(ERState.NEXT_ACTION_TAKEN);
		if (monteCarlo) {
			updatePreviousRecord(getReward());
		}
	}

	private void updatePreviousRecord(double[] score) {
		if (previousRecord != null) {
			double[] prevReward = previousRecord.getReward();
			double[] newScore = score;
			for (int i = 0; i < score.length; i++) newScore[i] = newScore[i] * gamma + prevReward[i];
			previousRecord.reward = newScore;
			previousRecord.endScore = new double[score.length];
			previousRecord.startScore = new double[score.length];
			previousRecord.updatePreviousRecord(newScore);
		} else {
			// end of the chain
		}
	}

	public State<A> getStartState() {
		return startState;
	}
	public double[] getStartStateAsArray() {
		return startStateAsArray;
	}

	public State<A> getEndState() {
		return endState;
	}
	public double[] getEndStateAsArray() {
		return endStateAsArray;
	}

	public double[] getFeatureTrace() {
		return featureTrace;
	}

	public Action<A> getActionTaken() {
		return actionTaken;
	}

	public List<ActionEnum<A>> getPossibleActionsFromEndState() {
		return possibleActionsFromEndState;
	}

	public List<ActionEnum<A>> getPossibleActionsFromStartState() {
		return possibleActionsFromStartState;
	}

	public double[] getReward() {
		double[] retValue = new double[reward.length];
		if (getState() == ERState.NEXT_ACTION_TAKEN && incrementalScoreAffectsReward) {
			for (int i = 0; i < reward.length; i++) retValue[i] = reward[i] + endScore[i] - startScore[i];
		} else if (isInFinalState() && !incrementalScoreAffectsReward) {
			for (int i = 0; i < reward.length; i++) retValue[i] = reward[i] + endScore[i];
		} else {
			retValue = reward;
		}
		return retValue;
	}

	public double[] getStartScore() {
		return startScore;
	}
	public double[] getEndScore() {
		return endScore;
	}

	public boolean isInFinalState() {
		return isFinalState;
	}

	public ERState getState() {
		return expRecState;
	}
	public A getAgent() {
		return agent;
	}
	public World getWorld() {
		return agent.getWorld();
	}
	protected void setState(ExperienceRecord.ERState newState) {
		if (dbStorage && newState == ERState.NEXT_ACTION_TAKEN && getState() != ERState.NEXT_ACTION_TAKEN) {
			writer.write(this, getWorld().toString());
		}
		expRecState = newState;
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
}
