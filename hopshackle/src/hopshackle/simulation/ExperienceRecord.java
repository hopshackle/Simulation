package hopshackle.simulation;

import java.util.*;

public class ExperienceRecord<A extends Agent> {
	
	public enum State {
		UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
	}

	private static double lambda = SimProperties.getPropertyAsDouble("QTraceLambda", "0.0");
	private static double gamma = SimProperties.getPropertyAsDouble("Gamma", "1.0");
	private static double traceCap = SimProperties.getPropertyAsDouble("QTraceMaximum", "10.0");
	protected double[] startState, endState;
	protected double[] featureTrace;
	protected Action<A> actionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double startScore, endScore, reward;
	protected List<GeneticVariable> variables;
	protected boolean isFinalState;
	protected State ERState = State.UNSEEN;
	
	public ExperienceRecord(double score, List<GeneticVariable> var, double[] state, Action<A> action, 
			List<ActionEnum<A>> possibleActions) {
		actionTaken = action;
		startState = state;
		variables = HopshackleUtilities.cloneList(var);
		possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
		ERState = State.DECISION_TAKEN;
		startScore = score;
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
		ERState = State.ACTION_COMPLETED;
	}
	
	public void updateNextActions(List<ActionEnum<A>> actions, double finalScore, ExperienceRecord<A> nextER) {
		if (nextER != null) nextER.constructFeatureTrace(this);
		possibleActionsFromEndState = HopshackleUtilities.cloneList(actions);
		endScore = finalScore;
		ERState = State.NEXT_ACTION_TAKEN;
	}
	
	public void setIsFinal() {
		isFinalState = true;
		endScore = 0.0;
		ERState = State.NEXT_ACTION_TAKEN;
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
}
