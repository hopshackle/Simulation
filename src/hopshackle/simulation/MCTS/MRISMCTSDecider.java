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
    protected void executeSearch(Game<P, ActionEnum<P>> clonedGame) {
        /*
        MRIS games work like normal IS-MCTS games, except that we redeterminise the game state after each move
        We do this by listening to the game, and mutable redeterminising it after each PLAYER_CHANGE event
         */
        determiniser = new MRISGameDeterminiser<>(clonedGame);
        clonedGame.registerListener(determiniser);
        super.executeSearch(clonedGame);
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
    private Game trackedGame;

    MRISGameDeterminiser(Game game) {
        trackedGame = game;
    }

    @Override
    public void processGameEvent(GameEvent<P> event) {
        if (!on) return;
        if (event.type == GameEvent.Type.PLAYER_CHANGE) {
            // event triggers after the player has changed
            trackedGame.redeterminise(trackedGame.getCurrentPlayerRef());
        }
    }

    void switchOn(boolean isOn) {
        on = isOn;
    }

}