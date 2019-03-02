package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;
import hopshackle.simulation.games.*;
import org.javatuples.Triplet;

import java.util.*;
import java.util.function.Predicate;

public class MCTSUtilities {
    /*
This executes one iteration of search, and stores any results directly in the provided Tree
 */
    public static <A extends Agent> void launchGame(Map<Integer, MonteCarloTree<A>> treeMap,
                                                    Game<A, ActionEnum<A>> clonedGame,
                                                    Decider<A> childDecider,
                                                    Decider<A> opponentModel,
                                                    DeciderProperties prop) {
        launchGame(treeMap, clonedGame, childDecider, opponentModel, prop,
                new BackPropagationTactics(new HashMap<>(), new HashMap<>(), clonedGame.getPlayerCount()),
                new ArrayList<>());
    }

    public static <A extends Agent> void launchGame(Map<Integer, MonteCarloTree<A>> treeMap,
                                                    Game<A, ActionEnum<A>> clonedGame,
                                                    Decider<A> childDecider,
                                                    Decider<A> opponentModel,
                                                    DeciderProperties prop,
                                                    BackPropagationTactics bpTactics,
                                                    List<ActionWithRef<A>> initialActions) {
        String treeSetting = prop.getProperty("MonteCarloTree", "single");
        boolean singleTree = treeSetting.equals("single");
        boolean multiTree = treeSetting.equals("perPlayer");
        int rolloutLimit = prop.getPropertyAsInteger("MonteCarloRolloutLimit", "0");

        A clonedAgent = clonedGame.getCurrentPlayer();
        int currentPlayer = clonedAgent.getActorRef();
        MonteCarloTree<A> tree = treeMap.get(currentPlayer);

        GameTracker<A>[] gameTrackers = new GameTracker[clonedGame.getPlayerCount() + 1];
        switch (treeSetting) {
            case "perPlayer":
                for (A player : clonedGame.getAllPlayers()) {
                    gameTrackers[player.getActorRef()] = new GameTracker<>(player, clonedGame);
                }
                break;
            case "single":
                // we track the whole game trajectory
                gameTrackers[currentPlayer] = new GameTracker<>(clonedGame);
                break;
            case "ignoreOthers":
                gameTrackers[currentPlayer] = new GameTracker<>(clonedAgent, clonedGame);
                break;
            default:
                throw new AssertionError("Unknown Tree Setting: " + treeSetting);
        }

        for (A player : clonedGame.getAllPlayers()) {
            // For each other player in the game, we have to model their behaviour in some way
            if (player != clonedAgent && !singleTree && !multiTree) {
                player.setDecider(opponentModel);
            } else {
                // always use the childDecider if we are openLoop with anything other than ignoreOthers
                player.setDecider(childDecider);
            }
        }

        // Now we apply the initialActions defined by the process that launches the Game
        initialActions.forEach(clonedGame::apply);

        clonedGame.playGame(rolloutLimit);
        // gameTracker tracks all events that are visible to us

        switch (treeSetting) {
            case "perPlayer":
                treeMap.keySet().forEach(
                        p -> {
                            List<Triplet<State<A>, ActionWithRef<A>, Long>> trajectory = gameTrackers[p].getTrajectory();
                            MonteCarloTree<A> t = treeMap.get(p);
                            t.setUpdatesLeft(1);
                            t.processTrajectory(trajectory, clonedGame.getFinalScores(),
                                    bpTactics.getStartNode(p), bpTactics.getStopNode(p));
                            //                       if (t.updatesLeft == 0) nodesExpanded.incrementAndGet();
                        }
                );
                break;
            case "ignoreOthers":
            case "single":
                tree.setUpdatesLeft(1);
                Predicate<Triplet<State<A>, ActionWithRef<A>, Long>> filterFunction = (t -> true);
                // default is to use whole visible trajectory
                if (treeSetting.equals("ignoreOthers"))
                    filterFunction = (t -> t.getValue1().agentRef == currentPlayer);
                List<Triplet<State<A>, ActionWithRef<A>, Long>> trajectoryToUse = gameTrackers[currentPlayer].getFilteredTrajectory(filterFunction);
                tree.processTrajectory(trajectoryToUse, clonedGame.getFinalScores(),
                        bpTactics.getStartNode(currentPlayer), bpTactics.getStopNode(currentPlayer));
                //           if (tree.updatesLeft == 0) nodesExpanded.incrementAndGet();
                break;

            default:
                throw new AssertionError("Unknown Tree Setting: " + treeSetting);
        }
    }

}
