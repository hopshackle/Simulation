package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

public class CRISMCTSDecider<A extends Agent> extends MCTSMasterDecider<A>{

    public CRISMCTSDecider(StateFactory<A> stateFactory, BaseStateDecider<A> rolloutDecider, Decider<A> opponentModel) {
        super(stateFactory, rolloutDecider, opponentModel);
    }

    /*
    The key method to override. We need to:
       i)
     */
    protected void executeSearch(MonteCarloTree<A> tree, Game<A, ActionEnum<A>> clonedGame) {

    }

}
