package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import java.util.*;

public class RISChildDecider<P extends Agent> extends MCTSChildDecider<P> {

    private AllPlayerDeterminiser<?, P> apd;

    public RISChildDecider(AllPlayerDeterminiser<?, P> allPlayerDeterminiser, StateFactory<P> stateFactory,
                           MonteCarloTree<P> tree, Decider<P> rolloutDecider, DeciderProperties prop) {
        super(stateFactory, tree, rolloutDecider, prop);
        this.apd = allPlayerDeterminiser;
    }

    @Override
    public ActionEnum<P> makeDecision(P decidingAgent, List<ActionEnum<P>> chooseableOptions) {
        if (decidingAgent.isDead()) return null;

        State<P> state = stateFactory.getCurrentState(decidingAgent);
        if (chooseableOptions.isEmpty()) {
            return null;
        }

        int decidingAgentRef = decidingAgent.getActorRef();
        ActionEnum<P> retValue = tree.getNextAction(state, chooseableOptions, decidingAgentRef);
        if (retValue == null) {
            retValue = rolloutDecider.makeDecision(decidingAgent, chooseableOptions);
        } else {
            // the new bit...we compatibilise the APD

        }
        return retValue;
    }
}
