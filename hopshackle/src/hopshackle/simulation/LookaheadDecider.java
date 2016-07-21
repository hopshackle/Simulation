package hopshackle.simulation;

import java.util.*;

public abstract class LookaheadDecider<A extends Agent> extends BaseDecider<A> {
	public enum DummyState {
		DUMMY;
	}
	private ActionEnum<A> dummyAction = new ActionEnum<A>(){

		@Override
		public String getChromosomeDesc() {
			return "DUMMY";
		}

		@Override
		public Action<A> getAction(A a) {
			return null;
		}

		@Override
		public <B extends Agent> Action<A> getAction(A a1, B a2) {
			return null;
		}

		@Override
		public boolean isChooseable(A a) {
			return true;
		}

		@Override
		public Enum<?> getEnum() {
			return DummyState.DUMMY;
		}
		
	};
	
	private static boolean lookaheadQLearning = SimProperties.getProperty("LookaheadQLearning", "false").equals("true");
	private LookaheadFunction<A> lookahead;
	protected List<ActionEnum<A>> dummyActionSet;

	public LookaheadDecider(StateFactory<A> stateFactory, LookaheadFunction<A> lookahead, List<ActionEnum<A>> actions) {
		super(stateFactory, actions);
		this.lookahead = lookahead;
		dummyActionSet = new ArrayList<ActionEnum<A>>();
		dummyActionSet.add(dummyAction);
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		LookaheadState<A> currentState = lookahead.getCurrentState(decidingAgent);
		LookaheadState<A> futureState = lookahead.apply(currentState, option);
		return value(futureState);
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
	 */
	protected ExperienceRecord<A> preProcessExperienceRecord(ExperienceRecord<A> baseER) {
		ExperienceRecord<A> retValue = new ExperienceRecord<A>(
				baseER.getAgent(), 
				lookahead.apply((LookaheadState<A>) baseER.getStartState(), baseER.getActionTaken().getType()),
				dummyAction.getAction(baseER.getAgent()), 
				dummyActionSet);
		LookaheadState<A> endState = (LookaheadState<A>) baseER.getEndState();
		if (lookaheadQLearning) {
			double highScore = Double.MIN_VALUE;
			LookaheadState<A> bestForward = null;
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
		retValue.endScore = baseER.endScore;
		retValue.setState(baseER.getState());
		
		return retValue;
	}

}
