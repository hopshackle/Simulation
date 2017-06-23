package hopshackle.simulation;

import java.util.*;

/**
 * Created by james on 15/06/2017.
 */
public class OpenLoopStateFactory<A extends Agent> implements StateFactory<A>, AgentListener {

    private Map<A, OpenLoopState<A>> agentToState = new HashMap<>();
    private Map<OpenLoopState<A>, Map<ActionEnum<A>, OpenLoopState<A>>> tree = new HashMap<>();

    @Override
    public State<A> getCurrentState(A agent) {
        if (!agentToState.keySet().contains(agent)) {
            if (agent.isDead())
                throw new AssertionError("Should be alove");
            agent.addListener(this);
            OpenLoopState<A> retValue = new OpenLoopState<A>(agent, this);
            if (!agentToState.containsKey(agent)) System.out.println("Added GCS " + agent);
            agentToState.put(agent, retValue);
            tree.put(retValue, new HashMap<>());
        }
        return agentToState.get(agent);
    }

    public <V extends GeneticVariable<A>> List<V> getVariables() {
        return new ArrayList<>();
    }

    public StateFactory<A> cloneWithNewVariables(List<GeneticVariable<A>> newVar) {
        return this;
    }

    public void processEvent(AgentEvent event) {
        if (event.getEvent() == AgentEvent.Type.ACTION_EXECUTED) {
            A agent = (A) event.getAgent();
            if (agent.isDead()) {
                throw new AssertionError("Should be alive");
            }
            ActionEnum<A> actionTaken = event.getAction().getType();
            OpenLoopState<A> currentState = (OpenLoopState<A>) getCurrentState(agent);
            if (currentState == null)
                throw new AssertionError("Current state should never be null");
            Map<ActionEnum<A>, OpenLoopState<A>> successors = tree.getOrDefault(currentState, new HashMap<>());
            if (successors.isEmpty())
                tree.put(currentState, successors);
            OpenLoopState<A> successorState = successors.getOrDefault(actionTaken, null);
            if (successorState == null) {
                // need to create a new state
                successorState = new OpenLoopState<A>(agent, this);
                addLink(currentState, actionTaken, successorState);
            }
            if (!agentToState.containsKey(agent)) System.out.println("Added " + event.getAgent());
            agentToState.put(agent, successorState);

        } else if (event.getEvent() == AgentEvent.Type.DEATH) {
            agentToState.remove(event.getAgent());
            System.out.println("Removed " + event.getAgent());
        }
    }

    public OpenLoopState<A> getNextState(OpenLoopState<A> startState, ActionEnum<A> actionTaken) {
        Map<ActionEnum<A>, OpenLoopState<A>> fromMap = tree.getOrDefault(startState, new HashMap<>());
        if (fromMap.containsKey(actionTaken))
            return fromMap.get(actionTaken);
        OpenLoopState<A> retValue = new OpenLoopState<A>(startState);
        addLink(startState, actionTaken, retValue);
        return retValue;
    }

    private void addLink(OpenLoopState<A> from, ActionEnum<A> action, OpenLoopState<A> to) {
        Map<ActionEnum<A>, OpenLoopState<A>> fromMap = tree.get(from);
        fromMap.put(action, to);
        if (!tree.containsKey(to))
            tree.put(to, new HashMap<>());
    }

    public void cloneGame(Game<A, ActionEnum<A>> oldGame, Game<A, ActionEnum<A>> newGame) {
        // we update all players in newGame to be at the same state as their counterparts in oldGame
        for (A player : oldGame.getAllPlayers()) {
            if (!agentToState.containsKey(player))
                continue;   // ignore any players who weren't being tracked
            int oldPlayerNumber = oldGame.getPlayerNumber(player);
            A newPlayer = newGame.getPlayer(oldPlayerNumber);
            if (newPlayer.isDead())
                throw new AssertionError("Should be alive");
            if (!agentToState.containsKey(newPlayer)) System.out.println("Added " + newPlayer);
            agentToState.put(newPlayer, agentToState.get(player));
            newPlayer.addListener(this);
        }
    }

    public void prune() {
        // the assumption here is that the only agents we need to track are those in agentToState
        // We start a new tree, and add states to it by descending the tree for each active agent
        Map<OpenLoopState<A>, Map<ActionEnum<A>, OpenLoopState<A>>> newTree = new HashMap<>();
        Deque<OpenLoopState<A>> currentStates = new LinkedList<>();
        currentStates.addAll(agentToState.values());
        while (!currentStates.isEmpty()) {
            OpenLoopState<A> from = currentStates.poll();
            Map<ActionEnum<A>, OpenLoopState<A>> currentMap = tree.getOrDefault(from, new HashMap<>());
            newTree.put(from, currentMap);
            currentStates.addAll(currentMap.values());
        }
        tree = newTree;
    }

    public boolean containsState(OpenLoopState<A> state) {
        return tree.containsKey(state);
    }
}
