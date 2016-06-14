package hopshackle.simulation.test;

import static org.junit.Assert.*;

import java.util.*;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class BasicMarriageTest {

	private World world;
	private BasicHex homeHex;
	private BasicAgent maleAgent1, maleAgent2, maleAgent3;
	private BasicAgent femaleAgent1, femaleAgent2, femaleAgent3;
	private TestActionProcessor ap;
	private AgentTeacher<BasicAgent> teacher = new AgentTeacher<BasicAgent>();
	private List<ActionEnum<BasicAgent>> actions;
	private List<GeneticVariable> variables = new ArrayList<GeneticVariable>(EnumSet.allOf(BasicVariables.class));
	private Decider<BasicAgent> baseDecider;
	private Decider<BasicAgent> restDecider = new HardCodedDecider(BasicActions.REST);

	@Before
	public void setUp() {
		actions = new ArrayList<ActionEnum<BasicAgent>>();
		actions.add(BasicActions.REST);
		actions.add(BasicActions.FORAGE);
		actions.add(BasicActions.MARRY);
		baseDecider = new GeneralLinearQDecider<BasicAgent>(actions, variables);
		ap = new TestActionProcessor();
		world = ap.w;
		homeHex = new BasicHex(0, 0);
		homeHex.setParentLocation(world);
		maleAgent1 = createAgent(50, 20, true);
		maleAgent2 = createAgent(40, 20, true);
		maleAgent3 = createAgent(50, 20, true);
		femaleAgent1 = createAgent(55, 20, false);
		femaleAgent2 = createAgent(45, 20, false);
		femaleAgent3 = createAgent(47, 20, false);
		baseDecider.setTeacher(teacher);
		restDecider.setTeacher(teacher);
	}

	private BasicAgent createAgent(int age, double health, boolean isMale) {
		BasicAgent retAgent = new BasicAgent(world);
		retAgent.setLocation(homeHex);
		retAgent.setDecider(restDecider);
		retAgent.setMale(isMale);
		retAgent.setAge(age*1000);
		assertEquals(retAgent.getAge(), age*1000);
		retAgent.addHealth(health - BasicAgent.FULL_HEALTH);
		assertEquals(retAgent.getHealth(), health, 0.0001);
		return retAgent;
	}

	@Test
	public void marriageStatusIsDetectable() {
		assertFalse(maleAgent1.isMarried());
		new Marriage(maleAgent1, femaleAgent2);
		assertTrue(maleAgent1.isMarried());
		assertTrue(femaleAgent2.isMarried());
	}
	
	@Test
	public void marriageMustBeBetweenDifferentGenders() {
		try {
			new Marriage(maleAgent3, maleAgent1);
			assertTrue(false);
		} catch (AssertionError e) {
			assertTrue(true);
		}
	}
	
	@Test
	public void maleIsAlwaysSeniorPartner() {
		Marriage m = new Marriage(maleAgent1, femaleAgent2);
		assertTrue(m.getSeniorPartner() == maleAgent1);
		m = new Marriage(femaleAgent1, maleAgent2);
		assertTrue(m.getSeniorPartner() == maleAgent2);
	}

	@Test
	public void cannotMarryUnlessSingleAndMale() {
		new Hut(maleAgent1);
		new Hut(femaleAgent2);
		assertTrue(BasicActions.MARRY.isChooseable(maleAgent1));
		assertFalse(BasicActions.MARRY.isChooseable(femaleAgent2));
		new Marriage(maleAgent1, femaleAgent2);
		assertFalse(BasicActions.MARRY.isChooseable(maleAgent1));
		assertFalse(BasicActions.MARRY.isChooseable(femaleAgent2));

		try {
			new Marriage(maleAgent3, femaleAgent2);
			assertTrue(false);
		} catch (AssertionError e) {
			assertTrue(true);
		}
	}

	@Test
	public void marriesYoungestSingleAgentAtFullHealthInCurrentLocation() {
		double fullHealth = BasicAgent.FULL_HEALTH;
		// Age, Health
		createAgent(5, fullHealth-10, false);
		BasicAgent marriedAgent1 = createAgent(5, fullHealth, false);
		createAgent(10, fullHealth, false);
		createAgent(10, fullHealth-5, false);
		createAgent(20, fullHealth, false);
		createAgent(20, fullHealth-5, false);
		new Marriage (maleAgent3, marriedAgent1);
		BasicAction marryAction = BasicActions.MARRY.getAction(maleAgent1);
		run(marryAction);
		assertTrue(maleAgent1.isMarried());
		assertEquals(maleAgent1.getPartner().getAge()/1000, 10);
		assertEquals(maleAgent1.getPartner().getHealth(), fullHealth, 0.01);
	}

	@Test
	public void foragingOutputIsDoubled() {
		int foodStart = maleAgent1.getNumberInInventoryOf(Resource.FOOD);
		BasicAction forage = BasicActions.FORAGE.getAction(maleAgent1);
		run(forage);
		int foodMid = maleAgent1.getNumberInInventoryOf(Resource.FOOD) + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertTrue(foodMid > foodStart);

		new Marriage(maleAgent1, femaleAgent2);

		forage = BasicActions.FORAGE.getAction(maleAgent1);
		run(forage);
		int foodEnd = maleAgent1.getNumberInInventoryOf(Resource.FOOD)  + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertEquals(foodEnd - foodMid, 2*(foodMid-foodStart));
	}

	@Test
	public void foragingOutputIsNotDoubledIfHexIsAlmostExhausted() {
		homeHex.changeCarryingCapacity(-9);
		int foodStart = maleAgent1.getNumberInInventoryOf(Resource.FOOD)  + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		new Marriage(maleAgent1, femaleAgent2);
		BasicAction forage = BasicActions.FORAGE.getAction(maleAgent1);
		run(forage);
		int foodEnd = maleAgent1.getNumberInInventoryOf(Resource.FOOD) + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertEquals(foodEnd - foodStart, 1);	
	}

	@Test
	public void farmingOutputIsDoubled() {
		int foodStart = femaleAgent1.getNumberInInventoryOf(Resource.FOOD)  + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		new Marriage(maleAgent1, femaleAgent2);
		BasicAction farm = BasicActions.FARM.getAction(maleAgent1);
		run(farm);
		int foodEnd = maleAgent1.getNumberInInventoryOf(Resource.FOOD) + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertEquals(foodEnd - foodStart, 4);
	}

	@Test
	public void moveActionsAlsoApplyToSpouse() {
		BasicHex hex2 = new BasicHex(0, 1);
		hex2.setParentLocation(world);
		homeHex.addAccessibleLocation(hex2);
		new Marriage(maleAgent1, femaleAgent2);
		BasicAction move = new BasicMove(BasicActions.FIND_UNKNOWN, maleAgent1, new TerrainMatcher(TerrainType.OCEAN));
		run(move);
		assertTrue(maleAgent1.getLocation() == hex2);
		assertTrue(femaleAgent2.getLocation() == hex2);
		assertTrue(femaleAgent3.getLocation() == homeHex);
	}

	@Test
	public void foodIsEquallySharedInMaintenance() {
		for (int loop = 0; loop < 5; loop++)
			maleAgent1.addItem(Resource.FOOD);
		new Marriage(maleAgent1, femaleAgent2);
		assertEquals(maleAgent1.getNumberInInventoryOf(Resource.FOOD), 5);
		assertEquals(femaleAgent2.getNumberInInventoryOf(Resource.FOOD), 0);
		maleAgent1.maintenance();
		assertEquals(maleAgent1.getNumberInInventoryOf(Resource.FOOD), 3);
		assertEquals(femaleAgent2.getNumberInInventoryOf(Resource.FOOD), 2);
		maleAgent1.addItem(Resource.FOOD);
		femaleAgent2.removeItem(Resource.FOOD);
		maleAgent1.maintenance();
		assertEquals(maleAgent1.getNumberInInventoryOf(Resource.FOOD), 3);
		assertEquals(femaleAgent2.getNumberInInventoryOf(Resource.FOOD), 2);
		maleAgent1.removeItem(Resource.FOOD);
		maleAgent1.removeItem(Resource.FOOD);
		maleAgent1.removeItem(Resource.FOOD);
		femaleAgent2.maintenance();
		maleAgent1.maintenance();
		assertEquals(maleAgent1.getNumberInInventoryOf(Resource.FOOD), 1);
		assertEquals(femaleAgent2.getNumberInInventoryOf(Resource.FOOD), 1);
	}

	@Test
	public void marriageIsDissolvedOnDeath() {
		new Marriage(maleAgent1, femaleAgent2);
		maleAgent1.die("OOps");
		assertFalse(femaleAgent2.isMarried());
		new Marriage(femaleAgent2, maleAgent3);
		assertTrue(femaleAgent2.isMarried());
		maleAgent3.die("Black Widow");
		assertFalse(femaleAgent2.isMarried());
	}
	
	@Test
	public void onMarriageFemaleActionPlanIsReplacedWithSpousalDecision() {
		femaleAgent2.setDecider(baseDecider);
		// Male is still a hard-coded RESTer
		femaleAgent2.decide();
		Action ifa = femaleAgent2.getActionPlan().getNextAction();
		assertEquals(ifa.getAllConfirmedParticipants().size(), 1);
		BasicAction marryAction = BasicActions.MARRY.getAction(maleAgent1);
		run(marryAction);
		Action nfa = femaleAgent2.getActionPlan().getNextAction();
		assertFalse(ifa.equals(nfa));
		assertTrue(ifa.getState() == Action.State.CANCELLED);
		Action nma = maleAgent1.getActionPlan().getNextAction();
		assertTrue(nma.getType() == BasicActions.REST);
		assertEquals(nma.getAllConfirmedParticipants().size(), 2);
		assertTrue(nma.equals(nfa));
	}
	@Test
	public void femaleDecideDoesNothingIfMarried() {
		femaleAgent2.setDecider(baseDecider);
		assertTrue(femaleAgent2.getActionPlan().getNextAction() == null);
		new Marriage(maleAgent1, femaleAgent2);
		femaleAgent2.decide();
		assertTrue(femaleAgent2.getActionPlan().getNextAction() == null);
		maleAgent1.decide();
		assertFalse(femaleAgent2.getActionPlan().getNextAction() == null);
	}
	@Test
	public void experienceRecordsUpdatedIndependentlyForSpousalAction() {
		femaleAgent2.setDecider(baseDecider);
		femaleAgent2.decide();	// this will register femaleAgent2 with the teacher
		ExperienceRecord<BasicAgent> initialFemaleER = teacher.getExperienceRecords(femaleAgent2).get(0);
		assertTrue(initialFemaleER.getState() == ExperienceRecord.State.DECISION_TAKEN);
		maleAgent1.setDecider(baseDecider);
		BasicAction marryAction = BasicActions.MARRY.getAction(maleAgent1);
		run(marryAction);
		assertTrue(initialFemaleER.getState() == ExperienceRecord.State.DECISION_TAKEN);
		BasicAction sa = (BasicAction) maleAgent1.getActionPlan().getNextAction();
		assertEquals(sa.getAllConfirmedParticipants().size(), 2);
		assertEquals(teacher.getExperienceRecords(femaleAgent2).size(), 2); // initial and Marry
		assertEquals(teacher.getExperienceRecords(maleAgent1).size(), 1); // Rest
		ExperienceRecord<BasicAgent> maleER = teacher.getExperienceRecords(maleAgent1).get(0);
		ExperienceRecord<BasicAgent> femaleMarryER = teacher.getExperienceRecords(femaleAgent2).get(1);
		assertTrue(maleER.getActionTaken() == sa);
		assertTrue(femaleMarryER.getActionTaken() == marryAction);
		assertTrue(maleER.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertEquals(maleER.getPossibleActionsFromStartState().size(), 2);
		assertTrue(maleER.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertEquals(femaleMarryER.getPossibleActionsFromStartState().size(), 1);
		assertTrue(initialFemaleER.getState() == ExperienceRecord.State.DECISION_TAKEN);
		assertTrue(femaleMarryER.getState() == ExperienceRecord.State.ACTION_COMPLETED);
		maleAgent1.addHealth(-2.0);
		femaleAgent1.addHealth(-4.0);
		run(sa);
		assertEquals(teacher.getExperienceRecords(femaleAgent2).size(), 2); // initial and REST
		ExperienceRecord<BasicAgent> femaleRestER = teacher.getExperienceRecords(femaleAgent2).get(1);
		assertTrue(maleER.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(femaleMarryER.getState() == ExperienceRecord.State.NEXT_ACTION_TAKEN);
		assertTrue(femaleRestER.getState() == ExperienceRecord.State.ACTION_COMPLETED);
		assertEquals(maleER.getReward(), -2.0, 0.01);
		assertEquals(femaleRestER.getReward(), 0.0, 0.01);
		assertEquals(femaleMarryER.getReward(), -4.0, 0.01);
		assertTrue(maleER.getStartState() != femaleRestER.getStartState());
		assertEquals(maleER.getPossibleActionsFromEndState().size(), 2);
		assertEquals(femaleMarryER.getPossibleActionsFromEndState().size(), 1);
	}
	
	@Test
	public void onDissolutionSpouseTakesActionsAgain() {
		new Hut(maleAgent1);
		maleAgent1.setDecider(new HardCodedDecider(BasicActions.MARRY));
		femaleAgent2.decide();
		maleAgent1.decide();
		ap.processActionsInQueue(4);
		assertTrue(maleAgent1.isMarried());
		assertTrue(femaleAgent2.isMarried());
		Action nextMaleAction = maleAgent1.getNextAction();
		assertTrue(nextMaleAction instanceof Rest);
		assertEquals(nextMaleAction.getAllConfirmedParticipants().size(), 2);
		assertEquals(maleAgent1.getActionPlan().sizeOfQueue(), 1);
		assertTrue(femaleAgent2.getNextAction() == nextMaleAction);
		maleAgent1.die("Oops");
		assertTrue(femaleAgent2.getNextAction() instanceof Rest);// i.e. not ObeySpouse
	}

	@Test
	public void checkStateVariableDistinguishesBetweenMarriedStates() {
		new Marriage(maleAgent1, femaleAgent2);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(maleAgent1, maleAgent1), 1.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(femaleAgent2, femaleAgent2), 1.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(maleAgent3, maleAgent3), 0.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(femaleAgent3, femaleAgent3), 0.0, 0.001);
	}
	
	private void run(BasicAction a) {
		for (BasicAgent agent : a.getAllInvitedParticipants()) {
			a.agree(agent);
		}
		a.start();
		a.run();
	}

}
