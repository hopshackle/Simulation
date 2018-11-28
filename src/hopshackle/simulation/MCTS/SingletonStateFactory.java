package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;

import java.util.*;

public class SingletonStateFactory<A extends Agent> implements StateFactory<A> {

    Map<Integer, State<A>> singletonStates = new HashMap();

    @Override
    public State<A> getCurrentState(A agent) {
        if (!singletonStates.containsKey(agent.getActorRef())) {

            int actorRef = agent.getActorRef();

            State<A> singleton = new State<A>() {
                @Override
                public double[] getAsArray() {
                    return new double[0];
                }

                @Override
                public int getActorRef() {
                    return actorRef;
                }

                @Override
                public double[] getScore() {
                    return new double[0];
                }

                @Override
                public String getAsString() {
                    return "SingletonState";
                }

                @Override
                public State<A> apply(ActionEnum<A> proposedAction) {
                    return this;
                }

                @Override
                public State<A> clone() {
                    return this;
                }
            };

            singletonStates.put(actorRef, singleton);
        }
        return singletonStates.get(agent.getActorRef());
    }

    @Override
    public <V extends GeneticVariable<A>> List<V> getVariables() {
        return new ArrayList<>();
    }

    @Override
    public StateFactory<A> cloneWithNewVariables(List<GeneticVariable<A>> newVar) {
        return this;
    }
}
