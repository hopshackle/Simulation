package hopshackle.simulation;

import java.util.*;

public abstract class LookaheadDecider<A extends Agent> extends BaseDecider<A> {

	private static boolean lookaheadQLearningStart = SimProperties.getProperty("LookaheadQLearningStart", "false").equals("true");
	private static boolean lookaheadQLearningEnd = SimProperties.getProperty("LookaheadQLearningEnd", "false").equals("true");
	private LookaheadFunction<A> lookahead;
	protected List<ActionEnum<A>> dummyActionSet;

	public LookaheadDecider(StateFactory<A> stateFactory, LookaheadFunction<A> lookahead, List<ActionEnum<A>> actions) {
		super(stateFactory, actions);
		this.lookahead = lookahead;
		dummyActionSet = new ArrayList<ActionEnum<A>>();
		dummyActionSet.add(DummyAction.DUMMY);
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		LookaheadState<A> currentState = (LookaheadState<A>) stateFactory.getCurrentState(decidingAgent);
		LookaheadState<A> futureState = lookahead.apply(currentState, option);
		double retValue = value(futureState);
		if (localDebug) {
			String message = "Option " + option.toString() + " has base Value of " + retValue; //+
				//	" with state representation of: \n \t" + Arrays.toString(futureState.getAsArray());
			decidingAgent.log(message);
			log(message);
		}
		return retValue;
	}
	
	public abstract double value(LookaheadState<A> state);
	
	// TODO: This may not be the best place for this method. Reconsider when refactoring at a later stage.
	/* This method takes the ER generated from the standard experience stream, and produces a dummy ER
	 * that can be used to train a LookaheadDecider. The key points are that:
	 * 	i) We lookahead from the startState/action to generate the feature set used in learning
	 *  ii) We set the possibleActions from the startState to be a single dummy action (as we are really just valuing the features, 
	 *  as they are a function of state and action already).
	 *  iii) Optionally, we also lookahead from the endState using each of the possibleActionsFromEndState. 
	 *  We then take the best scoring of these options as defining the actual endState we reach.
	 *  iv) We set possibleActionsFromEndState to be the dummyAction only. 
	 *  v) Note that this means featureTrace will not work for TD(lambda), as that would require the 
	 *  subsequent ExpRec also in pre-processed form.
	 */
	protected ExperienceRecord<A> preProcessExperienceRecord(ExperienceRecord<A> baseER) {
		// TODO: We should no longer need to apply the lookahead function to the start state, as this will be done 
		// within the ExperienceRecord. 
		// Instead we need to copy over ExperienceRecord as is (add a clone method?)
		// And then just update the actionTaken, nextActionTaken, possibleActions from Start/End States to be DUMMY
		// Also, we need to use the lookaheadStateArray and FeatureTraces during learning. So copy these over to the standard 
		// ones.
		// This is all to enable me to use the same ExperienceRecords for lookahead and non-lookahead deciders. Which I think
		// for the moment I do want to keep (although the current db persistence is insufficient)
		LookaheadState<A> startState = (LookaheadState<A>) baseER.getStartState();
		if (lookaheadQLearningStart) {
			startState = lookahead.apply(startState, baseER.getActionTaken().getType());
		}
		ExperienceRecord<A> retValue = new ExperienceRecord<A>(
				baseER.getAgent(), 
				startState,
				(Action<A>) DummyAction.DUMMY.getAction(baseER.getAgent()), 
				dummyActionSet);
		LookaheadState<A> endState = (LookaheadState<A>) baseER.getEndState();
		if (lookaheadQLearningEnd) {
			double highScore = Double.NEGATIVE_INFINITY;
			LookaheadState<A> bestForward = endState;
			for (ActionEnum<A> option : baseER.getPossibleActionsFromEndState()) {
				LookaheadState<A> forwardEndState = lookahead.apply(endState, option);
				double value = value(forwardEndState);
				if (value > highScore) {
					bestForward = forwardEndState;
					highScore = value;
				}
			}
			endState = bestForward;
		}
		retValue.updateWithResults(
				baseER.reward, 
				endState);
		retValue.possibleActionsFromEndState = dummyActionSet;
		retValue.startScore = baseER.startScore;
		retValue.endScore = baseER.endScore;
		retValue.isFinalState = baseER.isFinalState;
		retValue.setState(baseER.getState());
		// TODO: Move this to ExpRec creation, so that we can correctly set up the featureTrace needed for
		// TD(lambda) to work. 
		if (localDebug) {
			double maxResult = baseER.getAgent().getMaxScore();
			double startValue = value((LookaheadState<A>) retValue.getStartState()) * maxResult; // projection
			double endValue = value((LookaheadState<A>) retValue.getEndState()) * maxResult; // projection
			String message = String.format("Learning:\t%-20sScore: %.2f -> %.2f, State Valuation: %.2f -> %.2f, EndGame: %s", 
					retValue.getActionTaken(), retValue.getStartScore(), retValue.getEndScore(), endValue, startValue, retValue.isInFinalState());
			log(message);
			retValue.getAgent().log(message);
			double[] startArray = retValue.getStartStateAsArray();
			double[] endArray = retValue.getEndStateAsArray();
			double[] featureTrace = retValue.getFeatureTrace();
			StringBuffer logMessage = new StringBuffer("StartState -> EndState (FeatureTrace) :" + newline);
			for (int i = 0; i < startArray.length; i++) {
				if (startArray[i] != 0.0 || endArray[i] != 0.0 || Math.abs(featureTrace[i]) >= 0.01)
					logMessage.append(String.format("\t%.2f -> %.2f (%.2f) %s %s", startArray[i], endArray[i], featureTrace[i], stateFactory.getVariables().get(i).toString(), newline));
			}
			message = logMessage.toString();
			log(message);
			retValue.getAgent().log(message);
		}
		
		return retValue;
	}

}
