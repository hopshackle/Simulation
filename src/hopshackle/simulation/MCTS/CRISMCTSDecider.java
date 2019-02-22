package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.List;
import java.util.Optional;

public class CRISMCTSDecider<A extends Agent> extends OLMCTSMasterDecider<A> {

    AllPlayerDeterminiser apd;

    public CRISMCTSDecider(Game<A, ActionEnum<A>> game, StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    /*
    The key method to override. We need to:
       0) Create a new APD when we redeterminise the state - this the bit that we only do in the Master version
            the remaining steps apply for both master and child Deciders
       i) make a single decision
       ii) Check compatibility for APD
       iii) spawn branching game if needed
       iv) launchGame for the clonedGame we started with...once we have a compatible action
     */
    protected void executeSearch(MonteCarloTree<A> tree, Game<A, ActionEnum<A>> clonedGame) {

    }
    @Override
    protected void preIterationProcessing(Game clonedGame, Game rootGame) {
        int perspective = clonedGame.getCurrentPlayerRef();
        AllPlayerDeterminiser apd = AllPlayerDeterminiser.getAPD(clonedGame, perspective);
        // Unlike IS-MCTS, we determinise a game for every player independently
    }

    @Override
    public MCTSChildDecider<A> createChildDecider(Game clonedGame, MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
        MCTSChildDecider<A> retValue;

        OpenLoopTreeTracker<A> treeTracker = new OpenLoopTreeTracker<>(treeSetting, treeMap, clonedGame);
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new CRISChildDecider<>(stateFactory, treeTracker, new MCActionValueDecider<>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new CRISChildDecider<>(stateFactory, treeTracker, rolloutDecider, decProp);

        retValue.setName("Child_CRISMCTS");
        return retValue;
        // What is different abou CRISChildDEcider?
            // it may spawn off a new rollout (unlike OLMCTSChildDecider) if an incompatible action is made
    }
}



class CRISChildDecider<P extends Agent> extends OLMCTSChildDecider<P> {

    CRISChildDecider(StateFactory<P> stateFactory, OpenLoopTreeTracker<P> treeTracker, Decider<P> rolloutDecider, DeciderProperties prop) {
        super(stateFactory, treeTracker, rolloutDecider, prop);
    }
}