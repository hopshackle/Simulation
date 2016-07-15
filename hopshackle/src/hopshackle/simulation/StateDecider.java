package hopshackle.simulation;

import java.util.*;

public abstract class StateDecider<A extends Agent, S extends State<A>> extends QDecider<A, S> {

	private String stateType;
	private int pigeonHoles;
	private double baseValue;
	private double maxNoise, minNoise;

	public StateDecider(ArrayList<ActionEnum<A>> actions, ArrayList<GeneticVariable<A, S>> variables){
		super(actions, variables);
		setStateType("DEFAULT");
		pigeonHoles = 4;
		baseValue = SimProperties.getPropertyAsDouble("StateBaseValue", "10000");
		maxNoise = SimProperties.getPropertyAsDouble("StateMaxNoise", "1.0");
		minNoise = SimProperties.getPropertyAsDouble("StateMinNoise", "0.0");
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		HopshackleState agentState = getState(getCurrentState(decidingAgent));
		return agentState.valueOption(option, temperature * (maxNoise - minNoise) + minNoise );
	}
	
	@Override
	public double valueOption(ActionEnum<A> option, S state) {
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		HopshackleState agentState = getState(state);
		return agentState.valueOption(option, temperature * (maxNoise - minNoise) + minNoise );
	}

	protected HopshackleState getState(S state) {
		StringBuffer retStr = new StringBuffer( stateType );
		double[] stateDesc = HopshackleUtilities.stateToArray(state, variableSet);

		for (int i = 0; i < stateDesc.length; i++)
			retStr.append(":" + pigeonHole(stateDesc[i]));

		HopshackleState retValue =  HopshackleState.getState(retStr.toString());
		if (retValue.getVisited() == 0) 
			retValue.setValue(baseValue);
		// If the state has not been visited before, then we set the
		// default value to be baseValue
		return retValue;
	}
	
	public HopshackleState getState(A decidingAgent) {
		if (decidingAgent.isDead()) return HopshackleState.getState(stateType + ":DEAD");
		return getState(getCurrentState(decidingAgent));
	}

	/*
	 * 	Returns a string representation of the value based on the number of buckets
	 *  that the state exploration is using. value must be between -1 and 1 inclusive
	 */
	private String pigeonHole(double value) {
		if (value <-1.00 || value > 1.00) {
			//	logger.info("Value of Genetic Variable is outside permissible range: " + value);
			if (value > 1.00) return String.valueOf(pigeonHoles+1);
			return "0";
		}

		if (value > 0.999) value = 0.999;
		double temp = 1.0 + (1.0 + value)/2.0 * pigeonHoles;
		// so will return a number between 1 and pigeonHoles

		return String.valueOf((int) temp);
	}

	public String getStateType() {
		return stateType;
	}

	public void setStateType(String stateType) {		
		(HopshackleState.getState(stateType+":DEAD")).setValue(0.0);
		this.stateType = stateType;
	}

	public int getPigeonHoles() {
		return pigeonHoles;
	}

	public void setPigeonHoles(int pigeonHoles) {
		this.pigeonHoles = pigeonHoles;
	}

	public double getBaseValue() {
		return baseValue;
	}

	public void setBaseValue(double baseValue) {
		this.baseValue = baseValue;
	}

	@Override
	public void learnFrom(ExperienceRecord<A, S> exp, double maxResult) {
		ActionEnum<A> actionTaken = exp.getActionTaken().actionType;
		double observedResult = exp.getReward();
		HopshackleState startState = getState(exp.getStartState());
		HopshackleState endState = getState(exp.getEndState());
		if (exp.isInFinalState())
			endState = HopshackleState.getState(stateType + ":DEAD");
		startState.addExperience(actionTaken, endState, observedResult);
		startState.updateStateValue(actionTaken, endState, observedResult);
	}
}
