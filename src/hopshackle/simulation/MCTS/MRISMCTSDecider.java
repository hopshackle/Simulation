package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;

public class MRISMCTSDecider<P extends Agent> extends OLMCTSMasterDecider<P> {

    private MRISGameDeterminiser<P> determiniser;

    public MRISMCTSDecider(Game<P, ActionEnum<P>> game, StateFactory<P> stateFactory, BaseStateDecider<P> rolloutDecider, Decider<P> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    @Override
    protected void preIterationProcessing(Game clonedGame, Game rootGame) {
        int perspective = clonedGame.getCurrentPlayerRef();
        clonedGame.redeterminise(perspective, perspective, Optional.empty());
        // initial determinisation is as for IS-MCTS

        // we then kick off determiniser to re-determinise before each tree action is taken
        Optional<Game> rootOption = MRISRootConsistency ? Optional.of(rootGame) : Optional.empty();
        determiniser = new MRISGameDeterminiser<>(clonedGame, rootOption);
        clonedGame.registerListener(determiniser);
        if (debug) {
            clonedGame.debug = true;
            clonedGame.log(String.format("Starting status of clonedGame: %s", clonedGame.toString()));
        }
    }

    @Override
    protected void postIterationProcessing(Game clonedGame) {
        if (debug) clonedGame.log(String.format("MRIS Redeterminisations: %d", determiniser.count));
    }

    @Override
    public MCTSChildDecider<P> createChildDecider(Game clonedGame, Map<Integer, MonteCarloTree<P>> localTreeMap, int currentPlayer, boolean opponent) {
        MCTSChildDecider<P> retValue;

        OpenLoopTreeTracker<P> treeTracker = new OpenLoopTreeTracker<>(treeSetting, localTreeMap, clonedGame);
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new MRISChildDecider<>(determiniser, stateFactory, treeTracker, new MCActionValueDecider<>(localTreeMap.get(currentPlayer), stateFactory, currentPlayer), decProp);
        else
            retValue = new MRISChildDecider<>(determiniser, stateFactory, treeTracker, rolloutDecider, decProp);

        retValue.setName("Child_MRIS");
        return retValue;
    }
}


class MRISChildDecider<P extends Agent> extends OLMCTSChildDecider<P> {

    private MRISGameDeterminiser<P> determiniser;

    MRISChildDecider(MRISGameDeterminiser<P> gameDeterminiser, StateFactory<P> stateFactory, OpenLoopTreeTracker<P> treeTracker, Decider<P> rolloutDecider, DeciderProperties prop) {
        super(stateFactory, treeTracker, rolloutDecider, prop);
        this.determiniser = gameDeterminiser;
    }

    @Override
    protected ActionEnum<P> getNextTreeAction(State<P> state, List<ActionEnum<P>> chooseableOptions, int decidingAgentRef) {
        MCStatistics<P> currentPointer = treeTracker.getCurrentNode(decidingAgentRef);
        if (currentPointer != null) {
            ActionEnum<P> retValue = currentPointer.getNextAction(chooseableOptions, decidingAgentRef);
            if (currentPointer.getSuccessorNode(new ActionWithRef<>(retValue, decidingAgentRef)) == null) {
                // we will expand this node on BP
                determiniser.switchOn(false);
            }
            return retValue;
        }
        determiniser.switchOn(false);
        return null;
    }

}

class MRISGameDeterminiser<P extends Agent> implements GameListener<P> {

    private boolean on = true;
    int count = 0;
    private Game trackedGame;
    private Optional<Game> rootGame;

    MRISGameDeterminiser(Game game, Optional<Game> rootGame) {
        trackedGame = game;
        this.rootGame = rootGame;
    }

    @Override
    public void processGameEvent(GameEvent<P> event) {
        if (!on) return;
        if (event.type == GameEvent.Type.PLAYER_CHANGE) {
            // event triggers after previousPlayer has acted, and before the currentPlayer takes their turn
            int ISPlayer = rootGame.isPresent() ? rootGame.get().getCurrentPlayerRef() : event.actionTaken.agentRef;
            trackedGame.redeterminise(trackedGame.getCurrentPlayerRef(), ISPlayer, rootGame);
            count++;
        }
    }

    void switchOn(boolean isOn) {
        on = isOn;
    }

    boolean isOn() {return on;}

}