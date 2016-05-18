package hopshackle.simulation;

import java.util.*;

public class ExperienceRecord<A extends Agent> {

	protected double[] startState, endState;
	protected ActionEnum<A> actionTaken;
	protected List<ActionEnum<A>> possibleActionsFromEndState, possibleActionsFromStartState;
	protected double reward;
	protected List<GeneticVariable> variables;
	protected boolean isFinalState;
	
	public ExperienceRecord(List<GeneticVariable> var, double[] state, ActionEnum<A> action, List<ActionEnum<A>> possibleActions) {
		actionTaken = action;
		startState = state;
		variables = HopshackleUtilities.cloneList(var);
		possibleActionsFromStartState = HopshackleUtilities.cloneList(possibleActions);
	}

	public void updateWithResults(double reward, double[] newState, List<ActionEnum<A>> actions, boolean endOfRun) {
		endState = newState;
		possibleActionsFromEndState = HopshackleUtilities.cloneList(actions);
		this.reward = reward;
		isFinalState = endOfRun;
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

	public ActionEnum<A> getActionTaken() {
		return actionTaken;
	}

	public List<ActionEnum<A>> getPossibleActionsFromEndState() {
		return possibleActionsFromEndState;
	}
	
	public List<ActionEnum<A>> getPossibleActionsFromStartState() {
		return possibleActionsFromStartState;
	}

	public double getReward() {
		return reward;
	}

	public List<GeneticVariable> getVariables() {
		return variables;
	}
	
	public boolean isInFinalState() {
		return isFinalState;
	}
}
