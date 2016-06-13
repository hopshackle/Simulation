package hopshackle.simulation;

import java.util.*;

public class ExperienceRecord<A extends Agent> {
	
	public enum State {
		UNSEEN, DECISION_TAKEN, ACTION_COMPLETED, NEXT_ACTION_TAKEN;
	}

	protected A actor;
	protected double[] startState, endState;
	protected Action<A> actionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double startScore, endScore, reward;
	protected List<GeneticVariable> variables;
	protected boolean isFinalState;
	protected Decider<A> sourceDecider;
	protected State ERState = State.UNSEEN;
	
	public ExperienceRecord(A actor, List<GeneticVariable> var, double[] state, Action<A> action, List<ActionEnum<A>> possibleActions, Decider<A> decider) {
		this.actor = actor;
		actionTaken = action;
		startState = state;
		variables = HopshackleUtilities.cloneList(var);
		possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
		sourceDecider = decider;
		ERState = State.DECISION_TAKEN;
		startScore = actor.getScore();
	}

	public void updateWithResults(double reward, double[] newState) {
		endState = newState;
		this.reward = reward;
		ERState = State.ACTION_COMPLETED;
	}
	
	public void updateNextActions(List<ActionEnum<A>> actions) {
		possibleActionsFromEndState = HopshackleUtilities.cloneList(actions);
		endScore = actor.getScore();
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

	public List<GeneticVariable> getVariables() {
		return variables;
	}
	
	public boolean isInFinalState() {
		return isFinalState;
	}
	public Decider<A> getDecider() {
		return sourceDecider;
	}
	public State getState() {
		return ERState;
	}
}
