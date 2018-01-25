package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by james on 15/06/2017.
 */
public class OpenLoopState<A extends Agent> implements State<A> {

    private static AtomicLong idFountain = new AtomicLong(1);
    private long id;
    private double[] score;
    private OpenLoopStateFactory<A> factory;

    /*
    perspectiveAgent is the agent from whose perspective the state has been created
    actingAgent is the agent that is acting
    These are different when we have a single set of game level states.
     */
    public OpenLoopState(A perspectiveAgent, OpenLoopStateFactory<A> olsf) {
        factory = olsf;
        if (perspectiveAgent == null) {
            score = new double[1];
            id = 0;
            return;
        }
        if (perspectiveAgent.getGame() != null) {
            Game<A, ?> game = perspectiveAgent.getGame();
            List<A> players = game.getMasters();    // we only have a score for each master player
            score = new double[players.size()];
            for (int i = 0; i < players.size(); i++) {
                score[i] = players.get(i).getScore();
            }
        } else {
            score = new double[1];
            score[0] = perspectiveAgent.getScore();
        }
        id = idFountain.getAndAdd(1);
    }

    public OpenLoopState(OpenLoopState<A> from) {
        factory = from.factory;
        score = from.score.clone();
        id = idFountain.getAndAdd(1);
    }

    public double[] getAsArray() {
        return new double[1];
    }

    /*
     * Returns the index of the perspective/acting agent. Used primarily to access the associated score
     */
    public int getActorRef(){
        return -1;
    }

    /*
     * Returns the score of all associated agents in the state (at the moment of creation)
     */
    public double[] getScore(){
        return score;
    }

    @Override
    public String toString() {
        return getAsString();
    }

    public String getAsString() {
        return String.valueOf(id);
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OpenLoopState) {
            return ((OpenLoopState) o).id == id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    public State<A> apply(ActionEnum<A> proposedAction) {
        return this;
    }

    public State<A> clone() {
        return this;
    }
}
