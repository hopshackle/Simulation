package hopshackle.simulation;

import java.util.*;

/* S indicates the State representation to use for start and end states
 * 
 */
public class ExperienceRecord<A extends Agent, S extends State<A>> implements Persistent {
	
	public enum ERState {
		UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
	}

	private static boolean dbStorage = SimProperties.getProperty("ExperienceRecordDBStorage", "false").equals("true");
	private static DatabaseWriter<ExperienceRecord<?, ?>> writer = new DatabaseWriter<ExperienceRecord<?, ?>>(new ExpRecDAO());
	private static double lambda, gamma, traceCap;
	static {refreshProperties();}
	protected S startState, endState;
	protected double[] startStateAsArray, endStateAsArray;
	protected double[] featureTrace;
	protected Action<A> actionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double startScore, endScore, reward;
	protected List<GeneticVariable<A, S>> variables;
	protected boolean isFinalState;
	protected ERState expRecState = ERState.UNSEEN;
	private Agent agent;

	public static void refreshProperties() {
		lambda = SimProperties.getPropertyAsDouble("QTraceLambda", "0.0");
		gamma = SimProperties.getPropertyAsDouble("Gamma", "1.0");
		traceCap = SimProperties.getPropertyAsDouble("QTraceMaximum", "10.0");
	}

	public ExperienceRecord(Agent a, List<GeneticVariable<A, S>> var, S state, Action<A> action, 
			List<ActionEnum<A>> possibleActions) {
		variables = HopshackleUtilities.cloneList(var);
		actionTaken = action;
		startState = state;
		startStateAsArray = extractFeaturesFromState(state);
		featureTrace = startStateAsArray;
		possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
		setState(ERState.DECISION_TAKEN);
		startScore = a.getScore();
		agent = a;
	}

	private double[] extractFeaturesFromState(S state) {
		double[] retValue = new double[variables.size()];		// start Values, then end values
		int count = 0;
		for (GeneticVariable<A, S> gv : variables) {
				retValue[count] = gv.getValue(state);
			count++;
		}
		return retValue;
	}

	private void constructFeatureTrace(ExperienceRecord<A, S> previousER) {
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
	
	public void updateWithResults(double reward, S newState) {
		endState = newState;
		endStateAsArray = extractFeaturesFromState(endState);
		this.reward = reward;
		setState(ERState.ACTION_COMPLETED);
	}
	
	public void updateNextActions(ExperienceRecord<A, S> nextER) {
		if (nextER != null) {
			nextER.constructFeatureTrace(this);
			possibleActionsFromEndState = nextER.getPossibleActionsFromStartState();
			endState = nextER.getStartState();
			endStateAsArray = extractFeaturesFromState(endState);
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

	public S getStartState() {
		return startState;
	}
	public double[] getStartStateAsArray() {
		return startStateAsArray;
	}

	public S getEndState() {
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

	public List<GeneticVariable<A, S>> getVariables() {
		return variables;
	}
	
	public boolean isInFinalState() {
		return isFinalState;
	}
	
	public ERState getState() {
		return expRecState;
	}
	public Agent getAgent() {
		return agent;
	}
	public World getWorld() {
		return agent.getWorld();
	}
	private void setState(ExperienceRecord.ERState newState) {
		if (dbStorage && newState == ERState.NEXT_ACTION_TAKEN && getState() != ERState.NEXT_ACTION_TAKEN) {
			writer.write(this, getWorld().toString());
		}
		expRecState = newState;
	}
}
