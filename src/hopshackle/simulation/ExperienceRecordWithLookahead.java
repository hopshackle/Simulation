package hopshackle.simulation;

import java.util.*;

public class ExperienceRecordWithLookahead<A extends Agent> extends ExperienceRecord<A> {
	
	private static boolean lookaheadQLearningStart = SimProperties.getProperty("LookaheadQLearningStart", "false").equals("true");
	private static boolean lookaheadQLearningEnd = SimProperties.getProperty("LookaheadQLearningEnd", "false").equals("true");
	
	protected LookaheadFunction<A> lookahead;
	protected LookaheadState<A> lookaheadStartState;
	protected double[] lookaheadStartArray, lookaheadFeatureTrace;
	
	public ExperienceRecordWithLookahead(A a, LookaheadState<A> state, Action<A> action, List<ActionEnum<A>> possibleActions, LookaheadFunction<A> lookahead) {
		super(a, state, action, possibleActions);
		this.lookahead = lookahead;
		if (lookahead == null) 
			throw new AssertionError("Lookahead function must be non-null!");
		lookaheadStartState = lookahead.apply(state, action.getType());
		lookaheadStartArray = lookaheadStartState.getAsArray();
		lookaheadFeatureTrace = lookaheadStartArray;
	}
	
	private void constructLookaheadFeatureTrace(ExperienceRecordWithLookahead<A> previousER) {
		if (previousER.lookaheadFeatureTrace.length != lookaheadStartArray.length) {
			lookaheadFeatureTrace = lookaheadStartArray;
		} else {
			lookaheadFeatureTrace = new double[lookaheadStartArray.length];
			for (int i = 0; i < lookaheadStartArray.length; i++) {
				lookaheadFeatureTrace[i] = (gamma * lambda * previousER.lookaheadFeatureTrace[i]) + lookaheadStartArray[i];
				if (lookaheadFeatureTrace[i] > traceCap) 
					lookaheadFeatureTrace[i] = traceCap;
			}
		}
	}
	
	@Override
	public void updateNextActions(ExperienceRecord<A> nextER) {
		super.updateNextActions(nextER);
		if (nextER != null) {
			ExperienceRecordWithLookahead<A> nextERL = (ExperienceRecordWithLookahead<A>) nextER;
			nextERL.constructLookaheadFeatureTrace(this);
		}
	}
	
	public ExperienceRecord<A> convertToStandardER(LookaheadDecider<A> decider) {
		LookaheadState<A> newStartState = (LookaheadState<A>) this.startState;
		double[] newFeatureTrace = this.featureTrace;
		if (lookaheadQLearningStart) {
			newStartState = this.lookaheadStartState;
			newFeatureTrace = this.lookaheadFeatureTrace;
		}
		ExperienceRecord<A> retValue = new ExperienceRecord<A>(
				this.getAgent(), 
				newStartState,
				(Action<A>) DummyAction.DUMMY.getAction(this.getAgent()), 
				decider.dummyActionSet);
		retValue.timeOfDecision = this.timeOfDecision;
		LookaheadState<A> newEndState = (LookaheadState<A>) this.getEndState();
		if (lookaheadQLearningEnd) {
			double highScore = Double.NEGATIVE_INFINITY;
			LookaheadState<A> bestForward = newEndState;
			for (ActionEnum<A> option : this.getPossibleActionsFromEndState()) {
				LookaheadState<A> forwardEndState = lookahead.apply(newEndState, option);
				double value = decider.value(forwardEndState);
				if (value > highScore) {
					bestForward = forwardEndState;
					highScore = value;
				}
			}
			newEndState = bestForward;
		}
		retValue.updateWithResults(
				this.reward, 
				newEndState);
		retValue.possibleActionsFromEndState = decider.dummyActionSet;
		retValue.startScore = this.startScore;
		retValue.endScore = this.endScore;
		retValue.isFinalState = this.isFinalState;
		retValue.featureTrace = newFeatureTrace;
		retValue.timeOfResolution = this.timeOfResolution;
		retValue.setState(this.getState());
		return retValue;
	}
}
