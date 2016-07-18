package hopshackle.simulation;

import java.util.List;

public interface Decider<A extends Agent> {

	public double valueOption(ActionEnum<A> option, A decidingAgent);
	
	public void setStateFactory(StateFactory<A> stateFactory);

	public <S extends State<A>> void learnFrom(ExperienceRecord<A, S> exp, double maxResult);

	public <S extends State<A>> void learnFromBatch(List<ExperienceRecord<A, S>> exp, double maxResult);
	
	public <S extends State<A>> void learnFromBatch(ExperienceRecord<A, S>[] exp, double maxResult);

	public Action<A> decide(A decidingAgent);

	public List<ActionEnum<A>> getChooseableOptions(A a);

	public Decider<A> crossWith(Decider<A> decider);
	
	public void setName(String name);

	public <V extends GeneticVariable<A>> List<V> getVariables();

	public <V extends ActionEnum<A>> List<V> getActions();

	public ActionEnum<A> decideWithoutLearning(A decidingAgent);

	public ActionEnum<A> getOptimalDecision(A decidingAgent);

	public ActionEnum<A> makeDecision(A decidingAgent);
}
