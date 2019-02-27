package hopshackle.simulation.games;

import hopshackle.simulation.*;
import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.games.resistance.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AllPlayerDeterminiser<G extends Game<P, ActionEnum<P>>, P extends Agent> extends Game<P, ActionEnum<P>> implements GameListener<P> {

    public final int root;      // the master player ref
    protected Map<Integer, G> determinisationsByPlayer;
    protected Set<GameEvent<P>> allMessages = new HashSet<>();

    public static AllPlayerDeterminiser getAPD(Game game, int perspective) {
        if (game instanceof Resistance)
            return new ResistanceAPD((Resistance) game, perspective);
        throw new AssertionError("Unknown game type " + game.getClass());
    }

    public AllPlayerDeterminiser(AllPlayerDeterminiser<G, P> apd) {
        root = apd.root;
        determinisationsByPlayer = apd.determinisationsByPlayer.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> (G) e.getValue().clone()));
        determinisationsByPlayer.values().forEach(g -> g.registerListener(this));
    }

    /*
    rootGame is not re-determinised. We simply generate a determinisation for every other player
    that is compatible with the IS of the rootPlayer (i.e. it is a state the rootPlayer believes they could believe)
     */
    public AllPlayerDeterminiser(G rootGame, int rootPlayerID) {
        root = rootPlayerID;
        determinisationsByPlayer = new HashMap<>();
        determinisationsByPlayer.put(rootPlayerID, rootGame);

        int playerCount = rootGame.getPlayerCount();

        for (int i = 1; i <= playerCount; i++) {
            if (i != rootPlayerID)
                determinisationsByPlayer.put(i, determiniseFromRoot(i));
        }
        determinisationsByPlayer.values().forEach(g -> g.registerListener(this));
    }

    /*
    Returns a clone of the rootGame that is determinised from the pov of perspectivePlayer
    and that is compatible (i.e. the root player believes perspectivePlayer could believe)
     */
    public abstract G determiniseFromRoot(int perspectivePlayer);

    /*
    Adjusts/Hacks all Games in the APD, so that the actionWithRef is both valid and compatible
     */
    public abstract void compatibilise(ActionWithRef<P> actionWithRef, int perspectivePlayer);

    /*
    This method should redeterminise the game for the specified playerID, while keeping within their current
    Information Set, so that the determinisation remains consistent with all previous events
    */
    public abstract void redeterminiseWithinIS(int playerID);

    /*
    This returns a boolean to indicate if, when the specified action is applied to the perspectivePlayer game,
    will it give a public IS that is the same as that from applying the action to the rootGame?
     */
    public abstract boolean isCompatible(ActionWithRef<P> actionWithRef, int perspectivePlayer);

    //    This returns a boolean to indicate if the specified action can be applied to the perspectivePlayer game.
    public abstract boolean isValid(ActionWithRef<P> actionWithRef, int perspectivePlayer);

    /*
    Apply the specific action to all determinisations
    We force compatibilise any determination that would otherwise be out of kilter
     */
    @Override
    public void apply(ActionWithRef<P> actionWithRef) {
        for (int player : determinisationsByPlayer.keySet()) {
            G determinisation = determinisationsByPlayer.get(player);
            if (!isValid(actionWithRef, player) || !isCompatible(actionWithRef, player))
                compatibilise(actionWithRef, player);
            // TODO: This doesn't feel like the right place to put this (and should be done before calling apply)
            // but at the time of writing, this is only called from MCTSUtilities.launchGame() for CRIS deciders
            // when the initialAction is made for a new branch...and in this case it is safe.
            // and we can't just put it in launchGame as that could be called on Game or APD
            Action action = actionWithRef.actionTaken.getAction(determinisation.getCurrentPlayer());
            determinisation.applyAction(action);
        }
    }

    @Override
    public void applyAction(Action<P> action) {
        // we should not need to compatibilise here, as this is called from Game.oneAction()
        // and the CRISDecider is responsible for ensuring the APD is compatible before returning the selected action
        // ...need to create a separate action for each game (and use the given action for the rootGame
        determinisationsByPlayer.keySet().forEach(p -> {
            G g = getDeterminisationFor(p);
            Action<P> actionCopy = g.getCurrentPlayer() == action.getActor() ? action : action.getType().getAction(g.getCurrentPlayer());
            g.applyAction(actionCopy);
        });
    }

    /*
    This determines if an action is compatible with all of the determinisations
    It returns a List of the player ids for which the action is incompatible, and hence need branching
    An empty list as the returned value means we can continue this game without branching
     */
    public List<Integer> getIncompatible(ActionWithRef<P> actionWithRef) {
        return determinisationsByPlayer.keySet().stream()
                .filter(i -> !isValid(actionWithRef, i) || !isCompatible(actionWithRef, i))
                .collect(Collectors.toList());
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

    @Override
    public P getCurrentPlayer() {
        return determinisationsByPlayer.get(getMasterDeterminisation().getCurrentPlayerRef()).getCurrentPlayer();
    }

    @Override
    public List<P> getAllPlayers() {
        List<P> retValue = determinisationsByPlayer.keySet().stream()
                .map(i -> determinisationsByPlayer.get(i).getPlayer(i))
                .collect(Collectors.toList());
        return retValue;
    }

    @Override
    public int getPlayerCount() {
        return getMasterDeterminisation().getPlayerCount();
    }

    @Override
    public int getPlayerNumber(Agent player) {
        throw new AssertionError("Not Implemented");
    }

    @Override
    public P getPlayer(int n) {
        return determinisationsByPlayer.get(n).getPlayer(n);
    }

    @Override
    public List<ActionEnum<P>> getPossibleActions() {
        int activePlayer = getMasterDeterminisation().getCurrentPlayerRef();
        return determinisationsByPlayer.get(activePlayer).getPossibleActions();
    }

    @Override
    public boolean gameOver() {
        return getMasterDeterminisation().gameOver();
    }

    @Override
    public void updateGameStatus() {
        determinisationsByPlayer.values().forEach(Game::updateGameStatus);
    }

    @Override
    public void redeterminiseKeepingHiddenActions(int perspectivePlayer, int ISPlayer, Optional rootGame) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public void redeterminise(int perspectivePlayer, int ISPlayer, Optional rootGame) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public String logName() {
        return "APD_" + getMasterDeterminisation().logName();
    }

    @Override
    public void processGameEvent(GameEvent<P> event) {
        // received from component games
        // we will get huge numbers of duplicates, and only want to publish one of each
        if (allMessages.contains(event))
            return;
        allMessages.add(event);
        sendMessage(event);
    }
}
