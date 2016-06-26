package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;
import hopshackle.simulation.ExperienceRecord.State;
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
	
	public static boolean dummyMode = false;
	
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

enum TestGenVar implements GeneticVariable {
	TEST;

	@Override
	public double getValue(Object a, double var) {return 1.0;}

	@Override
	public double getValue(Object a1, Object a2) {return 1.0;}

	@Override
	public String getDescriptor() {
		return "TEST_GV";
	}

	@Override
	public boolean unitaryRange() {return true;}
	
}

class TestAgent extends Agent {
	
	int decisionsTaken = 0;
	
	public TestAgent(World world) {
		super(world);
		setDecider(new TestDecider());
	}
	@Override
	public void decide() {
		decisionsTaken++;
		super.decide();
	}
	@Override
	public void eventDispatch(AgentEvent ae) {
		super.eventDispatch(ae);
	}
}

class TestDecider extends BaseDecider<TestAgent> {
	
	public int learningEpisodes = 0;
	
	public static List<ActionEnum<TestAgent>> actionList = new ArrayList<ActionEnum<TestAgent>>();
	public static List<GeneticVariable> gvList = new ArrayList<GeneticVariable>();
	static {
		actionList.add(TestActionEnum.TEST);
		gvList.add(TestGenVar.TEST);
	}

	public TestDecider() {
		super(actionList, gvList);
	}

	@Override
	public double valueOption(ActionEnum<TestAgent> option, TestAgent decidingAgent, Agent contextAgent) {
		return 0;
	}
	@Override
	public void learnFrom(ExperienceRecord<TestAgent> exp, double maxResult) {
		learningEpisodes++;
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

class TestERCollector extends ExperienceRecordCollector<TestAgent> {

	public boolean agentKnown(TestAgent testAgent) {
		return agentAlreadySeen(testAgent);
	}

	public State agentActionState(TestAgent testAgent, Action<TestAgent> actionTaken) {
		List<ExperienceRecord<TestAgent>> allER = getExperienceRecords(testAgent);
		for (ExperienceRecord<TestAgent> er : allER) {
			if (er.getActionTaken().equals(actionTaken)) {
				return er.getState();
			}
		}
		return State.UNSEEN;
	}
	
}
