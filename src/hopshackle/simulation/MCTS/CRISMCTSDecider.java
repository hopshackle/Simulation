package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;
import java.util.stream.*;

public class CRISMCTSDecider<A extends Agent> extends OLMCTSMasterDecider<A> {

    OpenLoopTreeTracker<A> masterGameTreeTracker;

    public CRISMCTSDecider(Game<A, ActionEnum<A>> game, StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    protected void executeSearch(Game<A, ActionEnum<A>> clonedGame, BackPropagationTactics bpTactics, List<ActionWithRef<A>> initialActions) {
        MonteCarloTree<A> tree = getTree(clonedGame.getCurrentPlayer());
        int currentPlayer = clonedGame.getCurrentPlayerRef();

        // we use an APD instead of a single game with CRIS
        AllPlayerDeterminiser apd = AllPlayerDeterminiser.getAPD(clonedGame, currentPlayer);
        OpenLoopTreeTracker<A> treeTracker = new OpenLoopTreeTracker<>(treeSetting, treeMap, clonedGame);
        MCTSChildDecider<A> childDecider = createChildDecider(apd, tree, currentPlayer, false);

        if (useAVDForOpponent)
            opponentModel = new MCActionValueDecider<>(tree, stateFactory, currentPlayer);

        MCTSUtilities.launchGame(treeMap, apd, childDecider, opponentModel, decProp, bpTactics, initialActions);
    }


    @Override
    public MCTSChildDecider<A> createChildDecider(Game clonedGame, MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
        MCTSChildDecider<A> retValue;
        // new for CRIS is that we generate an APD around the cloned game
        if (!(clonedGame instanceof AllPlayerDeterminiser)) {
            throw new AssertionError("This should be an APD");
        }
        AllPlayerDeterminiser apd = (AllPlayerDeterminiser) clonedGame;

        OpenLoopTreeTracker<A> treeTracker = new OpenLoopTreeTracker<>(treeSetting, treeMap, clonedGame);
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new CRISChildDecider<>(apd, this, stateFactory, treeTracker, new MCActionValueDecider<>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new CRISChildDecider<>(apd, this, stateFactory, treeTracker, rolloutDecider, decProp);

        retValue.setName("Child_CRISMCTS");
        return retValue;
    }
}

