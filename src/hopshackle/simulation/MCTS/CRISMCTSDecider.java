package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

public class CRISMCTSDecider<A extends Agent> extends OLMCTSMasterDecider<A> {

    OpenLoopTreeTracker<A> masterGameTreeTracker;

    public CRISMCTSDecider(Game<A, ActionEnum<A>> game, StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    @Override
    protected void executeSearch(Game<A, ActionEnum<A>> clonedGame) {
        int currentPlayer = clonedGame.getCurrentPlayerRef();

        // we use an APD instead of a single game with CRIS
        Map<Integer, MonteCarloTree> treeMap2 = treeMap.keySet().stream().collect(Collectors.toMap(Function.identity(), treeMap::get));
        AllPlayerDeterminiser apd = AllPlayerDeterminiser.getAPD(clonedGame, currentPlayer, treeSetting, treeMap2);
        CRISChildDecider<A> childDecider = createChildDecider(apd, treeMap, currentPlayer, false);

        MCTSUtilities.launchGame(treeMap, apd, childDecider, opponentModel, decProp);
    }

    public void executeBranchingSearch(Game<A, ActionEnum<A>> clonedGame,
                                       BackPropagationTactics bpTactics, List<ActionWithRef<A>> initialActions) {
        // For a CRISDecider, we always start from the stopNodes specified in bpTactics
        int currentPlayer = clonedGame.getCurrentPlayerRef();
        if (bpTactics.getStartNode(currentPlayer) != null)
            throw new AssertionError("StartNodes should not be used for vanilla CRIS");
        if (bpTactics.getStopNode(currentPlayer) == null)
            throw new AssertionError("Vanilla CRIS should always have a stop node");

        // we now construct treeMap for the spawned game. The idea is simply to use the parent trees, but
        // with the root set to the node at which BP should stop.
        Map<Integer, MonteCarloTree<A>> reducedTreeMap = new HashMap<>();
        Map<Integer, MonteCarloTree> reducedTreeMapForAPD = new HashMap<>();
        for (int i = 1; i <= clonedGame.getPlayerCount(); i++) {
            MonteCarloTree<A> reducedTree = new OpenLoopMCTree<>(decProp, clonedGame.getPlayerCount());
            reducedTree.insertRoot(stateFactory.getCurrentState(clonedGame.getPlayer(i)), bpTactics.getStopNode(i));
            reducedTreeMap.put(i, reducedTree);
            reducedTreeMapForAPD.put(i, reducedTree);
        }
        AllPlayerDeterminiser apd = AllPlayerDeterminiser.getAPD(clonedGame, currentPlayer, treeSetting, reducedTreeMapForAPD);
        CRISChildDecider<A> childDecider = createChildDecider(apd, reducedTreeMap, currentPlayer, false);
        MCTSUtilities.launchGame(reducedTreeMap, apd, childDecider, opponentModel, decProp, bpTactics, initialActions);
    }

    @Override
    public CRISChildDecider<A> createChildDecider(Game clonedGame, Map<Integer, MonteCarloTree<A>> localTreeMap, int currentPlayer, boolean opponent) {
        CRISChildDecider<A> retValue;
        // new for CRIS is that we generate an APD around the cloned game
        if (!(clonedGame instanceof AllPlayerDeterminiser)) {
            throw new AssertionError("This should be an APD");
        }
        AllPlayerDeterminiser apd = (AllPlayerDeterminiser) clonedGame;

        OpenLoopTreeTracker<A> treeTracker = new OpenLoopTreeTracker<>(treeSetting, localTreeMap, clonedGame);
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new CRISChildDecider<>(apd, this, stateFactory, treeTracker, new MCActionValueDecider<>(localTreeMap.get(currentPlayer), stateFactory, currentPlayer), decProp);
        else
            retValue = new CRISChildDecider<>(apd, this, stateFactory, treeTracker, rolloutDecider, decProp);

        retValue.setName("Child_CRISMCTS");
        return retValue;
    }
}

