package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class OLMCTSMasterDecider<A extends Agent> extends MCTSMasterDecider<A> {

    private OpenLoopTreeTracker<A> masterGameTracker;

    public OLMCTSMasterDecider(Game<A, ActionEnum<A>> game, StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    @Override
    public MCTSChildDecider<A> createChildDecider(Game clonedGame, MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
        MCTSChildDecider<A> retValue;

        OpenLoopTreeTracker<A> treeTracker = new OpenLoopTreeTracker<>(treeSetting, treeMap, clonedGame);
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new OLMCTSChildDecider<>(stateFactory, treeTracker, new MCActionValueDecider<>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new OLMCTSChildDecider<>(stateFactory, treeTracker, rolloutDecider, decProp);

        retValue.setName("Child_OLMCTS");
        return retValue;
    }

    @Override
    protected void pruneTree(A agent) {
        switch (treeSetting) {
            case "ignoreOthers":
            case "singleTree":
                getTree(agent).pruneTree(masterGameTracker.getCurrentNode(agent.getActorRef()));
                break;
            case "perPlayer":
                treeMap.keySet().forEach(p -> treeMap.get(p).pruneTree(masterGameTracker.getCurrentNode(p)));
                break;
            default:
                throw new AssertionError("Unknown tree setting "+ treeSetting);
        }
    }

    @Override
    protected MonteCarloTree<A> getEmptyTree(int playerCount) {
        return new OpenLoopMCTree<>(decProp, playerCount);
    }

    @Override
    public void injectProperties(DeciderProperties decProp) {
        super.injectProperties(decProp);
        if (reuseOldTree && masterGameTracker == null) {
            // this tracks the current node for
            masterGameTracker = new OpenLoopTreeTracker<>(treeSetting, treeMap, masterGame);
        }
    }

}
