package hopshackle.simulation.games.resistance;

import hopshackle.simulation.ActionWithRef;
import hopshackle.simulation.MCTS.MonteCarloTree;
import hopshackle.simulation.games.*;

import java.util.*;

public class ResistanceAPD extends AllPlayerDeterminiser<Resistance, ResistancePlayer> {

    public ResistanceAPD(Resistance game, int perspective, String treeSetting, Map<Integer, MonteCarloTree<ResistancePlayer>> treeMap) {
        super(game, perspective, treeSetting, treeMap);
    }

    public ResistanceAPD(ResistanceAPD apdToClone) {
        super(apdToClone);
    }

    @Override
    public Resistance determiniseFromRoot(int perspectivePlayer) {
        Resistance clonedGame = (Resistance) getMasterDeterminisation().clone();
        clonedGame.redeterminiseKeepingHiddenActions(perspectivePlayer, root, Optional.empty());
        return clonedGame;
    }

    @Override
    public void compatibilise(ActionWithRef<ResistancePlayer> actionWithRef, int perspective) {
        int count = 0;
        while (!isValid(actionWithRef, perspective)) {
            // we redeterminise the perspective agent's D. This needs to have perspective as traitor
            determinisationsByPlayer.get(perspective).redeterminiseKeepingHiddenActions(perspective, root,
                    Optional.of(getMasterDeterminisation()));
            count++;
            if (count > 200 || root == perspective)
                throw new AssertionError("Should have found a valid one by now!");
        }
    }

    @Override
    public void redeterminiseWithinIS(int playerID) {
        throw new AssertionError("Not yet implemented");

    }

    @Override
    public boolean isValid(ActionWithRef<ResistancePlayer> actionWithRef, int perspectivePlayer) {
        // any action is valid as long as it is not a DEFECT by a LOYALIST
        Resistance game = determinisationsByPlayer.get(perspectivePlayer);
        return !((actionWithRef.actionTaken instanceof Defect) && game.getLoyalists().contains(actionWithRef.agentRef));    }

    @Override
    public boolean isCompatible(ActionWithRef<ResistancePlayer> actionWithRef, int perspectivePlayer) {
        // In Resistance there is no additional information (cf the value of the card in Hanabi) that comes into general play
        return true;
    }

    @Override
    public Game cloneLocalFields() {
        return new ResistanceAPD(this);
    }

}
