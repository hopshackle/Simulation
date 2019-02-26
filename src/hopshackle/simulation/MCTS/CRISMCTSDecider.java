package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;
import java.util.stream.*;

public class CRISMCTSDecider<A extends Agent> extends OLMCTSMasterDecider<A> {

    public CRISMCTSDecider(Game<A, ActionEnum<A>> game, StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    protected void executeSearch(Game<A, ActionEnum<A>> clonedGame, BackPropagationTactics bpTactics, List<ActionWithRef<A>> initialActions) {
        MonteCarloTree<A> tree = getTree(clonedGame.getCurrentPlayer());
        int currentPlayer = clonedGame.getCurrentPlayerRef();

        // we use an APD instead of a single game with CRIS
        AllPlayerDeterminiser apd = AllPlayerDeterminiser.getAPD(clonedGame, currentPlayer);
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


class CRISChildDecider<P extends Agent> extends OLMCTSChildDecider<P>  {
    AllPlayerDeterminiser apd;
    CRISMCTSDecider<P> masterDecider;

    CRISChildDecider(AllPlayerDeterminiser<Game<P, ActionEnum<P>>, P> apd, CRISMCTSDecider<P> master, StateFactory<P> stateFactory, OpenLoopTreeTracker<P> treeTracker, Decider<P> rolloutDecider, DeciderProperties prop) {
        super(stateFactory, treeTracker, rolloutDecider, prop);
        this.apd = apd;
        masterDecider = master;
    }

    @Override
    protected ActionEnum<P> getNextTreeAction(State<P> state, List<ActionEnum<P>> chooseableOptions, int decidingAgentRef) {
            /*
           i) make a single decision
           ii) Check compatibility for APD
           iii) spawn branching game if needed
           iv) launchGame for the clonedGame we started with...once we have a compatible action
     */
        int count = 0;
        do {
            ActionEnum<P> initialChoice = super.getNextTreeAction(state, chooseableOptions, decidingAgentRef);
            List<Integer> incompatiblePlayers = apd.getIncompatible(new ActionWithRef(initialChoice, decidingAgentRef));
            if (incompatiblePlayers.isEmpty()) {
                return initialChoice;
            } else {
                spawnBranches(incompatiblePlayers, initialChoice, decidingAgentRef);
            }
            count++;
        } while (count < 200);
        throw new AssertionError("Unable to find a compatible action in resonable time");
    }

    protected void spawnBranches(List<Integer> playersToSpawn, ActionEnum<P> initialChoice, int decidingAgentRef) {
        for (int player : playersToSpawn) {

            // TODO : should only apply to actingagent
            // we spawn a separate playout for each incompatible determinisation separately
            List<ActionWithRef<P>> initialActions = new ArrayList<>();
            initialActions.add(new ActionWithRef<>(initialChoice, decidingAgentRef));
            int playerCount = apd.getMasterDeterminisation().getPlayerCount();
            Map<Integer, MCStatistics> stopNodes = new HashMap<>();
            IntStream.rangeClosed(1, playerCount).forEach(i -> stopNodes.put(i, treeTracker.getCurrentNode(i)));
            BackPropagationTactics bpTactics = new BackPropagationTactics(new HashMap<>(), stopNodes, playerCount);
            Game newBasisGame = apd.getDeterminisationFor(player).clone();
            masterDecider.executeSearch(newBasisGame, bpTactics, initialActions);
        }
    }
}