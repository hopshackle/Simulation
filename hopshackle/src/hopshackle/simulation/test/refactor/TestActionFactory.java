package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;

import java.util.*;

class TestAction extends Action {
	public TestAction(List<Agent> mandatory, List<Agent> optional, long startOffset, long duration, boolean recordAction) {
		super(mandatory, optional, startOffset, duration, recordAction);
	}
	@Override
	public void initialisation() {
		waitABit();
	}
	@Override 
	public void doStuff() {
		waitABit();
	}
	private void waitABit() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	@Override
	public String toString() {
		return "TEST(" + getStartTime() + "-" + getEndTime() + ")";
	}
}

class TestActionEnum implements ActionEnum {

	@Override
	public String getChromosomeDesc() {
		return "TEST";
	}

	@Override
	public Action getAction(Agent a) {
		List<Agent> thisAgentAsList = new ArrayList<Agent>();
		thisAgentAsList.add(a);
		return new TestAction(thisAgentAsList, new ArrayList<Agent>(), 0, 1000, true);
	}

	@Override
	public Action getAction(Agent a1, Agent a2) {
		return getAction(a1);
	}

	@Override
	public boolean isChooseable(Agent a) {
		return true;
	}

	@Override
	public Enum getEnum() {
		return null;
	}
	
}

class TestAgent extends Agent {
	
	int decisionsTaken = 0;
	
	public TestAgent(World world) {
		super(world);
		setDecider(new TestDecider());
	}
	@Override
	public Action decide() {
		decisionsTaken++;
		return super.decide();
	}
}

class TestDecider extends BaseDecider {
	
	static List<ActionEnum> actionList = new ArrayList<ActionEnum>();
	static {
		actionList.add(new TestActionEnum());
	}

	public TestDecider() {
		super(actionList, new ArrayList<GeneticVariable>());
	}

	@Override
	public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
		return 0;
	}

}

class TestActionFactory {
	
	List<TestAgent> allAgents;
	public TestActionFactory(List<TestAgent> allAgents) {
		this.allAgents = allAgents;
	}
	public TestAction factory(int mandatory, int optional, long offset, long duration) {
		List<Agent> mandatoryAgents = new ArrayList<Agent>();
		List<Agent> optionalAgents = new ArrayList<Agent>();
		for (int i = 0; i < mandatory; i++) {
			mandatoryAgents.add(allAgents.get(i));
		}
		for (int i = mandatory; i < mandatory + optional; i++) {
			optionalAgents.add(allAgents.get(i));
		}
		return new TestAction(mandatoryAgents, optionalAgents, offset, duration, true);
	}
}

class TestActionPolicy extends Policy<Action> {
	Map<Action, Double> actionValues = new HashMap<Action, Double>();
	public TestActionPolicy(String name) {
		super(name);
	}
	@Override
	public double getValue(Action a, Agent p) {
		return actionValues.getOrDefault(a, 0.0);
	}
	public void setValue(Action a, double value) {
		actionValues.put(a, value);
	}
	
}
