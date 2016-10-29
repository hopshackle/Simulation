package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;
import hopshackle.simulation.ExperienceRecord.ERState;

import java.util.*;

class TestAction extends Action<TestAgent> {
	
	boolean dieInMiddle = false;
	boolean makeNextDecision = true;
	int waitTime;
	
	public TestAction(TestActionEnum action, List<TestAgent> mandatory, List<TestAgent> optional, long startOffset, long duration, boolean recordAction, boolean makeNext, int wait) {
		super(action, mandatory, optional, startOffset, duration, recordAction);
		makeNextDecision = makeNext;
		waitTime = wait;
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
		switch ((TestActionEnum)actionType) {
		case LEFT:
			actor.position++;
			break;
		case RIGHT:
			actor.position--;
		default:
		}
	}
	@Override
	protected void doNextDecision(TestAgent a) {
		if (makeNextDecision) {
			super.doNextDecision(a);
		} else {
			// Do Nothing
		}
	}
	private void waitABit() {
		if (waitTime > 0) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public String toString() {
		return actionType + " (" + getStartTime() + "-" + getEndTime() + ")";
	}
}

enum TestActionEnum implements ActionEnum<TestAgent> {

	TEST,
	LEFT,
	RIGHT;
	
	public static boolean dummyMode = false;
	public static boolean defaultMakeNextDecision = true;
	public static int waitTime = 200;
	
	@Override
	public String getChromosomeDesc() {
		return "TEST";
	}

	@Override
	public Action<TestAgent> getAction(TestAgent a) {
		List<TestAgent> thisAgentAsList = new ArrayList<TestAgent>();
		thisAgentAsList.add(a);
		return new TestAction(this, thisAgentAsList, new ArrayList<TestAgent>(), 0, 1000, true, defaultMakeNextDecision, waitTime);
	}

	@Override
	public boolean isChooseable(TestAgent a) {
		return true;
	}

	@Override
	public Enum<TestActionEnum> getEnum() {
		return this;
	}
}

enum TestGenVar implements GeneticVariable<TestAgent> {
	CONSTANT,
	GOLD,
	POSITION,
	AGE;

	@Override
	public double getValue(TestAgent a1) {
		switch(this) {
		case CONSTANT:
			return 1.0;
		case GOLD:
			return a1.getGold();
		case POSITION:
			return a1.position;
		case AGE:
			return a1.getAge() / 1000.0;
		}
		return 1.0;}
	@Override
	public String getDescriptor() {
		return "TEST_GV";
	}
	@Override
	public boolean unitaryRange() {return true;}
	
}

class TestAgent extends Agent {
	
	int decisionsTaken = 0;
	int position = 0;
	
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
	@Override
	public double getScore() {
		if (game != null) {
			return ((SimpleMazeGame) game).reward[0];
		}
		return 0.0;
	}
}

class TestDecider extends BaseDecider<TestAgent> {
	
	public int learningEpisodes = 0;
	
	public static List<ActionEnum<TestAgent>> actionList = new ArrayList<ActionEnum<TestAgent>>();
	public static List<GeneticVariable<TestAgent>> gvList = new ArrayList<GeneticVariable<TestAgent>>();
	static {
		actionList.add(TestActionEnum.TEST);
		gvList.add(TestGenVar.CONSTANT);
	}

	public TestDecider() {
		super(new LinearStateFactory<TestAgent>(gvList), actionList);
	}

	@Override
	public double valueOption(ActionEnum<TestAgent> option, TestAgent decidingAgent) {
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
		return new TestAction(TestActionEnum.TEST, mandatoryAgents, optionalAgents, offset, duration, true, true, 200);
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

	public TestERCollector() {
		super(new StandardERFactory<TestAgent>());
	}

	public boolean agentKnown(TestAgent testAgent) {
		return agentAlreadySeen(testAgent);
	}

	public ERState agentActionState(TestAgent testAgent, Action<TestAgent> actionTaken) {
		List<ExperienceRecord<TestAgent>> allER = getExperienceRecords(testAgent);
		for (ExperienceRecord<TestAgent> er : allER) {
			if (er.getActionTaken().equals(actionTaken)) {
				return er.getState();
			}
		}
		return ERState.UNSEEN;
	}
}

class TestTeacher extends Teacher<TestAgent> {
	
	public List<AgentEvent> eventsReceived = new ArrayList<AgentEvent>();

	@Override
	public void processEvent(AgentEvent event) {
		eventsReceived.add(event);
	}
	
}
