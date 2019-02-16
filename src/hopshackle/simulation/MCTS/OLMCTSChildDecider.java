package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import java.util.List;

public class OLMCTSChildDecider<A extends Agent> extends MCTSChildDecider<A> {

    protected OpenLoopTreeTracker<A> treeTracker;

    public OLMCTSChildDecider(StateFactory<A> stateFactory, OpenLoopTreeTracker<A> treeTracker, Decider<A> rolloutDecider, DeciderProperties prop) {
        super(stateFactory, null, rolloutDecider, prop);
        this.treeTracker = treeTracker;
    }

    @Override
    protected ActionEnum<A> getNextTreeAction(State<A> state, List<ActionEnum<A>> chooseableOptions, int decidingAgentRef) {
        MCStatistics<A> currentPointer = treeTracker.getCurrentNode(decidingAgentRef);
        if (currentPointer != null) {
            return currentPointer.getNextAction(chooseableOptions, decidingAgentRef);
        }
        return null;
    }
}
