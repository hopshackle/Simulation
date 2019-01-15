package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class RISMasterDecider<A extends Agent> extends MCTSMasterDecider<A> {

    public RISMasterDecider(StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(stateFactory, rolloutDecider, opponentModel);
    }

    /*
    The key method to override. We need to:
        - Create a new APD when we redeterminise the state - instead of just redeterminising the Game as in IS-MCTS
        - then launch game with RISChildDecider - and the APD linked to this; we create a new RISChildDecider for each iteration

        In the ChildDecider
        - We make one decision
        - Compatibilise APD to the decision
        - proceed as normal
     */

    protected void executeSearch(MonteCarloTree<A> tree, Game<A, ActionEnum<A>> clonedGame) {
        int currentPlayer = clonedGame.getCurrentPlayer().getActorRef();
        AllPlayerDeterminiser apd = clonedGame.getAPD(currentPlayer);
        // this cannot be a Decider level variable, and should be linked to the Game

        MCTSChildDecider<A> childDecider;
        if (openLoop) {
            OpenLoopStateFactory<A> factory = new OpenLoopStateFactory<>(treeSetting, treeMap, clonedGame);
            childDecider = createChildDecider(apd, factory, tree, currentPlayer, false);
        } else {
            childDecider = createChildDecider(apd, stateFactory, tree, currentPlayer, false);
        }
        if (useAVDForOpponent)
            opponentModel = new MCActionValueDecider<>(tree, this.stateFactory, currentPlayer);

        MCTSUtilities.launchGame(treeMap, apd, childDecider, opponentModel, decProp);
    }

    public MCTSChildDecider<A> createChildDecider(AllPlayerDeterminiser<?, A> apd, StateFactory<A> stateFactoryForChild, MonteCarloTree<A> tree, int currentPlayer, boolean opponent) {
        RISChildDecider<A> retValue;
        if ((useAVDForRollout && !opponent) || (useAVDForOpponent && opponent))
            retValue = new RISChildDecider<>(apd, stateFactoryForChild, tree, new MCActionValueDecider<>(tree, stateFactory, currentPlayer), decProp);
        else
            retValue = new RISChildDecider<>(apd, stateFactoryForChild, tree, rolloutDecider, decProp);

        retValue.setName("Child_RIS_MCTS");
        return retValue;
    }

}
