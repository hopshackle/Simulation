package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class CRISMCTSDecider<A extends Agent> extends MCTSMasterDecider<A>{

    public CRISMCTSDecider(Game<A, ActionEnum<A>> game, StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(game, stateFactory, rolloutDecider, opponentModel);
    }

    /*
    The key method to override. We need to:
       0) Create a new APD when we redeterminise the state - this the bit that we only do in the Master version
            the remainaing steps apply for both master and child Deciders
       i) make a single decision
       ii) Check compatibility for APD
       iii) spawn branching game if needed
       iv) launchGame for the clonedGame we started with...once we have a compatible action
     */
    protected void executeSearch(MonteCarloTree<A> tree, Game<A, ActionEnum<A>> clonedGame) {
        int currentPlayer = clonedGame.getCurrentPlayer().getActorRef();
    //    AllPlayerDeterminiser apd = clonedGame.allPlayerDeterminise(currentPlayer);
        // this cannot be a Decider level variable, and should be linked to the Game
    }

}

