package hopshackle.simulation;

import java.util.List;

public class LookaheadERFactory<A extends Agent> implements ExperienceRecordFactory<A> {
	
	private LookaheadFunction<A> lookahead;
	
	public LookaheadERFactory(LookaheadFunction<A> functionToUse) {
		lookahead = functionToUse;
	}

	@Override
	public ExperienceRecord<A> generate(A a, State<A> state, Action<A> action, List<ActionEnum<A>> possibleActions) {
		LookaheadState<A> lookaheadState = (LookaheadState<A>) state;
		return new ExperienceRecordWithLookahead<A>(a, lookaheadState, action, possibleActions, lookahead);
	}

}
