package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;

import java.util.*;

class TestAction extends Action<TestAgent> {
	boolean dieInMiddle = false;
	public TestAction(List<TestAgent> mandatory, List<TestAgent> optional, long startOffset, long duration, boolean recordAction) {
		super(TestActionEnum.TEST, mandatory, optional, startOffset, duration, recordAction);
	}
	
	@Override
	public void initialisation() {
		waitABit();
	}
	@Override 
	public void doStuff() {
		waitABit();
		if (dieInMiddle) {
			for (Agent a : getAllConfirmedParticipants()) {
				a.die("Oops");
			}
		}
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

enum TestActionEnum implements ActionEnum<TestAgent> {
	
	TEST;

	@Override
	public String getChromosomeDesc() {
		return "TEST";
	}

	@Override
	public Action<TestAgent> getAction(TestAgent a) {
		List<TestAgent> thisAgentAsList = new ArrayList<TestAgent>();
		thisAgentAsList.add(a);
		return new TestAction(thisAgentAsList, new ArrayList<TestAgent>(), 0, 1000, true);
	}

	@Override
	public Action<TestAgent> getAction(TestAgent a1, Agent a2) {
		return getAction(a1);
	}

	@Override
	public boolean isChooseable(TestAgent a) {
		return true;
	}

	@Override
	public Enum getEnum() {
		return this;
	}
	
}

class TestAgent extends Agent {
	
	int decisionsTaken = 0;
	
	public TestAgent(World world) {
		super(world);
		setDecider(new TestDecider());
	}
	@Override
	public Action<?> decide() {
		decisionsTaken++;
		return super.decide();
	}
}

class TestDecider extends BaseDecider<TestAgent> {
	
	static List<ActionEnum<TestAgent>> actionList = new ArrayList<ActionEnum<TestAgent>>();
	static {
		actionList.add(TestActionEnum.TEST);
	}

	public TestDecider() {
		super(actionList, new ArrayList<GeneticVariable>());
	}

	@Override
	public double valueOption(ActionEnum<TestAgent> option, TestAgent decidingAgent, Agent contextAgent) {
		return 0;
	}

}

class TestActionFactory {
	
	List<TestAgent> allAgents;
	public TestActionFactory(List<TestAgent> allAgents) {
		this.allAgents = allAgents;
	}
	public TestAction factory(int mandatory, int optional, long offset, long duration) {
		List<TestAgent> mandatoryAgents = new ArrayList<TestAgent>();
		List<TestAgent> optionalAgents = new ArrayList<TestAgent>();
		for (int i = 0; i < mandatory; i++) {
			mandatoryAgents.add(allAgents.get(i));
		}
		for (int i = mandatory; i < mandatory + optional; i++) {
			optionalAgents.add(allAgents.get(i));
		}
		return new TestAction(mandatoryAgents, optionalAgents, offset, duration, true);
	}
}

class TestActionPolicy extends Policy<TestAction> {
	Map<TestAction, Double> actionValues = new HashMap<TestAction, Double>();
	public TestActionPolicy(String name) {
		super(name);
	}
	@Override
	public double getValue(TestAction a, Agent p) {
		return actionValues.getOrDefault(a, 0.0);
	}
	public void setValue(TestAction a, double value) {
		actionValues.put(a, value);
	}
	
}
