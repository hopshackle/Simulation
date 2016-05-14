package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import java.util.List;

import org.junit.*;

public class BasicBreedTest {

	private World world;
	private BasicHex homeHex;
	private BasicAgent testAgent1, testAgent2, testAgent3;
	private TestActionProcessor ap;

	@Before
	public void setUp() {
		SimProperties.setProperty("FemaleBreedingAgeRange", "20-100");
		SimProperties.setProperty("MaleBreedingAgeRange", "20-180");
		BasicAgent.refreshBreedingAges();
		world = new World();
		ap = new TestActionProcessor(world);
		homeHex = new BasicHex(0, 0);
		homeHex.setParentLocation(world);
		testAgent1 = new BasicAgent(world);
		testAgent2 = new BasicAgent(world);
		testAgent3 = new BasicAgent(world);
		testAgent1.setMale(true);
		testAgent1.setAge(25000);
		testAgent2.setAge(25000);
		testAgent3.setAge(25000);
		testAgent2.setMale(false);
		testAgent2.setMale(false);
		testAgent1.setLocation(homeHex);
		testAgent2.setLocation(homeHex);
		testAgent3.setLocation(homeHex);
		testAgent1.setDecider(new HardCodedDecider(BasicActions.REST));
	}

	@Test
	public void canSeekToBreedOnlyIfMarriedAndYouHaveAHutAndAreOnFullHealthWithAtLeastOneFood() {
		Marriage marriage = new Marriage(testAgent1, testAgent2);
		testAgent1.setLocation(homeHex);
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
		new Hut(testAgent1);
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
		testAgent1.addItem(Resource.FOOD);
		assertTrue(BasicActions.BREED.isChooseable(testAgent1));
		testAgent1.addHealth(-10);
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
		testAgent1.addHealth(10);
		assertTrue(BasicActions.BREED.isChooseable(testAgent1));
		new Village(homeHex);
		assertTrue(BasicActions.BREED.isChooseable(testAgent1));
		marriage.dissolve();
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
	}

	@Test
	public void canOnlyBreedIfBothPartnersAreOfBreedingAge() {
		setUpValidMarriageBetween(testAgent1, testAgent2);
		testAgent1.setAge(15000);
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
		testAgent1.setAge(25000);
		assertTrue(BasicActions.BREED.isChooseable(testAgent1));
		testAgent2.setAge(15000);
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
		testAgent2.setAge(99500);
		assertTrue(BasicActions.BREED.isChooseable(testAgent1));
		testAgent2.setAge(100500);
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
		testAgent2.setAge(99500);
		testAgent1.setAge(175000);
		assertTrue(BasicActions.BREED.isChooseable(testAgent1));
		testAgent1.setAge(182000);
		assertFalse(BasicActions.BREED.isChooseable(testAgent1));
	}
	
	private void setUpValidMarriageBetween(BasicAgent agent1, BasicAgent agent2) {
		new Marriage(agent1, agent2);
		testAgent1.setLocation(homeHex);
		new Hut(agent1);
		agent1.addItem(Resource.FOOD);
		assertTrue(BasicActions.BREED.isChooseable(testAgent1));
	}

	@Test
	public void breedingProducesASingleChildAgentAndSubtractsHealthFromEachParent() {
		testAgent1.addItem(Resource.FOOD);
		assertEquals(homeHex.getAgents().size(), 3);
		Action breed = new Breed(testAgent1, testAgent2);
		run(breed);
		List<Agent> fullPopulation = homeHex.getAgents();
		assertEquals(fullPopulation.size(), 4);
		assertEquals(testAgent1.getNumberOfChildren(), 1);
		assertEquals(testAgent2.getNumberOfChildren(), 1);
		assertEquals(testAgent1.getHealth(), 10, 0.001);
		assertEquals(testAgent2.getHealth(), 10, 0.001);
		assertEquals(testAgent1.getNumberInInventoryOf(Resource.FOOD), 0);

		for (Agent a : fullPopulation) {
			if (a.equals(testAgent1) || a.equals(testAgent2) || a.equals(testAgent3))
				continue;
			BasicAgent child = (BasicAgent)a;
			assertEquals(child.getHealth(), 20, 0.001);
			assertEquals(child.getNumberInInventoryOf(Resource.FOOD), 1);
			assertEquals(testAgent1.getChildren().get(0).getUniqueID(), child.getUniqueID());
			assertEquals(testAgent2.getChildren().get(0).getUniqueID(), child.getUniqueID());

			List<Long> parents = child.getParents();
			assertEquals((long)parents.get(0), testAgent1.getUniqueID());
			assertEquals((long)parents.get(1), testAgent2.getUniqueID());
		}
	}

	@Test
	public void aChildIncreasesScoreOfBothParentsByItsHealthAtBirth() {
		Action breed = new Breed(testAgent1, testAgent2);
		run(breed);
		assertEquals(testAgent1.getScore(), 30, 0.001);
		assertEquals(testAgent2.getScore(), 30, 0.001);
		assertEquals(testAgent3.getScore(), 20, 0.001);
	}

	@Test
	public void unmarriedParentIsForcedToRestWhileBreedingOccursAndChildAlsoMakesADecision() {
		// this will never currently happen given that BREED cannot be selected unless married.
		// However the functionality can be re-enabled easily, and hence is tested
		long currentTime  = world.getCurrentTime();
		testAgent2.addAction(new Forage(testAgent2));
		Action breed = new Breed(testAgent1, testAgent2);
		testAgent1.addAction(breed);
		Action nextAction = ap.getNextAction();
		assertTrue(nextAction instanceof Rest);
		assertEquals(nextAction.getActor(), testAgent2);
		assertEquals(nextAction.getStartTime() - currentTime, 4990);
		nextAction = ap.getNextAction();
		assertEquals(nextAction.getActor(), testAgent1);
		nextAction = ap.getNextAction();
		assertEquals(nextAction.getActor().getUniqueID(), testAgent1.getChildren().get(0).getUniqueID());
	}

	@Test
	public void marriedParentAlwaysBreedsWithSpouse() {
		new Marriage(testAgent1, testAgent2);
		assertTrue( new PartnerFinder(testAgent1, new BreedingPartnerScoringFunction(testAgent1)).getPartner().equals(testAgent2));
	}

	@Test
	public void spouseKeepsObeyingSpouseDuringPregnancy() {
		new Hut(testAgent1);
		testAgent2.setDecider(new HardCodedDecider(BasicActions.REST));
		testAgent1.setDecider(new HardCodedDecider(BasicActions.BREED));
		Action marriage = new Marry(testAgent1, testAgent2);
		run(marriage);
		assertTrue(testAgent1.isMarried());
		assertTrue(testAgent2.isMarried());
		ap.getValidateAndRunNextAction(ObeySpouse.class);
		ap.getValidateAndRunNextAction(ObeySpouse.class);
		ap.getValidateAndRunNextAction(ObeySpouse.class);
	}

	@Test
	public void childInheritsMapKnowledgeFromBothParents() {
		Location newLoc1 = new Location();
		Location newLoc2 = new Location();
		testAgent1.setLocation(newLoc1);
		testAgent2.setLocation(newLoc2);
		testAgent1.setLocation(homeHex);
		testAgent2.setLocation(homeHex);
		Action breed = new Breed(testAgent1, testAgent2);
		run(breed);
		BasicAgent child = getChildFromHomeHex();

		assertTrue(child.getMapKnowledge().isKnown(homeHex));
		assertTrue(child.getMapKnowledge().isKnown(newLoc1));
		assertTrue(child.getMapKnowledge().isKnown(newLoc2));
	}

	@Test
	public void childGenerationIsSetToOneGreaterThanParents() {
		testAgent1.setGeneration(1);
		testAgent2.setGeneration(2);
		Action breed = new Breed(testAgent1, testAgent2);
		run(breed);
		BasicAgent child = getChildFromHomeHex();
		assertEquals(child.getGeneration(), 2);
	}

	@Test
	public void childInheritHuts() {
		new Hut(testAgent1);
		Action breed = new Breed(testAgent1, testAgent2);
		run(breed);
		BasicAgent child = getChildFromHomeHex();
		assertEquals(child.getNumberInInventoryOf(BuildingType.HUT), 0);
		assertEquals(testAgent1.getNumberInInventoryOf(BuildingType.HUT), 1);
		testAgent1.die("Ooops");
		assertEquals(child.getNumberInInventoryOf(BuildingType.HUT), 1);
		assertEquals(testAgent1.getNumberInInventoryOf(BuildingType.HUT), 0);
	}

	@Test
	public void childrenInheritAHutEach() {
		new Hut(testAgent1);
		new Hut(testAgent1);
		new Hut(testAgent1);

		Action breed = new Breed(testAgent1, testAgent2);
		run(breed);
		BasicAgent child1 = getChildFromHomeHex();

		testAgent1.addHealth(20);
		testAgent2.addHealth(20);
		breed = new Breed(testAgent1, testAgent2);
		run(breed);
		testAgent3.setLocation(new Location());
		testAgent3 = child1;
		BasicAgent child2 = getChildFromHomeHex();

		assertEquals(child1.getNumberInInventoryOf(BuildingType.HUT), 0);
		assertEquals(child2.getNumberInInventoryOf(BuildingType.HUT), 0);
		assertEquals(testAgent1.getNumberInInventoryOf(BuildingType.HUT), 3);
		testAgent1.die("Ooops");
		assertEquals(child1.getNumberInInventoryOf(BuildingType.HUT), 2);
		assertEquals(child2.getNumberInInventoryOf(BuildingType.HUT), 1);
		assertEquals(testAgent1.getNumberInInventoryOf(BuildingType.HUT), 0);
	}

	@Test
	public void spouseInheritsIfNoChildren() {
		new Marriage(testAgent1, testAgent2);
		new Hut(testAgent1);
		Action breed = new Breed(testAgent1, testAgent2);
		run(breed);
		BasicAgent child = getChildFromHomeHex();
		child.die("ooops");
		testAgent1.die("Oops");
		assertEquals(child.getNumberInInventoryOf(BuildingType.HUT), 0);
		assertEquals(testAgent2.getNumberInInventoryOf(BuildingType.HUT), 1);
	}	

	private BasicAgent getChildFromHomeHex() {
		List<Agent> fullPopulation = homeHex.getAgents();
		BasicAgent child = null;
		for (Agent a : fullPopulation) {
			if (a.equals(testAgent1) || a.equals(testAgent2) || a.equals(testAgent3))
				continue;
			child = (BasicAgent)a;
		}
		return child;
	}
	private void run(Action a) {
		a.agree(a.getActor());
		a.start();
		a.run();
	}
}
