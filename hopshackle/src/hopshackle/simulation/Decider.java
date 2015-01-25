package hopshackle.simulation;

import java.util.List;

public interface Decider {

	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent);

	public void learnFrom(ExperienceRecord exp, double maxResult);
	
	public void learnFromBatch(ExperienceRecord[] exp, double maxResult);

	public ActionEnum decide(Agent decidingAgent);

	public ActionEnum decide(Agent decidingAgent, Agent contextAgent);

	public double[] getCurrentState(Agent decidingAgent, Agent contextAgent);

	public void setTeacher(Teacher teacher);

	public Teacher getTeacher();

	public List<ActionEnum> getChooseableOptions(Agent a, Agent a2);

	public Decider crossWith(Decider decider);
	
	public void setName(String name);

	public List<? extends GeneticVariable> getVariables();

	public List<? extends ActionEnum> getActions();

	public ActionEnum decideWithoutLearning(Agent decidingAgent, Agent contextAgent);

	public ActionEnum getOptimalDecision(Agent decidingAgent, Agent contextAgent);
}
