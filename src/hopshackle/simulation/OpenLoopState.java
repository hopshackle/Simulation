package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by james on 15/06/2017.
 */
public class OpenLoopState<A extends Agent> implements State<A> {

    private static AtomicLong idFountain = new AtomicLong(1);
    private long id;
    private int actingAgentNumber;
    private double[] score;
    private OpenLoopStateFactory<A> factory;

    public OpenLoopState(A agent, OpenLoopStateFactory<A> olsf) {
        factory = olsf;
        if (agent == null) {
            score = new double[1];
            id = 0;
            return;
        }
        if (agent.getGame() != null) {
            actingAgentNumber = agent.getGame().getPlayerNumber(agent)-1;
            List<Agent> players = agent.getGame().getAllPlayers();
            score = new double[players.size()];
            for (int i = 0; i < players.size(); i++){
                score[i] = agent.getScore();
            }
        } else {
            actingAgentNumber = 0;
            score = new double[1];
            score[0] = agent.getScore();
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
        return actingAgentNumber;
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
        if (factory != null) {
            return factory.getNextState(this, proposedAction);
        }
        return this;
    }

    public State<A> clone() {
        return this;
    }
}
