package hopshackle.simulation;

import java.util.*;

/* S indicates the State representation to use for start and end states
 * 
 */
public class ExperienceRecord<A extends Agent> implements Persistent {
	
	public enum ERState {
		UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
	}

	private static boolean incrementalScoreAffectsReward, dbStorage, monteCarlo;
	private static DatabaseWriter<ExperienceRecord<?>> writer = new DatabaseWriter<ExperienceRecord<?>>(new ExpRecDAO());
	protected static double lambda, gamma, traceCap, timePeriod;
	static {refreshProperties();}
	protected State<A> startState, endState;
	protected double[] startStateAsArray, endStateAsArray;
	protected double[] featureTrace;
	protected Action<A> actionTaken, nextActionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double startScore, endScore, reward;
	protected boolean isFinalState;
	protected ERState expRecState = ERState.UNSEEN;
	private A agent;
	private ExperienceRecord<A> previousRecord;
	protected long timeOfDecision, timeOfResolution;

	public static void refreshProperties() {
		lambda = SimProperties.getPropertyAsDouble("QTraceLambda", "0.0");
		gamma = SimProperties.getPropertyAsDouble("Gamma", "1.0");
		traceCap = SimProperties.getPropertyAsDouble("QTraceMaximum", "10.0");
		incrementalScoreAffectsReward = SimProperties.getProperty("IncrementalScoreReward", "true").equals("true");
		dbStorage = SimProperties.getProperty("ExperienceRecordDBStorage", "false").equals("true");
		monteCarlo = SimProperties.getProperty("MonteCarloReward", "false").equals("true");
		timePeriod = SimProperties.getPropertyAsDouble("TimePeriodForGamma", "1000");
	}

	public ExperienceRecord(A a, State<A> state, Action<A> action, List<ActionEnum<A>> possibleActions) {
		actionTaken = action;
		startState = state;
		startStateAsArray = state.getAsArray();
		featureTrace = startStateAsArray;
		possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
		setState(ERState.DECISION_TAKEN);
		startScore = a.getScore();
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
				featureTrace[i] = gamma * lambda * previousER.featureTrace[i] + startStateAsArray[i];
				if (featureTrace[i] > traceCap)	featureTrace[i] = traceCap;
			}
		}
	}
	
	public void updateWithResults(double reward, State<A> newState) {
		endState = newState;
		endStateAsArray = endState.getAsArray();
		this.reward = reward;
		timeOfResolution = getAgent().getWorld().getCurrentTime();
		setState(ERState.ACTION_COMPLETED);
	}
	
	public void updateNextActions(ExperienceRecord<A> nextER) {
		if (nextER != null) {
			nextER.previousRecord = this;
			nextER.constructFeatureTrace(this);
			possibleActionsFromEndState = nextER.getPossibleActionsFromStartState();
			nextActionTaken = nextER.actionTaken;
			endState = nextER.getStartState();
			endStateAsArray = endState.getAsArray();
			endScore = nextER.getStartScore();
		} else {
			possibleActionsFromEndState = new ArrayList<ActionEnum<A>>();
			endScore = agent.getScore();
		}
		timeOfResolution = getAgent().getWorld().getCurrentTime();
		setState(ERState.NEXT_ACTION_TAKEN);
	}
	
	public void setIsFinal() {
		isFinalState = true;
		endScore = agent.getScore();
		timeOfResolution = getAgent().getWorld().getCurrentTime();
		setState(ERState.NEXT_ACTION_TAKEN);
		if (monteCarlo) {
			updatePreviousRecord(getReward());
		}
	}
	
	private void updatePreviousRecord(double score) {
		if (previousRecord != null) {
			double newScore = score * gamma + previousRecord.getReward();
			previousRecord.reward = newScore;
			previousRecord.endScore = 0.0;
			previousRecord.startScore = 0.0;
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

	public double getReward() {
		if (getState() == ERState.NEXT_ACTION_TAKEN && incrementalScoreAffectsReward) 
			return (reward + endScore - startScore);
		if (isInFinalState() && !incrementalScoreAffectsReward) {
			return reward + endScore;
		}
		return reward;
	}
	
	public double getStartScore() {
		return startScore;
	}
	public double getEndScore() {
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
}
