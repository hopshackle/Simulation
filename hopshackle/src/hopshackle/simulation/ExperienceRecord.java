package hopshackle.simulation;

import java.util.*;

/* S indicates the State representation to use for start and end states
 * 
 */
public class ExperienceRecord<A extends Agent> implements Persistent {
	
	public enum ERState {
		UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
	}

	private static boolean dbStorage = SimProperties.getProperty("ExperienceRecordDBStorage", "false").equals("true");
	private static DatabaseWriter<ExperienceRecord<?>> writer = new DatabaseWriter<ExperienceRecord<?>>(new ExpRecDAO());
	private static double lambda, gamma, traceCap;
	static {refreshProperties();}
	protected State<A> startState, endState;
	protected double[] startStateAsArray, endStateAsArray;
	protected double[] featureTrace;
	protected Action<A> actionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double startScore, endScore, reward;
	protected boolean isFinalState;
	protected ERState expRecState = ERState.UNSEEN;
	private A agent;

	public static void refreshProperties() {
		lambda = SimProperties.getPropertyAsDouble("QTraceLambda", "0.0");
		gamma = SimProperties.getPropertyAsDouble("Gamma", "1.0");
		traceCap = SimProperties.getPropertyAsDouble("QTraceMaximum", "10.0");
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
		setState(ERState.ACTION_COMPLETED);
	}
	
	public void updateNextActions(ExperienceRecord<A> nextER) {
		if (nextER != null) {
			nextER.constructFeatureTrace(this);
			possibleActionsFromEndState = nextER.getPossibleActionsFromStartState();
			endState = nextER.getStartState();
			endStateAsArray = endState.getAsArray();
			endScore = nextER.getStartScore();
		} else {
			possibleActionsFromEndState = new ArrayList<ActionEnum<A>>();
			endScore = agent.getScore();
		}
		setState(ERState.NEXT_ACTION_TAKEN);
	}
	
	public void setIsFinal() {
		isFinalState = true;
		endScore = agent.getScore();
		setState(ERState.NEXT_ACTION_TAKEN);
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
		if (getState() == ERState.NEXT_ACTION_TAKEN) 
			return (reward + endScore - startScore);
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
}
