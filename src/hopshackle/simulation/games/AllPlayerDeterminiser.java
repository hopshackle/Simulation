package hopshackle.simulation.games;

import hopshackle.simulation.*;
import hopshackle.simulation.MCTS.*;
import hopshackle.simulation.games.resistance.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AllPlayerDeterminiser<G extends Game<P, ActionEnum<P>>, P extends Agent> extends Game<P, ActionEnum<P>> implements GameListener<P> {

    public final int root;      // the master player ref
    private boolean inRolloutMode;
    protected Map<Integer, G> determinisationsByPlayer;
    protected OpenLoopTreeTracker<P> treeTracker;

    public static AllPlayerDeterminiser getAPD(Game game, int perspective,
                                               String treeSetting, Map<Integer, MonteCarloTree> treeMap) {
        if (game instanceof Resistance) {
            Map<Integer, MonteCarloTree<ResistancePlayer>> treeMap2 = treeMap.keySet().stream()
                    .collect(Collectors.toMap(Function.identity(), treeMap::get));
            // hack to get typing correct
            return new ResistanceAPD((Resistance) game, perspective, treeSetting, treeMap2);
        }
        throw new AssertionError("Unknown game type " + game.getClass());
    }

    public AllPlayerDeterminiser(AllPlayerDeterminiser<G, P> apd) {
        root = apd.root;
        determinisationsByPlayer = apd.determinisationsByPlayer.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> (G) e.getValue().clone()));
        getMasterDeterminisation().registerListener(this);
        // TODO: Need to instantiate a new TreeTracker that inherits from the old one, and tracks the cloned game
        // currently no such method exists
        if (inRolloutMode)
            throw new AssertionError("Should never need to clone an APD when in rollout mode");
    }

    /*
    rootGame is not re-determinised. We simply generate a determinisation for every other player
    that is compatible with the IS of the rootPlayer (i.e. it is a state the rootPlayer believes they could believe)
     */
    public AllPlayerDeterminiser(G rootGame, int rootPlayerID, String treeSetting, Map<Integer, MonteCarloTree<P>> treeMap) {
        root = rootPlayerID;
        determinisationsByPlayer = new HashMap<>();
        determinisationsByPlayer.put(rootPlayerID, rootGame);

        int playerCount = rootGame.getPlayerCount();

        for (int i = 1; i <= playerCount; i++) {
            if (i != rootPlayerID)
                determinisationsByPlayer.put(i, determiniseFromRoot(i));
        }
        getMasterDeterminisation().registerListener(this);
        treeTracker = new OpenLoopTreeTracker<>(treeSetting, treeMap, rootGame);
        // this assumes we always start at the rootNode of the tree, i.e. that rootGame has just been instantiated
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
        checkRolloutMode(actionWithRef.agentRef);
        for (int player : determinisationsByPlayer.keySet()) {
            if (inRolloutMode && player != root)
                continue;
            G determinisation = getDeterminisationFor(player);
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
        checkRolloutMode(action.getActor().getActorRef());
        determinisationsByPlayer.keySet().forEach(p -> {
            if (!inRolloutMode || p == root) {
                G g = getDeterminisationFor(p);
                Action<P> actionCopy = g.getCurrentPlayer() == action.getActor() ? action : action.getType().getAction(g.getCurrentPlayer());
                g.applyAction(actionCopy);
            }
        });
    }

    private void checkRolloutMode(int player) {
        if (!inRolloutMode && treeTracker.hasLeftTree(player)) {
            IntStream.rangeClosed(1, getPlayerCount())
                    .forEach( p-> getMasterDeterminisation().getPlayer(p).setDecider(getPlayer(p).getDecider())
            );
            inRolloutMode = true;
        }
    }

    /*
    This determines if an action is compatible with all of the determinisations
    It returns a List of the player ids for which the action is incompatible, and hence need branching
    An empty list as the returned value means we can continue this game without branching
     */
    public List<Integer> getIncompatible(ActionWithRef<P> actionWithRef) {
        if (inRolloutMode) return new ArrayList<>();
        return determinisationsByPlayer.keySet().stream()
                .filter(i -> !isValid(actionWithRef, i) || !isCompatible(actionWithRef, i))
                .collect(Collectors.toList());
    }

    public G getDeterminisationFor(int playerID) {
        G retValue = inRolloutMode ? getMasterDeterminisation() : determinisationsByPlayer.get(playerID);
        if (retValue == null)
            throw new AssertionError(playerID + " does not have a determinisation");
        return retValue;
    }

    public G getMasterDeterminisation() {
        return determinisationsByPlayer.get(root);
    }

    @Override
    public P getCurrentPlayer() {
        int playerRef = getMasterDeterminisation().getCurrentPlayerRef();
        return getDeterminisationFor(playerRef).getCurrentPlayer();
    }

    @Override
    public int getCurrentPlayerRef() {
        return getMasterDeterminisation().getCurrentPlayerRef();
    }

    @Override
    public List<P> getAllPlayers() {
        List<P> retValue = IntStream.rangeClosed(1, getPlayerCount())
                .mapToObj(i -> getDeterminisationFor(i).getPlayer(i))
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
        return getDeterminisationFor(n).getPlayer(n);
    }

    @Override
    public List<ActionEnum<P>> getPossibleActions() {
        int activePlayer = getMasterDeterminisation().getCurrentPlayerRef();
        return getDeterminisationFor(activePlayer).getPossibleActions();
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
        sendMessage(event);
    }

}
