package hopshackle.simulation.MCTS;

import hopshackle.simulation.*;

import java.util.*;

public class SingletonStateFactory<A extends Agent> implements StateFactory<A> {

    State<A> singletonState;

    @Override
    public State<A> getCurrentState(A agent) {
        if (singletonState == null) {


            singletonState = new State<A>() {
                @Override
                public double[] getAsArray() {
                    return new double[0];
                }

                @Override
                public int getActorRef() {
                    throw new AssertionError("Not implemented");
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

        }
        return singletonState;
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
