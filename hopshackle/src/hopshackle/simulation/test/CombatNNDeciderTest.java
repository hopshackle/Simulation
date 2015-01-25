package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.DoNothing;
import hopshackle.simulation.dnd.genetics.*;

import java.io.File;
import java.util.*;

import org.junit.*;
public class CombatNNDeciderTest {

	private Character testClr, testFtr, testLoneClr;
	private World w;
	private Party party;
	NeuralDecider decider;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	@Before
	public void setUp() throws Exception {
		SimProperties.setProperty("Temperature", "0.00");
		SimProperties.setProperty("StartWithPotion", "false");
		SimProperties.setProperty("NeuralNoise", "0.20");
		w = new World();
		Square sq = new Square(0,1);
		sq.setParentLocation(w);
		testLoneClr = new Character(Race.HUMAN, CharacterClass.CLERIC, w);
		testLoneClr.setWisdom(new Attribute(16));
		testLoneClr.rest(1);
		testClr = new Character(Race.HUMAN, CharacterClass.CLERIC, w);	
		testClr.setWisdom(new Attribute(8));
		testClr.rest(1);
		testFtr = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		party = new Party(testClr);
		party.addMember(testFtr);
		decider = new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputs.class)));
		testClr.setCombatDecider(decider);
		testLoneClr.setCombatDecider(decider);

		testClr.setLocation(sq);
		testLoneClr.setLocation(sq);
		testFtr.setLocation(sq);

	}

	@Test
	public void checkName() {
		assertTrue (decider.toString().equals("DEFAULT"));
		decider.setName("test");
		assertTrue (decider.toString().equals("test"));
	}

	@Test
	public void checkNoiseOverride() {
		assertEquals(decider.getNoise(), 0.20, 0.001);
		SimProperties.setProperty("NeuralNoise.Career", "0.30");
		decider.setName("Career");
		assertEquals(decider.getNoise(), 0.30, 0.001);	
	}
	
	@Test
	public void checkErrorOnNoiseOverride() {
		assertEquals(decider.getNoise(), 0.20, 0.001);
		SimProperties.setProperty("NeuralNoise.test", "0.30");
		decider.setName("Other");
		assertEquals(decider.getNoise(), 0.20, 0.001);	
	}
	
	@Test
	public void checkLoneClrOptimism() {
		Fight f = new Fight(testLoneClr, new Character(Race.GOBLIN, CharacterClass.WARRIOR, w));
		double testResult = 0.0;
		Iterator i = EnumSet.allOf(CombatActionsI.class).iterator();
		ActionEnum choice = null;
		double highResult = 0.0;
		while(i.hasNext()) {
			ActionEnum ie = (ActionEnum)i.next();
			testResult = decider.valueOption(ie, testLoneClr, testLoneClr);
			assertTrue(testResult > 0.8);	// test optimism training
			if (testResult > highResult && ie.isChooseable(testLoneClr)) {
				highResult = testResult;
				choice = ie;
			}
		}

		Action predictedDecision = choice.getAction(testLoneClr);
		Action decision = decider.decide(testLoneClr, testLoneClr).getAction(testLoneClr);
		assertTrue(predictedDecision.toString().equals(decision.toString()));
	}

	@Test
	public void checkPartyOptimism() {
		double testResult = 0.0;
		Iterator i = EnumSet.allOf(CombatActionsI.class).iterator();
		while(i.hasNext()) {
			testResult = decider.valueOption((ActionEnum)i.next(), testClr, testClr);
			assertTrue(testResult > 0.8);	// test optimism training
		}
	}

	@Test
	public void testGeneticOdds() {
		Character goblin = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		Character goblin2 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);

		Party goblinParty = new Party(goblin);
		goblinParty.addMember(goblin2);
		Fight f = new Fight(party, goblinParty);

		assertEquals(CombatNeuronInputs.ODDS.getValue(testClr, goblin2), 0.0, 0.0001);
		assertEquals(CombatNeuronInputs.ODDS.getValue(goblin2, testClr), 0.0, 0.0001);

		f.removeCombatant(goblin.getCombatAgent());
		assertEquals(CombatNeuronInputs.ODDS.getValue(testClr, goblin2), 0.1, 0.0001);
		assertEquals(CombatNeuronInputs.ODDS.getValue(goblin2, testClr), -0.1, 0.0001);

	}

	@Test
	public void testGeneticVariables() {
		Character goblin = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		Character goblin2 = new Character(Race.GOBLIN, CharacterClass.WARRIOR, w);
		new Fight(party, goblin);
		new Fight(testLoneClr, goblin2);
		goblin.getCombatAgent().setCurrentTarget(testClr.getCombatAgent());
		testClr.getCombatAgent().setTargetted(true);
		Character loneFtr = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		
		Square sq2 = new Square(1,1);
		sq2.setParentLocation(w);
		Square sq1 = new Square(1,0);
		sq2.setParentLocation(w);
		party.setLocation(sq2);
		testLoneClr.setLocation(sq1);
		testLoneClr.setMaxHp(8);
		testLoneClr.addHp(8, false);
		assertEquals(testLoneClr.getHp(), 8);

		assertEquals(CombatNeuronInputs.CLR_PERCENTAGE.getValue(testClr), 0.50, 0.0001);
		assertEquals(CombatNeuronInputs.CLR_PERCENTAGE.getValue(testLoneClr), 1.00, 0.0001);
		assertEquals(CombatNeuronInputs.CLR_PERCENTAGE.getValue(loneFtr), 0.00, 0.0001);
		
		assertEquals(CombatNeuronInputs.CR_DIFF.getValue(testClr), 0.20, 0.0001);
		assertEquals(CombatNeuronInputs.CR_DIFF.getValue(testLoneClr), 0.00, 0.0001);

		assertEquals(CombatNeuronInputs.MAGIC_LEFT.getValue(testClr), 0.00, 0.0001);
		assertEquals(CombatNeuronInputs.MAGIC_LEFT.getValue(testLoneClr), 1.00, 0.0001);
		(new Bless()).cast(testLoneClr, testLoneClr);
		assertEquals(CombatNeuronInputs.MAGIC_LEFT.getValue(testClr), 0.00, 0.0001);
		assertEquals(CombatNeuronInputs.MAGIC_LEFT.getValue(testLoneClr), 0.50, 0.0001);

		assertEquals(CombatNeuronInputs.TARGETTED.getValue(testClr), 1.00, 0.0001);
		assertEquals(CombatNeuronInputs.TARGETTED.getValue(testLoneClr), 0.00, 0.0001);

		assertEquals(CombatNeuronInputs.ODDS.getValue(testClr, goblin), 0.1, 0.0001);
		assertEquals(CombatNeuronInputs.ODDS.getValue(testLoneClr), 0.00, 0.0001);

		assertEquals(CombatNeuronInputs.WOUND_PARTY.getValue(testClr), 0.00, 0.0001);
		assertEquals(CombatNeuronInputs.WOUND_PARTY.getValue(testLoneClr), 0.00, 0.0001);
		testFtr.addHp(-5, false);
		assertTrue(CombatNeuronInputs.WOUND_PARTY.getValue(testClr) > 0.10);
		assertEquals(CombatNeuronInputs.WOUND_PARTY.getValue(testLoneClr), 0.00, 0.0001);

		assertEquals(CombatNeuronInputs.WOUND_US.getValue(testClr), 0.00, 0.0001);
		assertEquals(CombatNeuronInputs.WOUND_US.getValue(testLoneClr), 0.00, 0.0001);
		testLoneClr.addHp(-4, false);
		assertEquals(CombatNeuronInputs.WOUND_US.getValue(testClr), 0.00, 0.0001);
		assertEquals(CombatNeuronInputs.WOUND_US.getValue(testLoneClr), 0.50, 0.001);
	}
	
	@Test
	public void testIncorrectAction() {
		double testResult = 0.0;
		Iterator i = EnumSet.allOf(BasicActionsI.class).iterator();
		while(i.hasNext()) {
			testResult = decider.valueOption((ActionEnum)i.next(), testClr, testClr);
			assertEquals(testResult, -1.0, 0.00001);	// test error code thrown
		}
	}

	@Test
	public void testBrainSave() {

		File f = new File(baseDir + "\\Brains\\TEST_CBT1_CBT1.brain");
		if (f.exists())
			f.delete();

		assertFalse(f.exists());
		w.setName("TestWorld");
		Decider cd = testClr.getCombatDecider();
		((NeuralDecider)cd).saveBrain("TEST", baseDir + "\\Brains\\");

		assertTrue(f.exists());

	}

	@Test
	public void testBrainLoad() {

		testBrainSave();
		File f = new File(baseDir + "\\Brains\\TEST_CBT1_CBT1.brain");
		decider = NeuralDecider.createNeuralDecider(f);

		testClr.setCombatDecider(decider);
		testLoneClr.setCombatDecider(decider);

		checkLoneClrOptimism();
		testIncorrectAction();
		testGeneticVariables();
	}

	@Test
	public void testMultipleBrainLoad() {
		testBrainSave();

		ArrayList<NeuralDecider> brainArray = new ArrayList<NeuralDecider>();
		brainArray = LoadBrains.loadNN(new File(baseDir + "\\Brains"), LoadBrains.createBrainFilter("TEST_"), new World());

		assertTrue(brainArray.size() > 0);
		decider = brainArray.get(0);

		testClr.setCombatDecider(decider);
		testLoneClr.setCombatDecider(decider);

		testIncorrectAction();
		testGeneticVariables();	
	}

	@Test
	public void testNoWisdom() {
		testGeneticVariables();
		testLoneClr.setMaxMp(0);
		testLoneClr.addMp(-10);

		assertTrue(CombatActionsI.ATTACK.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testLoneClr));
		assertFalse(CombatActionsI.HEAL.isChooseable(testLoneClr));
		assertFalse(CombatActionsI.SPELL_ATTACK.isChooseable(testLoneClr));
		assertFalse(CombatActionsI.BUFF.isChooseable(testLoneClr));

		testClr.setMaxMp(0);
		testClr.addMp(-10);

		assertTrue(CombatActionsI.ATTACK.isChooseable(testClr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testClr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testClr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testClr));
		assertFalse(CombatActionsI.HEAL.isChooseable(testClr));
		assertFalse(CombatActionsI.SPELL_ATTACK.isChooseable(testClr));
		assertFalse(CombatActionsI.BUFF.isChooseable(testClr));
	}

	@Test
	public void testWisdomChoice() {
		testGeneticVariables();
		testLoneClr.setMaxMp(2);
		testLoneClr.addMp(2);

		assertTrue(CombatActionsI.ATTACK.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.HEAL.isChooseable(testLoneClr));
		assertFalse(CombatActionsI.SPELL_ATTACK.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.BUFF.isChooseable(testLoneClr));

		testClr.setMaxMp(2);
		testClr.addMp(2);
		assertTrue(CombatActionsI.ATTACK.isChooseable(testClr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testClr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testClr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testClr));
		assertTrue(CombatActionsI.HEAL.isChooseable(testClr));
		assertFalse(CombatActionsI.SPELL_ATTACK.isChooseable(testClr));
		assertTrue(CombatActionsI.BUFF.isChooseable(testClr));
	}

	@Test
	public void testHighLevelAttack() {
		testGeneticVariables();
		testLoneClr.setWisdom(new Attribute(14));
		testLoneClr.levelUp();
		testLoneClr.levelUp();
		testLoneClr.levelUp();
		testLoneClr.rest(1);
		testLoneClr.addHp(-2, false);
		assertTrue(testLoneClr.getMp() > 3);;

		assertTrue(CombatActionsI.ATTACK.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.HEAL.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.SPELL_ATTACK.isChooseable(testLoneClr));
		assertTrue(CombatActionsI.BUFF.isChooseable(testLoneClr));
	}

	@Test
	public void testFighterWithPotion() {
		testGeneticVariables();
		testFtr.addHp(100, false);

		assertTrue(CombatActionsI.ATTACK.isChooseable(testFtr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testFtr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testFtr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testFtr));
		assertFalse(CombatActionsI.HEAL.isChooseable(testFtr));
		assertFalse(CombatActionsI.HEAL_ITEM.isChooseable(testFtr));

		testFtr.addItem(Potion.CURE_LIGHT_WOUNDS);
		assertTrue(CombatActionsI.ATTACK.isChooseable(testFtr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testFtr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testFtr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testFtr));
		assertFalse(CombatActionsI.HEAL.isChooseable(testFtr));
		assertFalse(CombatActionsI.HEAL_ITEM.isChooseable(testFtr));

		testFtr.addHp(-5, false);
		assertTrue(CombatActionsI.ATTACK.isChooseable(testFtr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testFtr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testFtr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testFtr));
		assertFalse(CombatActionsI.HEAL.isChooseable(testFtr));
		assertTrue(CombatActionsI.HEAL_ITEM.isChooseable(testFtr));

		testFtr.removeItem(Potion.CURE_LIGHT_WOUNDS);
		assertTrue(CombatActionsI.ATTACK.isChooseable(testFtr));
		assertTrue(CombatActionsI.DEFEND.isChooseable(testFtr));
		assertTrue(CombatActionsI.RETREAT.isChooseable(testFtr));
		assertTrue(CombatActionsI.DISENGAGE.isChooseable(testFtr));
		assertFalse(CombatActionsI.HEAL.isChooseable(testFtr));
		assertFalse(CombatActionsI.HEAL_ITEM.isChooseable(testFtr));
	}

	@Test
	public void testIsChooseable() {
		Fight f = new Fight(party, new Character(Race.GOBLIN, CharacterClass.WARRIOR, w));
		decider = new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(TestActions.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class)));
		assertTrue(decider.decide(testClr, null) == null);

		decider = new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(PartyJoinActions.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class)));
		assertTrue(decider.decide(testClr, null) != null);
	}

	@Test
	public void testValidCrossDeciders() {
		NeuralDecider newDecider = new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputs.class)));

		NeuralDecider crossDecider = (NeuralDecider) decider.crossWith(newDecider);
		assertTrue(crossDecider != decider);
		assertTrue(crossDecider != newDecider);
		assertTrue(crossDecider != null);

		// and test that new brain is useable
		testLoneClr.setCombatDecider(crossDecider);
		checkLoneClrOptimism();
	}

	@Test
	public void testInvalidCrossDeciders() {
		NeuralDecider crossDecider = (NeuralDecider) decider.crossWith(decider);
		assertTrue(crossDecider == decider);

		NeuralDecider newDecider = new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CareerActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputs.class)));
		crossDecider = (NeuralDecider) decider.crossWith(newDecider);
		assertTrue(crossDecider == decider);
	}

	@After
	public void removeFiles() {
		File f = new File(baseDir + "\\Brains\\TEST_CBT1_CBT1.brain");

		if(f.exists())
			assertTrue(f.delete());
	}
}

enum TestActions implements ActionEnum {
	APPLY_TO_PARTY;

	public Action getAction(Agent a) {
		return getAction(a, a);
	}

	public boolean isChooseable(Agent a) {
		return false;
	}
	public Action getAction(Agent a1, Agent a2) {
		return new DoNothing(a1, 100);
	}

	public EnumSet<DnDAgentInteractionGeneticSet> getGeneticVariables() {
		return EnumSet.allOf(DnDAgentInteractionGeneticSet.class);
	}

	public String getChromosomeDesc() {
		return "TEST";
	}
	
	@Override
	public Enum<TestActions> getEnum() {
		return this;
	}

}