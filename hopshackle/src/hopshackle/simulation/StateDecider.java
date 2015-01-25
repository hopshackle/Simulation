package hopshackle.simulation;

import java.util.*;

public class StateDecider extends QDecider {

	private String stateType;
	private int pigeonHoles;
	private double baseValue;
	private double maxNoise, minNoise;

	public StateDecider(ArrayList<ActionEnum> actions, ArrayList<GeneticVariable> variables){
		super(actions, variables);
		setStateType("DEFAULT");
		pigeonHoles = 4;
		baseValue = SimProperties.getPropertyAsDouble("StateBaseValue", "10000");
		maxNoise = SimProperties.getPropertyAsDouble("StateMaxNoise", "1.0");
		minNoise = SimProperties.getPropertyAsDouble("StateMinNoise", "0.0");
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		HopshackleState agentState = getState(decidingAgent, contextAgent);
		return agentState.valueOption(option, temperature * (maxNoise - minNoise) + minNoise );
	}
	
	@Override
	public double valueOption(ActionEnum option, double[] state) {
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		HopshackleState agentState = getState(state);
		return agentState.valueOption(option, temperature * (maxNoise - minNoise) + minNoise );
	}

	protected HopshackleState getState(double[] stateDesc) {
		StringBuffer retStr = new StringBuffer( stateType );

		for (int i = 0; i < stateDesc.length; i++)
			retStr.append(":" + pigeonHole(stateDesc[i]));

		HopshackleState retValue =  HopshackleState.getState(retStr.toString());
		if (retValue.getVisited() == 0) 
			retValue.setValue(baseValue);
		// If the state has not been visited before, then we set the
		// default value to be baseValue
		return retValue;
	}
	
	public HopshackleState getState(Agent decidingAgent, Agent contextAgent) {
		if (decidingAgent.isDead()) return HopshackleState.getState(stateType + ":DEAD");
		double[] stateDesc = getCurrentState(decidingAgent, contextAgent);
		return getState(stateDesc);
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
	public void learnFrom(ExperienceRecord exp, double maxResult) {
		ActionEnum actionTaken = exp.getActionTaken();
		double observedResult = exp.getReward();
		HopshackleState startState = getState(exp.getStartState());
		HopshackleState endState = getState(exp.getEndState());
		if (exp.isInFinalState())
			endState = HopshackleState.getState(stateType + ":DEAD");
		startState.addExperience(actionTaken, endState, observedResult);
		startState.updateStateValue(actionTaken, endState, observedResult);
	}
}
