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
    protected boolean localDebug = false;
    protected DeciderProperties decProp;
    protected boolean absoluteDifferenceNoise, monteCarlo, useLookahead;
    protected double maxChanceOfRandomChoice = getPropertyAsDouble("RandomDeciderMaxChance", "0.0");
    protected double minChanceOfRandomChoice = getPropertyAsDouble("RandomDeciderMinChance", "0.0");
    protected double maxTemp = getPropertyAsDouble("StartTemperature", "1.0");
    protected double minTemp = getPropertyAsDouble("EndTemperature", "0.0");
    protected double gamma = getPropertyAsDouble("Gamma", "0.95");
    protected double alpha = getPropertyAsDouble("Alpha", "0.05");
    protected double lambda = getPropertyAsDouble("Lambda", "0.001");
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
        ExperienceRecord<A>[] asArray = new ExperienceRecord[exp.size()];
        for (int i = 0; i < exp.size(); i++)
            asArray[i] = exp.get(i);
        learnFromBatch(asArray, maxResult);
    }

    @Override
    public Action<A> decide(A decidingAgent, List<ActionEnum<A>> possibleActions) {
        List<ActionEnum<A>> remainingDecisions = HopshackleUtilities.cloneList(possibleActions);
        Action<A> action = null;
        //      do {
        // TODO: We did loop through all decisions to see if one fit. However we now also
        // check priority against the plan - which could mean that a low probability, high priority
        // decisoin *always* gets made
        // so when deciding here, we do not apply the actionPolicy to override existing plans
        // this is only used when an action is suggested by another agent (or an environmental
        // constraint outside of the main decision cycle)
        ActionEnum<A> decisionMade = makeDecision(decidingAgent, remainingDecisions);
        if (decisionMade != null) {
            action = decisionMade.getAction(decidingAgent);
            if (decidingAgent.getActionPlan().willFitInPlan(action, false)) {
                // we're good
            } else {
                action = null;
            }
        }
        remainingDecisions.remove(decisionMade);
        //      } while (action != null && action.isDeleted() && !remainingDecisions.isEmpty());

        if (action != null) {
            AgentEvent learningEvent = new AgentEvent(decidingAgent, AgentEvent.Type.DECISION_TAKEN, action,
                    this, HopshackleUtilities.convertList(possibleActions));
            action.eventDispatch(learningEvent);
            action.addToAllPlans();
        }
        return action;
    }

    @Override
    public ActionEnum<A> makeDecision(A decidingAgent, List<ActionEnum<A>> options) {
        double temp = SimProperties.getPropertyAsDouble("Temperature", "1.0");
        double explorationChance = (maxChanceOfRandomChoice - minChanceOfRandomChoice) * (temp - minTemp) / (maxTemp - minTemp) + minChanceOfRandomChoice;
//		decidingAgent.log(String.format("Temperature: %.2f, Exploration: %.2f", temp, explorationChance));
        return makeDecision(decidingAgent, explorationChance, options);
    }

    protected ActionEnum<A> makeDecision(A decidingAgent, double explorationChance, List<ActionEnum<A>> chooseableOptions) {
        if (decidingAgent.isDead()) return null;

        ActionEnum<A> winningChoice = null;
        if (chooseableOptions.isEmpty()) return null;

        if (chooseableOptions.size() == 1)
            return chooseableOptions.get(0);    // only one option, so no decision to take

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

        List<Double> optionValues = valueOptions(optionList, decidingAgent);
        double highestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < optionList.size(); i++) {
            if (localDebug || decidingAgent.getDebugDecide()) {
                if (!decidingAgent.getDebugDecide()) decidingAgent.setDebugDecide(true);
                String message = String.format("Option %-20s has value %.3f", optionList.get(i).toString(), optionValues.get(i));
                if (localDebug) log(message);
                decidingAgent.log(message);
            }
            if (optionValues.get(i) > highestScore) {
                highestScore = optionValues.get(i);
                winningChoice = optionList.get(i);
            }
        }

        if (localDebug || decidingAgent.getDebugDecide()) {
            String message = "Winning choice is " + winningChoice;
            if (localDebug) log(message);
            decidingAgent.log(message);
        }

        if (winningChoice == null) {
            String logString = "No option chosen in Decider " + this.toString() + ". ";
            for (ActionEnum<A> option : optionList)
                logString = logString + option.toString() + "   ";
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
                if (localDebug || decidingAgent.getDebugDecide()) {
                    String message = String.format("Boltzmann choice is %s (Roll: %.3f)", optionList.get(loop).toString(), randomNumber);
                    if (localDebug) log(message);
                    decidingAgent.log(message);
                }
                break;
            }
        }

        if (winningChoice == null) {
            String logString = "No option chosen in Decider " + this.toString() + ". ";
            for (ActionEnum<A> option : optionList)
                logString = logString + option.toString() + "   ";
            logger.warning(logString);
            logger.warning(optionWeightings.toString());
        }

        return winningChoice;
    }

    @Override
    public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state) {
        List<Double> retValue = new ArrayList<Double>(options.size());
        for (int i = 0; i < options.size(); i++) retValue.add(0.0);
        for (int i = 0; i < options.size(); i++) {
            double optionValue = this.valueOption(options.get(i), state);
            retValue.set(i, optionValue);
        }
        return retValue;
    }

    protected List<Double> getNormalisedBoltzmannValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent) {
        double temperature = SimProperties.getPropertyAsDouble("Temperature", "1.0");
        return getNormalisedBoltzmannValuesPerOption(optionList, decidingAgent, temperature);
    }

    protected List<Double> getNormalisedBoltzmannValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent, double temperature) {
        List<Double> baseValuesPerOption = valueOptions(optionList, decidingAgent);
        for (int i = 0; i < baseValuesPerOption.size(); i++)
            if (baseValuesPerOption.get(i) == Double.NaN)
                baseValuesPerOption.set(i, 0.0);
        double baseValue = Collections.max(baseValuesPerOption);
        if (baseValue < 0.01)
            baseValue = -Collections.min(baseValuesPerOption);
        if (baseValue < 0.01)
            baseValue = 0.01;

        if (temperature < 0.001) temperature = 0.001;
        if (localDebug || decidingAgent.getDebugDecide()) {
            String message = "Base Value is " + baseValue;
            if (localDebug) log(message);
            decidingAgent.log(message);
        }

        double sumOfActionValues = 0.0;
        double maxValue = decidingAgent.getMaxScore();
        if (maxValue < 1.0) maxValue = 1.0;
        for (int i = 0; i < optionList.size(); i++) {
            double val = baseValuesPerOption.get(i) / baseValue / maxValue / temperature;
            if (absoluteDifferenceNoise) val = (baseValuesPerOption.get(i) - baseValue) / maxValue / temperature;
            double boltzmannValue = Math.exp(val);
            if (boltzmannValue > 1e+20 || Double.isNaN(boltzmannValue)) boltzmannValue = 1e+20;
            if (boltzmannValue < 1e-20) boltzmannValue = 1e-20;
            sumOfActionValues += boltzmannValue;
            baseValuesPerOption.set(i, boltzmannValue);
            if (localDebug || decidingAgent.getDebugDecide()) {
                String message = String.format("Option %d, %s, has weight %.4g", i, optionList.get(i).toString(), boltzmannValue);
                if (localDebug) log(message);
                decidingAgent.log(message);
            }
        }
        for (int i = 0; i < optionList.size(); i++) {
            double normalisedValue = baseValuesPerOption.get(i) / sumOfActionValues;
            if (Double.isNaN(normalisedValue)) {
                throw new AssertionError(String.format("normalisedValue is invalid (%.3g / %.3g)", baseValuesPerOption.get(i), sumOfActionValues));
            }
            baseValuesPerOption.set(i, normalisedValue);
            if (localDebug || decidingAgent.getDebugDecide()) {
                String message = String.format("Option %-20s has normalised value %.3f", optionList.get(i).toString(), normalisedValue);
                if (localDebug) log(message);
                decidingAgent.log(message);
            }
        }
        return baseValuesPerOption;
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
    public Decider<A> mutate(double intensity) {
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

    @Override
    public void log(String s) {
        if (entityLogger == null) {
            entityLogger = new EntityLog("Decider_" + toString(), null);
        }
        entityLogger.log(s);
    }

    @Override
    public void flushLog() {
        if (entityLogger != null)
            entityLogger.flush();
    }

    @Override
    public void injectProperties(DeciderProperties dp) {
        decProp = dp;
        maxChanceOfRandomChoice = getPropertyAsDouble("RandomDeciderMaxChance", "0.0");
        minChanceOfRandomChoice = getPropertyAsDouble("RandomDeciderMinChance", "0.0");
        absoluteDifferenceNoise = getProperty("BoltzmannValueDifference", "absolute").equals("absolute");
        gamma = getPropertyAsDouble("Gamma", "0.95");
        alpha = getPropertyAsDouble("Alpha", "0.05");
        lambda = getPropertyAsDouble("Lambda", "0.001");
        monteCarlo = getProperty("MonteCarloReward", "false").equals("true");
        useLookahead = getProperty("LookaheadQLearning", "false").equals("true");
    }

    @Override
    public DeciderProperties getProperties() {
        if (decProp == null)
            return SimProperties.getDeciderProperties("GLOBAL");
        return decProp;
    }

    public double getPropertyAsDouble(String prop, String defaultValue) {
        if (decProp == null) {
            return SimProperties.getPropertyAsDouble(prop, defaultValue);
        }
        return decProp.getPropertyAsDouble(prop, defaultValue);
    }

    public int getPropertyAsInteger(String prop, String defaultValue) {
        if (decProp == null) {
            return SimProperties.getPropertyAsInteger(prop, defaultValue);
        }
        return decProp.getPropertyAsInteger(prop, defaultValue);
    }

    public String getProperty(String prop, String defaultValue) {
        if (decProp == null) {
            return SimProperties.getProperty(prop, defaultValue);
        }
        return decProp.getProperty(prop, defaultValue);
    }
}
