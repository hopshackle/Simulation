package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public abstract class BaseDecider<A extends Agent> implements Decider<A> {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	protected List<ActionEnum<A>> actionSet = new ArrayList<ActionEnum<A>>();
	protected List<GeneticVariable> variableSet = new ArrayList<GeneticVariable>();
	protected Teacher<A> teacher;
	protected String name = "DEFAULT";
	protected double maxChanceOfRandomChoice = SimProperties.getPropertyAsDouble("RandomDeciderMaxChance", "0.0");
	protected double minChanceOfRandomChoice = SimProperties.getPropertyAsDouble("RandomDeciderMinChance", "0.0");
	protected boolean localDebug = true;
	protected double gamma = SimProperties.getPropertyAsDouble("Gamma", "0.95");
	protected double alpha = SimProperties.getPropertyAsDouble("Alpha", "0.05");
	protected double lambda = SimProperties.getPropertyAsDouble("Lambda", "0.001");
	private EntityLog entityLogger;
	private static AtomicInteger idFountain = new AtomicInteger(0);
	private int id;

	public BaseDecider(List<? extends ActionEnum<A>> actions, List<GeneticVariable> variables) {
		if (actions != null) {
			for (ActionEnum<A> ae : actions)
				actionSet.add(ae);
		}
		variableSet = variables;
		id = idFountain.incrementAndGet();
	}

	@Override
	public abstract double valueOption(ActionEnum<A> option, A decidingAgent, Agent contextAgent);

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		// no default implementation - requires override
	}

	@Override
	public void learnFromBatch(ExperienceRecord<A>[] exp, double maxResult) {
		// no default implementation - requires override
	}

	/*
	 *  getExperienceRecord() is called immediately after a decision is made, and returns
	 *  a record of initial set of conditions that will be required when the final result is known
	 *  and this can be applied to the relevant learning algorithm used by the Decider (if any)
	 *  
	 *  Once the result is known, the Teacher will call the 'learn' method on the Decider, providing it with the updated ExperienceRecord
	 */

	@Override
	public ActionEnum<A> decide(A decidingAgent) {
		ActionEnum<A> retValue = decide(decidingAgent, null);
		return retValue;
	}

	@Override
	public ActionEnum<A> decide(A decidingAgent, Agent contextAgent) {
		ActionEnum<A> decisionMade = makeDecision(decidingAgent, contextAgent);
		learnFromDecision(decidingAgent, contextAgent, decisionMade);

		return decisionMade;
	}

	protected ActionEnum<A> makeDecision(A decidingAgent, Agent contextAgent) {
		double temp = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		double explorationChance = (maxChanceOfRandomChoice - minChanceOfRandomChoice) * temp + minChanceOfRandomChoice;
		return makeDecision(decidingAgent, contextAgent, explorationChance);
	}

	@Override
	public ActionEnum<A> getOptimalDecision(A decidingAgent, Agent contextAgent) {
		return makeDecision(decidingAgent, contextAgent, 0.0);
	}

	protected ActionEnum<A> makeDecision(A decidingAgent, Agent contextAgent, double explorationChance) {
		if (decidingAgent.isDead()) return null;

		ActionEnum <A>winningChoice = null;
		List<ActionEnum<A>> chooseableOptions = getChooseableOptions(decidingAgent, contextAgent);
		if (chooseableOptions.size() == 0) return null;

		double chance = Math.random();
		if (chance < explorationChance) {
			winningChoice = selectOptionUsingBoltzmann(chooseableOptions, decidingAgent, contextAgent);
		} else {
			winningChoice = selectOption(chooseableOptions, decidingAgent, contextAgent);
		}

		return winningChoice;
	}

	public List<ActionEnum<A>> getChooseableOptions(A decidingAgent, Agent contextAgent) {
		List<ActionEnum<A>> retValue = new ArrayList<ActionEnum<A>>();
		for (ActionEnum<A> option : actionSet) {
			if (option.isChooseable(decidingAgent)) {
				retValue.add(option);
			}
		}
		return retValue;
	}

	protected ActionEnum<A> selectOption(List<ActionEnum<A>> optionList, A decidingAgent, Agent contextAgent) {
		ActionEnum<A> winningChoice = null;

		List<Double> optionValues = getValuesPerOption(optionList, decidingAgent, contextAgent);
		double highestScore = Double.NEGATIVE_INFINITY;
		for (int i = 0; i<optionList.size(); i++) {
			if (localDebug) 
				log(String.format("Option %-20s has value %.3f", optionList.get(i).toString(), optionValues.get(i)));
			if (optionValues.get(i) > highestScore) {
				highestScore = optionValues.get(i);
				winningChoice = optionList.get(i);
			}
		}

		if (localDebug) log("Winning choice is " + winningChoice);

		if (winningChoice == null) {
			String logString = "No option chosen in Decider " + this.toString() + ". ";
			for (ActionEnum<A> option : optionList)
				logString = logString +  option.toString() + "   ";
			logger.warning(logString);
		}

		return winningChoice;
	}

	protected ActionEnum<A> selectOptionUsingBoltzmann(List<ActionEnum<A>> optionList, A decidingAgent, Agent contextAgent) {
		ActionEnum<A> winningChoice = null;
		List<Double> optionWeightings = getNormalisedBoltzmannValuesPerOption(optionList, decidingAgent, contextAgent);
		double randomNumber = Math.random();
		double runningSum = 0.0;
		for (int loop = 0; loop < optionList.size(); loop++) {
			runningSum += optionWeightings.get(loop);
			if (runningSum > randomNumber) {
				winningChoice = optionList.get(loop);
				if (localDebug)
					log(String.format("Boltzmann choice is %s", optionList.get(loop).toString()));
				break;
			}
		}

		if (winningChoice == null) {
			String logString = "No option chosen in Decider " + this.toString() + ". ";
			for (ActionEnum<A> option : optionList)
				logString = logString +  option.toString() + "   ";
			logger.warning(logString);
			logger.warning(optionWeightings.toString());
		}

		return winningChoice;
	}

	protected List<Double> getValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent, Agent contextAgent){
		List<Double> retValue = new ArrayList<Double>();
		for (int i = 0; i < optionList.size(); i++) {
			double optionValue = this.valueOption(optionList.get(i), decidingAgent, contextAgent);
			retValue.add(optionValue);
		}
		return retValue;
	}

	protected List<Double> getNormalisedBoltzmannValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent, Agent contextAgent){
		List<Double> baseValuesPerOption = getValuesPerOption(optionList, decidingAgent, contextAgent);
		for (int i = 0; i < baseValuesPerOption.size(); i++)
			if (baseValuesPerOption.get(i) == Double.NaN)
				baseValuesPerOption.set(i, 0.0);
		double baseValue = Collections.max(baseValuesPerOption);
		if (baseValue < 0.01) 
			baseValue = -Collections.min(baseValuesPerOption);
		if (baseValue < 0.01)
			baseValue = 0.01;
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		if (temperature < 0.01) temperature = 0.01;
		if (localDebug) log("Base Value is " + baseValue);

		double sumOfActionValues = 0.0;
		for (int i = 0; i < optionList.size(); i++) {
			double val = baseValuesPerOption.get(i) / baseValue / temperature;
			double boltzmannValue = Math.exp(val);
			sumOfActionValues += boltzmannValue;
			baseValuesPerOption.set(i, boltzmannValue);
			if (localDebug) log(String.format("Option %d, %s, has weight %.4g", i, optionList.get(i).toString(), boltzmannValue));
		}
		for (int i = 0; i < optionList.size(); i++) {
			double normalisedValue = baseValuesPerOption.get(i) / sumOfActionValues;
			baseValuesPerOption.set(i, normalisedValue);
			if (localDebug) log(String.format("Option %-20s has normalised value %.4g", optionList.get(i).toString(), normalisedValue));
		}
		return baseValuesPerOption;
	}


	protected void learnFromDecision(A decidingAgent, Agent contextAgent, ActionEnum<A> decisionMade) {
		if (teacher != null) 
			teacher.registerDecision(decidingAgent, getExperienceRecord(decidingAgent, contextAgent, decisionMade));
	}

	protected ExperienceRecord<A> getExperienceRecord(A decidingAgent, Agent contextAgent, ActionEnum<A> option) {
		ExperienceRecord<A> output = new ExperienceRecord<A>(variableSet, getCurrentState(decidingAgent, contextAgent), option, 
				getChooseableOptions(decidingAgent, contextAgent));
		return output;
	}

	@Override
	public double[] getCurrentState(A decidingAgent, Agent contextAgent) {
		double[] inputs = new double[variableSet.size()];
		for (int i = 0; i < variableSet.size(); i ++) {
			GeneticVariable gv = variableSet.get(i);
			inputs[i] = gv.getValue(decidingAgent, contextAgent);
		}
		return inputs;
	}

	@Override
	public ActionEnum<A> decideWithoutLearning(A decidingAgent, Agent contextAgent) {
		return makeDecision(decidingAgent, contextAgent);
	}

	@Override
	public List<ActionEnum<A>> getActions() {
		return HopshackleUtilities.cloneList(actionSet);
	}
	public void setActions(List<? extends ActionEnum<A>> actionList) {
		actionSet = new ArrayList<ActionEnum<A>>();
		for (ActionEnum<A> ae : actionList) {
			actionSet.add(ae);
		}
	}
	@Override
	public List<GeneticVariable> getVariables() {
		return HopshackleUtilities.cloneList(variableSet);
	}
	public void setVariables(List<GeneticVariable> variableList) {
		variableSet = new ArrayList<GeneticVariable>();
		for (GeneticVariable gv : variableList) {
			variableSet.add(gv);
		}
	}

	@Override
	public void setTeacher(Teacher<A> teacher) {
		this.teacher = teacher;
	}
	@Override
	public Teacher<A> getTeacher() {
		return teacher;
	}

	/* 
	 *  Available for overriding if needed by the relevant sub-type of Decider.
	 *  Default behaviour is to return the Decider on which the call is made
	 */
	@Override
	public Decider<A> crossWith(Decider<A> careerDecider) {
		return this;
	}

	@Override
	public void setName(String newName) {
		name = newName;
		if (entityLogger != null)
			entityLogger.rename("Decider_" + name);
	}

	public String toString() {
		if (name != null)
			return name;
		return String.valueOf(id);
	}

	public void log(String s) {
		if (entityLogger == null) {
			entityLogger = new EntityLog("Decider_" + toString(), null);
		}
		entityLogger.log(s);
	}
}
