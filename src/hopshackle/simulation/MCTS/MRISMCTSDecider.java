package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;

public class MRISMCTSDecider<P extends Agent> extends MCTSMasterDecider<P> {

    private MRISGameDeterminiser<P> determiniser;

    public MRISMCTSDecider(StateFactory<P> stateFactory, BaseStateDecider<P> rolloutDecider, Decider<P> opponentModel) {
        super(stateFactory, rolloutDecider, opponentModel);
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
    public MCTSChildDecider<P> createChildDecider(Game clonedGame, MonteCarloTree<P> tree, int currentPlayer, boolean opponent) {
        MCTSChildDecider<P> retValue;

        StateFactory<P> childFactory = openLoop ? new OpenLoopStateFactory<>(treeSetting, treeMap, clonedGame) : stateFactory;
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new MRISChildDecider<>(determiniser, childFactory, tree, new MCActionValueDecider<>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new MRISChildDecider<>(determiniser, childFactory, tree, rolloutDecider, decProp);

        retValue.setName("Child_MRIS");
        return retValue;
    }
}


class MRISChildDecider<P extends Agent> extends MCTSChildDecider<P> {

    private MRISGameDeterminiser<P> determiniser;

    MRISChildDecider(MRISGameDeterminiser<P> gameDeterminiser, StateFactory<P> stateFactory, MonteCarloTree<P> tree, Decider<P> rolloutDecider, DeciderProperties prop) {
        super(stateFactory, tree, rolloutDecider, prop);
        this.determiniser = gameDeterminiser;
    }

    @Override
    protected ActionEnum<P> rolloutDecision(P decidingAgent, List<ActionEnum<P>> chooseableOptions) {
        determiniser.switchOn(false);
        return super.rolloutDecision(decidingAgent, chooseableOptions);
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

}