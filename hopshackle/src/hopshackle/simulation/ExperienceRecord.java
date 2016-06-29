package hopshackle.simulation;

import java.util.*;

public class ExperienceRecord<A extends Agent> implements Persistent {
	
	public enum State {
		UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
	}

	private static boolean dbStorage = SimProperties.getProperty("ExperienceRecordDBStorage", "false").equals("true");
	private static DatabaseWriter<ExperienceRecord<?>> writer = new DatabaseWriter<ExperienceRecord<?>>(new ExpRecDAO());
	private static double lambda, gamma, traceCap;
	static {refreshProperties();}
	protected double[] startState, endState;
	protected double[] featureTrace;
	protected Action<A> actionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double startScore, endScore, reward;
	protected List<GeneticVariable> variables;
	protected boolean isFinalState;
	protected State ERState = State.UNSEEN;
	private Agent agent;

	public static void refreshProperties() {
		lambda = SimProperties.getPropertyAsDouble("QTraceLambda", "0.0");
		gamma = SimProperties.getPropertyAsDouble("Gamma", "1.0");
		traceCap = SimProperties.getPropertyAsDouble("QTraceMaximum", "10.0");
	}

	public ExperienceRecord(Agent a, List<GeneticVariable> var, double[] state, Action<A> action, 
			List<ActionEnum<A>> possibleActions) {
		actionTaken = action;
		startState = state;
		featureTrace = state;
		variables = HopshackleUtilities.cloneList(var);
		possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
		setState(State.DECISION_TAKEN);
		startScore = a.getScore();
		agent = a;
	}

	private void constructFeatureTrace(ExperienceRecord<A> previousER) {
		if (previousER.featureTrace.length != startState.length) {
			featureTrace = startState;
		} else {
			featureTrace = new double[startState.length];
			for (int i = 0; i < startState.length; i++) {
				featureTrace[i] = gamma * lambda * previousER.featureTrace[i] + startState[i];
				if (featureTrace[i] > traceCap)	featureTrace[i] = traceCap;
			}
		}
	}
	
	public void updateWithResults(double reward, double[] newState) {
		endState = newState;
		this.reward = reward;
		setState(State.ACTION_COMPLETED);
	}
	
	public void updateNextActions(ExperienceRecord<A> nextER) {
		if (nextER != null) {
			nextER.constructFeatureTrace(this);
			possibleActionsFromEndState = nextER.getPossibleActionsFromStartState();
			endScore = nextER.getStartScore();
		} else {
			possibleActionsFromEndState = new ArrayList<ActionEnum<A>>();
			endScore = 0.0;
		}
		setState(State.NEXT_ACTION_TAKEN);
	}
	
	public void setIsFinal() {
		isFinalState = true;
		endScore = 0.0;
		setState(State.NEXT_ACTION_TAKEN);
	}
	
	public double[][] getValues(List<GeneticVariable> gvList) {
		double[][] retValue = new double[2][gvList.size()];		// start Values, then end values
		int count = 0;
		for (GeneticVariable gv : gvList) {
			int index = variables.indexOf(gv);
			if (index > -1) {
				retValue[0][count] = startState[index];
				retValue[1][count] = endState[index];
			} else {
				retValue[0][count] = 0.0;
				retValue[1][count] = 0.0;
			}
			count++;
		}
		return retValue;
	}

	public double[] getStartState() {
		return startState;
	}

	public double[] getEndState() {
		return endState;
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
		if (getState() == State.NEXT_ACTION_TAKEN) 
			return (reward + endScore - startScore);
		return reward;
	}
	public double getStartScore() {
		return startScore;
	}
	public double getEndScore() {
		return endScore;
	}

	public List<GeneticVariable> getVariables() {
		return variables;
	}
	
	public boolean isInFinalState() {
		return isFinalState;
	}
	
	public State getState() {
		return ERState;
	}
	public Agent getAgent() {
		return agent;
	}
	public World getWorld() {
		return agent.getWorld();
	}
	private void setState(ExperienceRecord.State newState) {
		if (dbStorage && newState == State.NEXT_ACTION_TAKEN && getState() != State.NEXT_ACTION_TAKEN) {
			writer.write(this, getWorld().toString());
		}
		ERState = newState;
	}
}
