package hopshackle.simulation;

import java.util.List;

public interface Decider<A extends Agent> {

    public void injectProperties(DeciderProperties decProp);

    public DeciderProperties getProperties();

    public double valueOption(ActionEnum<A> option, A decidingAgent);

    public abstract double valueOption(ActionEnum<A> option, State<A> state);

    public List<Double> valueOptions(List<ActionEnum<A>> options, A decidingAgent);

    public List<Double> valueOptions(List<ActionEnum<A>> options, State<A> state);

    public void learnFrom(ExperienceRecord<A> exp, double maxResult);

    public void learnFromBatch(List<ExperienceRecord<A>> exp, double maxResult);

    public void learnFromBatch(ExperienceRecord<A>[] exp, double maxResult);

    public State<A> getCurrentState(A agent);

    public Action<A> decide(A decidingAgent, List<ActionEnum<A>> possibleActions);

    public ActionEnum<A> makeDecision(A decidingAgent, List<ActionEnum<A>> possibleActions);

    public Decider<A> mutate(double intensity);

    public Decider<A> crossWith(Decider<A> decider);

    public void setName(String name);

    public <V extends GeneticVariable<A>> List<V> getVariables();

    public void log(String s);

    public void flushLog();

}
