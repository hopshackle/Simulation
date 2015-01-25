package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import java.util.ArrayList;

import org.junit.*;
public class BasicMarriageTest {

	private World world;
	private BasicHex homeHex;
	private BasicAgent maleAgent1, maleAgent2, maleAgent3;
	private BasicAgent femaleAgent1, femaleAgent2, femaleAgent3;
	private TestActionProcessor ap;

	@Before
	public void setUp() {
		world = new World();
		ap = new TestActionProcessor(world);
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
		retAgent.setDecider(new HardCodedDecider(BasicActions.REST));
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
		Action marryAction = BasicActions.MARRY.getAction(maleAgent1);
		marryAction.run();
		assertTrue(maleAgent1.isMarried());
		assertEquals(maleAgent1.getPartner().getAge()/1000, 10);
		assertEquals(maleAgent1.getPartner().getHealth(), fullHealth, 0.01);
	}

	@Test
	public void spouseOnlyTakesObeySpouseActionsAfterMarriage() {
		maleAgent1.setDecider(new HardCodedDecider(BasicActions.MARRY));
		ap.makeValidateAndRunFirstDecision(femaleAgent2, Rest.class);
		ap.makeValidateAndRunFirstDecision(maleAgent1, Marry.class);
		assertTrue(maleAgent1.isMarried());
		assertTrue(femaleAgent2.isMarried());
		ArrayList<Action> actionList = new ArrayList<Action>();
		actionList.add(ap.getNextAction());
		actionList.add(ap.getNextAction());
		findInActionArray(actionList, Rest.class, maleAgent1);
		findInActionArray(actionList, ObeySpouse.class, femaleAgent2);
		actionList.get(0).run();
		actionList.get(1).run();
		actionList.clear();
		actionList.add(ap.getNextAction());
		actionList.add(ap.getNextAction());
		findInActionArray(actionList, Rest.class, maleAgent1);
		findInActionArray(actionList, ObeySpouse.class, femaleAgent2);
	}


	private void findInActionArray(ArrayList<Action> actionList, Class<? extends Action> classType, BasicAgent testAgent) {
		boolean matchFound = false;
		for (Action a : actionList) {
			if (a.getActor() == testAgent) {
				assertTrue(classType.isInstance(a));
				matchFound = true;
			}
		}
		assertTrue("No match found in array", matchFound);
	}

	@Test
	public void foragingOutputIsDoubled() {
		int foodStart = maleAgent1.getNumberInInventoryOf(Resource.FOOD);
		Action forage = BasicActions.FORAGE.getAction(maleAgent1);
		forage.run();
		int foodMid = maleAgent1.getNumberInInventoryOf(Resource.FOOD) + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertTrue(foodMid > foodStart);

		new Marriage(maleAgent1, femaleAgent2);

		forage = BasicActions.FORAGE.getAction(maleAgent1);
		forage.run();
		int foodEnd = maleAgent1.getNumberInInventoryOf(Resource.FOOD)  + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertEquals(foodEnd - foodMid, 2*(foodMid-foodStart));
	}

	@Test
	public void foragingOutputIsNotDoubledIfHexIsAlmostExhausted() {
		homeHex.changeCarryingCapacity(-9);
		int foodStart = maleAgent1.getNumberInInventoryOf(Resource.FOOD)  + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		new Marriage(maleAgent1, femaleAgent2);
		Action forage = BasicActions.FORAGE.getAction(maleAgent1);
		forage.run();
		int foodEnd = maleAgent1.getNumberInInventoryOf(Resource.FOOD) + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertEquals(foodEnd - foodStart, 1);	
	}

	@Test
	public void farmingOutputIsDoubled() {
		int foodStart = femaleAgent1.getNumberInInventoryOf(Resource.FOOD)  + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		new Marriage(maleAgent1, femaleAgent2);
		Action farm = BasicActions.FARM.getAction(maleAgent1);
		farm.run();
		int foodEnd = maleAgent1.getNumberInInventoryOf(Resource.FOOD) + femaleAgent2.getNumberInInventoryOf(Resource.FOOD);
		assertEquals(foodEnd - foodStart, 4);
	}

	@Test
	public void moveActionsAlsoApplyToSpouse() {
		BasicHex hex2 = new BasicHex(0, 1);
		hex2.setParentLocation(world);
		homeHex.addAccessibleLocation(hex2);
		new Marriage(maleAgent1, femaleAgent2);
		Action move = new Move(maleAgent1, 0, hex2);
		move.run();
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
	public void onDissolutionSpouseTakesActionsAgain() {
		new Hut(maleAgent1);
		maleAgent1.setDecider(new HardCodedDecider(BasicActions.MARRY));
		ap.makeValidateAndRunFirstDecision(femaleAgent2, Rest.class);
		ap.makeValidateAndRunFirstDecision(maleAgent1, Marry.class);
		assertTrue(maleAgent1.isMarried());
		assertTrue(femaleAgent2.isMarried());
		ArrayList<Action> actionList = new ArrayList<Action>();
		actionList.add(ap.getNextAction());
		actionList.add(ap.getNextAction());
		findInActionArray(actionList, Rest.class, maleAgent1);
		findInActionArray(actionList, ObeySpouse.class, femaleAgent2);
		maleAgent1.die("Oops");
		actionList.get(0).run();
		actionList.get(1).run();
		actionList.clear();
		actionList.add(ap.getNextAction());
		findInActionArray(actionList, Rest.class, femaleAgent2); // i.e. not ObeySpouse
	}

	@Test
	public void checkStateVariableDistinguishesBetweenMarriedStates() {
		new Marriage(maleAgent1, femaleAgent2);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(maleAgent1, maleAgent1), 1.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(femaleAgent2, femaleAgent2), 1.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(maleAgent3, maleAgent3), 0.0, 0.001);
		assertEquals(BasicVariables.MARRIED_STATUS.getValue(femaleAgent3, femaleAgent3), 0.0, 0.001);
	}

}
