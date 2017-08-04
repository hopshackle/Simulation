package hopshackle.simulation;

import java.util.*;

/**
 * Created by james on 15/06/2017.
 */
public class OpenLoopStateFactory<A extends Agent> implements StateFactory<A>, AgentListener {

    private Map<A, OpenLoopState<A>> agentToState = new HashMap<>();
    private Map<OpenLoopState<A>, Map<ActionEnum<A>, OpenLoopState<A>>> tree = new HashMap<>();
    private Map<A, Integer> updatesLeft = new HashMap<>();
    private int updatesPerAgent = SimProperties.getPropertyAsInteger("OpenLoopUpdateLimit", "0");
    private boolean gameLevel = false;

    private OpenLoopStateFactory() {
        // private to stop this being called outside a factory method
    }

    public static <B extends Agent> OpenLoopStateFactory<B> newInstance() {
        return new OpenLoopStateFactory<B>();
    }

    public static <B extends Agent> OpenLoopStateFactory<B> newInstanceGameLevelStates() {
        OpenLoopStateFactory<B> retValue = new OpenLoopStateFactory<B>();
        retValue.gameLevel = true;
        return retValue;
    }

    @Override
    public State<A> getCurrentState(A agent) {
        A perspectiveAgent = agent;
        if (gameLevel) {
            perspectiveAgent = (A) agent.getGame().getPlayer(1);
            // always key to the first player if we track by game
            // but store the agent as actingAgent
        }
        if (!agentToState.keySet().contains(perspectiveAgent)) {
            OpenLoopState<A> retValue = new OpenLoopState<A>(perspectiveAgent, this);
            if (perspectiveAgent.isDead()) {
                return retValue;
                // if dead, then we do not want to keep tracking the agent
            }
            setState(perspectiveAgent, retValue);
        }
        return agentToState.get(perspectiveAgent);
    }

    public <V extends GeneticVariable<A>> List<V> getVariables() {
        return new ArrayList<>();
    }

    public StateFactory<A> cloneWithNewVariables(List<GeneticVariable<A>> newVar) {
        return this;
    }

    public void processEvent(AgentEvent event) {
        if (event.getEvent() == AgentEvent.Type.ACTION_EXECUTED) {
            if (event.getAction().hasNoAssociatedDecision()) return;
            A perspectiveAgent = (A) event.getAgent();
            if (perspectiveAgent.isDead()) {
                throw new AssertionError("Should be alive");
            }
            if (gameLevel) perspectiveAgent = (A) perspectiveAgent.getGame().getPlayer(1);
            if (updatesPerAgent > 0 && updatesLeft.getOrDefault(perspectiveAgent, updatesPerAgent) <= 0) {
                return;
            }
            ActionEnum<A> actionTaken = event.getAction().getType();
            OpenLoopState<A> currentState = (OpenLoopState<A>) getCurrentState(perspectiveAgent);
            if (currentState == null)
                throw new AssertionError("Current state should never be null");
            Map<ActionEnum<A>, OpenLoopState<A>> successors = tree.getOrDefault(currentState, new HashMap<>());
            if (successors.isEmpty())
                tree.put(currentState, successors);
            OpenLoopState<A> successorState = successors.getOrDefault(actionTaken, null);
            if (successorState == null) {
                // need to create a new state
                successorState = new OpenLoopState<A>(perspectiveAgent, this);
                addLink(currentState, actionTaken, successorState);
                if (updatesPerAgent > 0) {
                    updatesLeft.put(perspectiveAgent, updatesLeft.getOrDefault(perspectiveAgent, updatesPerAgent) - 1);
                }
            }
            agentToState.put(perspectiveAgent, successorState);

        } else if (event.getEvent() == AgentEvent.Type.DEATH) {
            agentToState.remove(event.getAgent());
            updatesLeft.remove(event.getAgent());
        }

    }

    public OpenLoopState<A> getNextState(OpenLoopState<A> startState, ActionEnum<A> actionTaken) {
        Map<ActionEnum<A>, OpenLoopState<A>> fromMap = tree.getOrDefault(startState, new HashMap<>());
        if (fromMap.containsKey(actionTaken))
            return fromMap.get(actionTaken);
        return startState;
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
            setState(newPlayer, agentToState.get(player));
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

    public OpenLoopState<A> getState(int id) {
        for (OpenLoopState s : tree.keySet()) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    public void setState(A agent, OpenLoopState<A> state) {
        if (!agentToState.containsKey(agent)) {
            if (gameLevel) {
                for (Agent player : agent.getGame().getAllPlayers()) {
                    player.addListener(this);
                }
            } else {
                agent.addListener(this);
            }
        }
        if (!tree.containsKey(state))
            tree.put(state, new HashMap<>());
        if (updatesPerAgent > 0)
            updatesLeft.put(agent, updatesPerAgent);
        agentToState.put(agent, state);
    }
}
