package hopshackle.simulation;

import java.util.List;

public interface Decider<A extends Agent> {

	public double valueOption(ActionEnum<A> option, A decidingAgent, Agent contextAgent);

	public void learnFrom(ExperienceRecord<A> exp, double maxResult);
	
	public void learnFromBatch(ExperienceRecord<A>[] exp, double maxResult);

	public Action<A> decide(A decidingAgent);

	public Action<A> decide(A decidingAgent, Agent contextAgent);

	public double[] getCurrentState(A decidingAgent, Agent contextAgent);

	public void setTeacher(Teacher<A> teacher);

	public Teacher<A> getTeacher();

	public List<ActionEnum<A>> getChooseableOptions(A a, Agent a2);

	public Decider<A> crossWith(Decider<A> decider);
	
	public void setName(String name);

	public List<? extends GeneticVariable> getVariables();

	public List<? extends ActionEnum<A>> getActions();

	public ActionEnum<A> decideWithoutLearning(A decidingAgent, Agent contextAgent);

	public ActionEnum<A> getOptimalDecision(A decidingAgent, Agent contextAgent);
}
