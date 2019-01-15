package hopshackle.simulation.games;

import hopshackle.simulation.*;
import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.games.resistance.Resistance;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AllPlayerDeterminiser<G extends Game, P extends Agent> extends Game<P, ActionEnum<P>> {

    public final int root;      // the master player ref
    protected MCStatistics<P> mctsNode;
    protected Map<Integer, G> determinisationsByPlayer;

    public AllPlayerDeterminiser(AllPlayerDeterminiser<G, P> apd) {
        root = apd.root;
        mctsNode = apd.mctsNode;
        determinisationsByPlayer = apd.determinisationsByPlayer.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> (G) e.getValue().clone()));
    }

    public AllPlayerDeterminiser(G rootGame, int rootPlayerID) {
        root = rootPlayerID;
        determinisationsByPlayer = new HashMap<>();

        int playerCount = rootGame.getAllPlayers().size();

        for (int i = 1; i <= playerCount; i++) {
            if (i == rootPlayerID)
                determinisationsByPlayer.put(i, rootGame);
            else
                determinisationsByPlayer.put(i, determinise(i, rootGame));
        }
    }

    /*
    Returns a cloned Game that is determinised from the pov of perspectivePlayer
     */
    public abstract G determinise(int perspectivePlayer, G game);

    /*
    Returns a Game that is a version of game (as far as possible), but which will be compatible
    with actionWithRef, were this to be applied to the game state
     */
    public abstract G compatibilise(int perspectivePlayer, ActionWithRef<P> actionWithRef, G game);

    /*
    This method should redeterminise the game for the specified playerID, while keeping within their current
    Information Set, so that the determinisation remains consistent with all previous events
    */
    public abstract void redeterminiseWithinIS(int playerID);

    /*
    This returns a boolean to indicate if the specified action when taken on the two games will give
    compatible game states
     */
    public abstract boolean isCompatible(ActionWithRef<P> actionWithRef, G masterGame, G determinisation);

    /*
    Apply the specific action to all determinisations
     */
    public void apply(ActionWithRef<P> actionWithRef) {
        for (int player : determinisationsByPlayer.keySet()) {
            G determinisation = determinisationsByPlayer.get(player);
            Action action = actionWithRef.actionTaken.getAction((P) determinisation.getCurrentPlayer());
            determinisation.applyAction(action);
        }
    }

    /*
    This determines if an action is compatible with all of the determinisations
    It returns a List of the player ids for which the action is incompatible, and hence need branching
    An empty list as the returned value means we can continue this game without branching
     */
    public List<Integer> getIncompatible(ActionWithRef<P> actionWithRef) {
        return determinisationsByPlayer.keySet().stream()
                .filter(i -> isCompatible(actionWithRef,
                        determinisationsByPlayer.get(root),
                        determinisationsByPlayer.get(i)))
                .collect(Collectors.toList());
    }

    /*
    Applies actionWithRef to all determinisations (compatibilising any first)
     */
    public void applyAndCompatibilise(ActionWithRef<P> actionWithRef) {
        G masterGame = determinisationsByPlayer.get(root);
        Map<Integer, G> newDeterminisations = determinisationsByPlayer.entrySet().stream()
                .map(entry -> isCompatible(actionWithRef, masterGame, entry.getValue())
                        ? entry
                        : new AbstractMap.SimpleEntry<>(entry.getKey(), compatibilise(entry.getKey(), actionWithRef, entry.getValue())))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
        determinisationsByPlayer = newDeterminisations;
    }

    /*
    The parent MCTS Node indicates where we stop back-propagation (up to, but excluding the node)
     */
    public void setParentNode(MCStatistics<P> node) {
        mctsNode = node;
    }

    public MCStatistics<P> getParentNode() {
        return mctsNode;
    }

    public G getDeterminisationFor(int playerID) {
        G retValue = determinisationsByPlayer.get(playerID);
        if (retValue == null)
            throw new AssertionError(playerID + " does not have a determinisation");
        return retValue;
    }

    public G getMasterDeterminisation() {
        return determinisationsByPlayer.get(root);
    }
}
