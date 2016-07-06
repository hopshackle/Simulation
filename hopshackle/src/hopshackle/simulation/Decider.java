package hopshackle.simulation;

import java.util.List;

public interface Decider<A extends Agent> {

	public double valueOption(ActionEnum<A> option, A decidingAgent, Agent contextAgent);

	public void learnFrom(ExperienceRecord<A> exp, double maxResult);
	
	public void learnFromBatch(ExperienceRecord<A>[] exp, double maxResult);

	public void learnFromBatch(List<ExperienceRecord<A>> exp, double maxResult);
	
	public Action<A> decide(A decidingAgent);

	public Action<A> decide(A decidingAgent, Agent contextAgent);

	public double[] getCurrentState(A decidingAgent, Agent contextAgent, Action<A> action);

	public List<ActionEnum<A>> getChooseableOptions(A a, Agent a2);

	public Decider<A> crossWith(Decider<A> decider);
	
	public void setName(String name);

	public <V extends GeneticVariable<A>> List<V> getVariables();

	public <V extends ActionEnum<A>> List<V> getActions();

	public ActionEnum<A> decideWithoutLearning(A decidingAgent, Agent contextAgent);

	public ActionEnum<A> getOptimalDecision(A decidingAgent, Agent contextAgent);

	public ActionEnum<A> makeDecision(A decidingAgent, Agent contextAgent);
}
