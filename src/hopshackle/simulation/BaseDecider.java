package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/*
 * The two methods not implemented are:
 * 	valueOption
 * 	learnFrom
 */
public abstract class BaseDecider<A extends Agent> implements Decider<A> {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");
	protected String name = "DEFAULT";
	protected double maxChanceOfRandomChoice = SimProperties.getPropertyAsDouble("RandomDeciderMaxChance", "0.0");
	protected double minChanceOfRandomChoice = SimProperties.getPropertyAsDouble("RandomDeciderMinChance", "0.0");
	protected boolean localDebug = false;
	protected double gamma = SimProperties.getPropertyAsDouble("Gamma", "0.95");
	protected double alpha = SimProperties.getPropertyAsDouble("Alpha", "0.05");
	protected double lambda = SimProperties.getPropertyAsDouble("Lambda", "0.001");
	private EntityLog entityLogger;
	private static AtomicInteger idFountain = new AtomicInteger(0);
	protected StateFactory<A> stateFactory;
	private int id;

	public BaseDecider(StateFactory<A> stateFactory) {
		this.stateFactory = stateFactory;
		id = idFountain.incrementAndGet();
	}

	@Override
	public State<A> getCurrentState(A agent) {
		return stateFactory.getCurrentState(agent);
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see hopshackle.simulation.Decider#learnFromBatch(hopshackle.simulation.ExperienceRecord[], double)
	 * A default method. Override this for efficiency with batch data.
	 */
	public void learnFromBatch(ExperienceRecord<A>[] exp, double maxResult) {
		for (ExperienceRecord<A> er : exp) {
			learnFrom(er, maxResult);
		}
	}

	@Override
	public void learnFromBatch(List<ExperienceRecord<A>> exp, double maxResult) {
		@SuppressWarnings("unchecked")
		ExperienceRecord<A>[] asArray = new ExperienceRecord[exp.size()];
		for (int i = 0; i < exp.size(); i++)
			asArray[i] = exp.get(i);
		learnFromBatch(asArray, maxResult);
	}

	@Override
	public Action<A> decide(A decidingAgent, List<ActionEnum<A>> possibleActions) {
		ActionEnum<A> decisionMade = makeDecision(decidingAgent, possibleActions);
		Action<A> action = null;
		long chosenDuration = 0;
		long availableTime = decidingAgent.actionPlan.timeToNextActionStarts();
		if (decisionMade != null)
			action = decisionMade.getAction(decidingAgent);
		if (action != null) 
			chosenDuration = action.getEndTime() - decidingAgent.world.getCurrentTime();
		if (chosenDuration > availableTime && action != null) {
			action = null;
		} else {
			AgentEvent learningEvent = new AgentEvent(decidingAgent, AgentEvent.Type.DECISION_TAKEN, action, 
					this, HopshackleUtilities.convertList(possibleActions));
			action.eventDispatch(learningEvent);
			decidingAgent.actionPlan.addActionToAllPlans(action);
		}
		return action;
	}

	@Override
	public ActionEnum<A> makeDecision(A decidingAgent, List<ActionEnum<A>> options) {
		double temp = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		double explorationChance = (maxChanceOfRandomChoice - minChanceOfRandomChoice) * temp + minChanceOfRandomChoice;
		return makeDecision(decidingAgent, explorationChance, options);
	}

	@Override
	public ActionEnum<A> getOptimalDecision(A decidingAgent, List<ActionEnum<A>> options) {
		return makeDecision(decidingAgent, 0.0, options);
	}

	protected ActionEnum<A> makeDecision(A decidingAgent, double explorationChance, List<ActionEnum<A>> chooseableOptions) {
		if (decidingAgent.isDead()) return null;

		ActionEnum<A> winningChoice = null;
		if (chooseableOptions.isEmpty()) return null;

		double chance = Math.random();
		if (chance < explorationChance) {
			winningChoice = selectOptionUsingBoltzmann(chooseableOptions, decidingAgent);
		} else {
			winningChoice = selectOption(chooseableOptions, decidingAgent);
		}

		return winningChoice;
	}

	protected ActionEnum<A> selectOption(List<ActionEnum<A>> optionList, A decidingAgent) {
		ActionEnum<A> winningChoice = null;

		List<Double> optionValues = getValuesPerOption(optionList, decidingAgent);
		double highestScore = Double.NEGATIVE_INFINITY;
		for (int i = 0; i<optionList.size(); i++) {
			if (localDebug) {
				String message = String.format("Option %-20s has value %.3f", optionList.get(i).toString(), optionValues.get(i));
				log(message);
				decidingAgent.log(message);
			}
			if (optionValues.get(i) > highestScore) {
				highestScore = optionValues.get(i);
				winningChoice = optionList.get(i);
			}
		}

		if (localDebug) {
			String message = "Winning choice is " + winningChoice;
			log(message);
			decidingAgent.log(message);
		}

		if (winningChoice == null) {
			String logString = "No option chosen in Decider " + this.toString() + ". ";
			for (ActionEnum<A> option : optionList)
				logString = logString +  option.toString() + "   ";
			logger.warning(logString);
		}

		return winningChoice;
	}

	protected ActionEnum<A> selectOptionUsingBoltzmann(List<ActionEnum<A>> optionList, A decidingAgent) {
		ActionEnum<A> winningChoice = null;
		List<Double> optionWeightings = getNormalisedBoltzmannValuesPerOption(optionList, decidingAgent);
		double randomNumber = Math.random();
		double runningSum = 0.0;
		for (int loop = 0; loop < optionList.size(); loop++) {
			runningSum += optionWeightings.get(loop);
			if (runningSum > randomNumber) {
				winningChoice = optionList.get(loop);
				if (localDebug) {
					String message = String.format("Boltzmann choice is %s", optionList.get(loop).toString());
					log(message);
					decidingAgent.log(message);
				}
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

	protected List<Double> getValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent){
		List<Double> retValue = new ArrayList<Double>();
		for (int i = 0; i < optionList.size(); i++) {
			double optionValue = this.valueOption(optionList.get(i), decidingAgent);
			retValue.add(optionValue);
		}
		return retValue;
	}
	
	protected List<Double> getNormalisedBoltzmannValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent){
		double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		return getNormalisedBoltzmannValuesPerOption(optionList, decidingAgent, temperature);
	}

	protected List<Double> getNormalisedBoltzmannValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent, double temperature){
		List<Double> baseValuesPerOption = getValuesPerOption(optionList, decidingAgent);
		for (int i = 0; i < baseValuesPerOption.size(); i++)
			if (baseValuesPerOption.get(i) == Double.NaN)
				baseValuesPerOption.set(i, 0.0);
		double baseValue = Collections.max(baseValuesPerOption);
		if (baseValue < 0.01) 
			baseValue = -Collections.min(baseValuesPerOption);
		if (baseValue < 0.01)
			baseValue = 0.01;

		if (temperature < 0.01) temperature = 0.01;
		if (localDebug) {
			String message = "Base Value is " + baseValue;
			log(message);
			decidingAgent.log(message);
		}

		double sumOfActionValues = 0.0;
		for (int i = 0; i < optionList.size(); i++) {
			double val = baseValuesPerOption.get(i) / baseValue / temperature;
			double boltzmannValue = Math.exp(val);
			sumOfActionValues += boltzmannValue;
			baseValuesPerOption.set(i, boltzmannValue);
			if (localDebug) {
				String message = String.format("Option %d, %s, has weight %.4g", i, optionList.get(i).toString(), boltzmannValue);
				log(message);
				decidingAgent.log(message);
			}
		}
		for (int i = 0; i < optionList.size(); i++) {
			double normalisedValue = baseValuesPerOption.get(i) / sumOfActionValues;
			baseValuesPerOption.set(i, normalisedValue);
			if (localDebug) {
				String message = String.format("Option %-20s has normalised value %.4g", optionList.get(i).toString(), normalisedValue);
				log(message);
				decidingAgent.log(message);
			}
		}
		return baseValuesPerOption;
	}

	@Override
	public ActionEnum<A> decideWithoutLearning(A decidingAgent, List<ActionEnum<A>> options) {
		return makeDecision(decidingAgent, options);
	}

	@Override
	public <V extends GeneticVariable<A>> List<V> getVariables() {
		return stateFactory.getVariables();
	}

	/* 
	 *  Available for overriding if needed by the relevant sub-type of Decider.
	 *  Default behaviour is to return the Decider on which the call is made
	 */
	@Override
	public Decider<A> crossWith(Decider<A> otherDecider) {
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
