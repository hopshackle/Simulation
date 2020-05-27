package hopshackle.simulation.test;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.*;
public class BasicMarriageTest {

	private World world;
	private BasicHex homeHex;
	private BasicAgent maleAgent1, maleAgent2, maleAgent3;
	private BasicAgent femaleAgent1, femaleAgent2, femaleAgent3;
	private TestActionProcessor ap;
	private ExperienceRecordCollector<BasicAgent> teacher;
	private List<ActionEnum<BasicAgent>> actions;
	private List<GeneticVariable<BasicAgent>> variables = new ArrayList<GeneticVariable<BasicAgent>>(EnumSet.allOf(BasicVariables.class));
	private Decider<BasicAgent> baseDecider;
	private Decider<BasicAgent> restDecider = new HardCodedDecider<BasicAgent>(BasicActions.REST);
	DeciderProperties localProp;
	private List<ActionEnum<BasicAgent>> allActions = new ArrayList<ActionEnum<BasicAgent>>(EnumSet.allOf(BasicActions.class));


	@Before
	public void setUp() {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
		localProp.setProperty("IncrementalScoreReward", "true");
		teacher = new ExperienceRecordCollector<BasicAgent>(new StandardERFactory<BasicAgent>(localProp));
		actions = new ArrayList<ActionEnum<BasicAgent>>();
		actions.add(BasicActions.REST);
		actions.add(BasicActions.FORAGE);
		actions.add(BasicActions.MARRY);
		actions.add(BasicActions.LOOK_FOR_PARTNER);
		StateFactory<BasicAgent> sf = new LinearStateFactory<BasicAgent>(variables);
		baseDecider = new GeneralLinearQDecider<BasicAgent>(sf);
		world = new World(null, "test", new SimpleWorldLogic<BasicAgent>(allActions));
		world.setCalendar(new FastCalendar(0l));
		ap = new TestActionProcessor(world);
		world.registerWorldLogic(new SimpleWorldLogic<BasicAgent>(actions), "AGENT");
		homeHex = new BasicHex(0, 0);
		homeHex.setParentLocation(world);
		maleAgent1 = createAgent(50, 20, true);
		maleAgent2 = createAgent(40, 20, true);
		maleAgent3 = createAgent(50, 20, true);
		femaleAgent1 = createAgent(55, 20, false);
		femaleAgent2 = createAgent(45, 20, false);
		femaleAgent3 = createAgent(47, 20, false);
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
		assertTrue(BasicActions.LOOK_FOR_PARTNER.isChooseable(maleAgent1));
		assertFalse(BasicActions.LOOK_FOR_PARTNER.isChooseable(femaleAgent2));
		new Marriage(maleAgent1, femaleAgent2);
		assertFalse(BasicActions.LOOK_FOR_PARTNER.isChooseable(maleAgent1));
		assertFalse(BasicActions.LOOK_FOR_PARTNER.isChooseable(femaleAgent2));

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
		BasicAction lookForPartner = new LookForPartner(maleAgent1);
		world.setCurrentTime(500l); // otherwise we have time to REST again
		run(lookForPartner);
		BasicAction marryAction = (BasicAction) maleAgent1.getActionPlan().getNextAction();
		assertTrue(marryAction instanceof Marry);
		run(marryAction);
		assertTrue(maleAgent1.isMarried());
		assertEquals(maleAgent1.getPartner().getAge()/1000, 10);
		assertEquals(maleAgent1.getPartner().getHealth(), fullHealth, 0.01);
	}

	@Test
	public void foragingOutputIsDoubled() {
		int foodStart = maleAgent1.getNumberInInventoryOf(Resource.FOOD)  + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		new Marriage(maleAgent1, femaleAgent2);
		BasicAction forage = BasicActions.FORAGE.getAction(maleAgent1);
		run(forage);
		int foodEnd = maleAgent1.getNumberInInventoryOf(Resource.FOOD) + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertEquals(foodEnd - foodStart, 2);
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
		BasicAction lookForPartner = new LookForPartner(maleAgent1);
		world.setCurrentTime(500l); // otherwise we have time to REST again
		run(lookForPartner);
		BasicAction marryAction = (BasicAction) maleAgent1.getActionPlan().getNextAction();
		assertTrue(marryAction instanceof Marry);
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
		homeHex.setTerrain(TerrainType.FOREST);		// otherwise Foraging gives FOOD, which spoils score tests
		teacher.registerAgent(femaleAgent2);
		teacher.registerAgent(maleAgent1);
		femaleAgent2.setDecider(baseDecider);
		femaleAgent2.decide();	// Chooses REST for 0-1000
		femaleAgent2.getNextAction().start();
		ExperienceRecord<BasicAgent> initialFemaleER = teacher.getExperienceRecords(femaleAgent2).get(0);
		assertSame(initialFemaleER.getState(), ExperienceRecord.ERState.DECISION_TAKEN);
		maleAgent1.setDecider(baseDecider);
		assertSame(initialFemaleER.getState(), ExperienceRecord.ERState.DECISION_TAKEN);
		BasicAction lookForPartner = new LookForPartner(maleAgent1);
		world.setCurrentTime(600L); // otherwise we have time to REST again
		run(lookForPartner); // sets Marry for 1100-2100
		BasicAction marryAction = (BasicAction) maleAgent1.getActionPlan().getNextAction();
		ExperienceRecord<BasicAgent> femaleMarryER = teacher.getExperienceRecords(femaleAgent2).get(1);
		assertEquals(femaleMarryER.getActionTaken(), marryAction);
		assertTrue(marryAction instanceof Marry);
		assertSame(marryAction.getState(), Action.State.PLANNED);
		assertSame(initialFemaleER.getActionTaken().getState(), Action.State.EXECUTING);
		femaleAgent2.getNextAction().run();
		assertSame(initialFemaleER.getActionTaken().getState(), Action.State.FINISHED);
		marryAction.start();
		marryAction.run();
		// this agrees for all participants, then start, then run
		// male will decide after action execution. Decision should bind spouse as well.
		// The cancellation of the unexecuted female action will move ER to ACTION_COMPLETED
		// and then when female agrees to male action, this creates a new dummy ER, which
		// moves the earlier ER to NEXT_ACTION_TAKEN
		// The same thing happens with the Marry action: Marry.run() should move to ACTION_COMPLETED
		// Then agreement to the next male action takes us to NEXT_ACTION_TAKEN
		assertSame(initialFemaleER.getState(), ExperienceRecord.ERState.NEXT_ACTION_TAKEN);
		assertSame(marryAction.getState(), Action.State.FINISHED);
		assertSame(femaleMarryER.getState(), ExperienceRecord.ERState.NEXT_ACTION_TAKEN);
		assertEquals(teacher.getExperienceRecords(femaleAgent2).size(), 4);
		// these four are 0->600 REST, 600->600 MARRY, 600->600 FORAGE (CANCELLED), 600->??? FORAGE (from husband)
		assertSame(teacher.getExperienceRecords(femaleAgent2).get(2).getActionTaken().getState(), Action.State.CANCELLED);
		BasicAction sa = (BasicAction) maleAgent1.getActionPlan().getNextAction();
		assertEquals(sa.getAllConfirmedParticipants().size(), 2);
		assertEquals(teacher.getExperienceRecords(maleAgent1).size(), 4);
		// these four are 0->600 REST, 600->600 MARRY, 600->600 FORAGE (CANCELLED), 600->??? FORAGE
		ExperienceRecord<BasicAgent> maleER = teacher.getExperienceRecords(maleAgent1).get(3);
		ExperienceRecord<BasicAgent> femaleRestER = teacher.getExperienceRecords(femaleAgent2).get(3);
		assertSame(maleER.getActionTaken(), sa);
		assertSame(femaleRestER.getActionTaken(), sa);
		assertSame(maleER.getState(), ExperienceRecord.ERState.DECISION_TAKEN);
		assertEquals(maleER.getPossibleActionsFromStartState().size(), 2);
		assertSame(maleER.getState(), ExperienceRecord.ERState.DECISION_TAKEN);
		assertEquals(femaleMarryER.getPossibleActionsFromStartState().size(), 1);
		assertSame(femaleRestER.getState(), ExperienceRecord.ERState.DECISION_TAKEN);
		maleAgent1.addHealth(-2.0);
		femaleAgent2.addHealth(-4.0);
		run(sa);
		assertEquals(teacher.getExperienceRecords(femaleAgent2).size(), 5); // Rest
		ExperienceRecord<BasicAgent> femaleNewRestER = teacher.getExperienceRecords(femaleAgent2).get(4);
		assertSame(maleER.getState(), ExperienceRecord.ERState.NEXT_ACTION_TAKEN);
		assertSame(femaleRestER.getState(), ExperienceRecord.ERState.NEXT_ACTION_TAKEN);
		assertSame(femaleNewRestER.getState(), ExperienceRecord.ERState.DECISION_TAKEN);
		assertEquals(maleER.getReward()[0], -2.0, 0.01);
		assertEquals(femaleRestER.getReward()[0], -4.0, 0.01);
		assertNotSame(maleER.getStartState(false), femaleRestER.getStartState(false));
		assertEquals(maleER.getPossibleActionsFromEndState().size(), 2);
		assertEquals(femaleMarryER.getPossibleActionsFromEndState().size(), 1);
		assertEquals(femaleRestER.getPossibleActionsFromEndState().size(), 1);
	}
	
	@Test
	public void onDissolutionSpouseTakesActionsAgain() {
		new Hut(maleAgent1);
		maleAgent1.setDecider(new HardCodedDecider<BasicAgent>(BasicActions.LOOK_FOR_PARTNER));
		femaleAgent2.decide();
		maleAgent1.decide();
		maleAgent1.setDecider(new HardCodedDecider<BasicAgent>(BasicActions.REST));
		ap.processActionsInQueue(6);	// REST, LOOK_FOR_PARTNER, then MARRY
		assertTrue(maleAgent1.isMarried());
		assertTrue(femaleAgent2.isMarried());
		Action<BasicAgent> nextMaleAction = (Action<BasicAgent>) maleAgent1.getNextAction();
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
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(maleAgent1), 1.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(femaleAgent2), 1.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(maleAgent3), 0.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(femaleAgent3), 0.0, 0.001);
	}
	
	@Test
	public void lookForPartnerNotEligibleIfMarriageIsPlanned() {
		assertTrue(BasicActions.LOOK_FOR_PARTNER.isChooseable(maleAgent1));
		BasicAction marryAction = new Marry(maleAgent1, femaleAgent2);
		marryAction.addToAllPlans();
		assertFalse(BasicActions.LOOK_FOR_PARTNER.isChooseable(maleAgent1));
	}
	
	@Test
	public void partnerNotEligibleIfMarriagePlanned() {
		PartnerFinder advertMarry = new PartnerFinder(maleAgent1, new MarriagePartnerScoringFunction(maleAgent1));
		assertTrue(advertMarry.getPartner() == femaleAgent2);
		BasicAction marryAction = new Marry(maleAgent2, femaleAgent2);
		marryAction.addToAllPlans();
		assertTrue(advertMarry.getPartner() != femaleAgent2);
	}
	
	private void run(BasicAction a) {
		a.addToAllPlans();
		a.start();
		a.run();
	}

}
