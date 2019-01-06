package hopshackle.simulation.games;

import hopshackle.simulation.*;
import org.javatuples.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GameTracker<A extends Agent> implements GameListener<A> {

    private A perspectiveActor;
    private int perspectiveRef;
    protected List<Triplet<State<A>, ActionWithRef<A>, Long>> trajectory = new ArrayList();

    public GameTracker(Game game) {
        perspectiveRef = -1;
        game.registerListener(this);
    }

    public GameTracker(A agent, Game game) {
        if (agent == null)
            throw new AssertionError("Must provide a perspective agent");
        perspectiveActor = agent;
        perspectiveRef = agent.getActorRef();
        game.registerListener(this);
    }

    private boolean isEventRelevant(GameEvent<A> event) {
        switch (event.type) {
            case GAME_OVER:
                return false;
            case MOVE:
                if (perspectiveRef == -1)
                    return true;
                return event.visibleTo().contains(perspectiveRef);
        }
        return false;
    }

    @Override
    public void processGameEvent(GameEvent<A> event) {
        if (isEventRelevant(event)) {
            // a move that is visible to us
            State<A> currentState = perspectiveRef == -1 ? null : (State<A>) perspectiveActor.getDecider().getCurrentState(perspectiveActor);
            trajectory.add(new Triplet<>(currentState, event.actionTaken, event.time));
        }
    }

    public List<Triplet<State<A>, ActionWithRef<A>, Long>> getTrajectory() {
        return getFilteredTrajectory(i -> true);
    }

    public List<Triplet<State<A>, ActionWithRef<A>, Long>> getFilteredTrajectory(Predicate<Triplet<State<A>, ActionWithRef<A>, Long>> filter) {
        return trajectory.stream().filter(filter).collect(Collectors.toList());
    }
}
