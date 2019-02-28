package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;

import java.util.*;
import java.util.stream.IntStream;

public class CRISChildDecider<P extends Agent> extends OLMCTSChildDecider<P> {
    AllPlayerDeterminiser apd;
    CRISMCTSDecider<P> masterDecider;

    public CRISChildDecider(AllPlayerDeterminiser<Game<P, ActionEnum<P>>, P> apd, CRISMCTSDecider<P> master, StateFactory<P> stateFactory, OpenLoopTreeTracker<P> treeTracker, Decider<P> rolloutDecider, DeciderProperties prop) {
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
            // TODO: If this is null, then we are out of the tree...but have no way of communicating this to the main process running the game
            ActionWithRef action = new ActionWithRef(initialChoice, decidingAgentRef);
            if (!(apd.isValid(action, apd.root) && apd.isCompatible(action, apd.root))) {
                spawnBranch(initialChoice, decidingAgentRef);
                // TODO: redeterminiseWithinIS in this case? This is needed for Hanabi, where it is not that the choice is invalid, but the determinisation gives a different result.
                // or possibly we check isCompatible for this? (This is really only a problem in the Hanabi-like case of an information partition that means that our move reveals hidden
                // hidden information (to us), that is known to others.
            } else {
                return initialChoice;
            }
            count++;
        } while (count < 200);
        throw new AssertionError("Unable to find a compatible action in resonable time");
    }

    protected void spawnBranch(ActionEnum<P> initialChoice, int decidingAgentRef) {
        List<ActionWithRef<P>> initialActions = new ArrayList<>();
        initialActions.add(new ActionWithRef<>(initialChoice, decidingAgentRef));
        int playerCount = apd.getMasterDeterminisation().getPlayerCount();
        Map<Integer, MCStatistics> stopNodes = new HashMap<>();
        IntStream.rangeClosed(1, playerCount).forEach(i -> stopNodes.put(i, treeTracker.getCurrentNode(i)));
        BackPropagationTactics bpTactics = new BackPropagationTactics(new HashMap<>(), stopNodes, playerCount);
        Game newBasisGame = apd.getDeterminisationFor(decidingAgentRef).clone();
        masterDecider.executeSearch(newBasisGame, bpTactics, initialActions);
    }

}