package hopshackle.simulation.test;
import static org.junit.Assert.assertTrue;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;
import hopshackle.simulation.dnd.genetics.BasicActionsI;

import org.junit.*;

public class MoveTest {
	
	World w;
	Character ftr, ftr2;
	Square sq1, sq2;
	Party p;
	
	@Before
	public void setUp() throws Exception {
		w = new World();
		ftr = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		ftr.setStrength(new Attribute(16));
		ftr.setDexterity(new Attribute(16));
		sq1 = new Square(0, 0);
		sq2 = new Square(0, 1);
		sq1.addAccessibleLocation(sq2);
		sq2.addAccessibleLocation(sq1);
		sq1.setParentLocation(w);
		sq2.setParentLocation(w);

		ftr.setLocation(sq1);

		ftr.setCombatDecider(TestFight.alwaysAttack);

		ftr.levelUp();
		ftr.levelUp();
		ftr.maintenance();

		ftr.addItem(Weapon.BASTARD_SWORD);
		ftr.addItem(Armour.CHAIN_SHIRT);
		ftr.addItem(Shield.LARGE_SHIELD);
		
		ftr.setDecider(new HardCodedDecider(BasicActionsI.MOVE_RANDOM));
	}

	@Test
	public void testBasicMove() {
		assertTrue(ftr.getLocation() == sq1);
		Action nextAction = ftr.decide();
		assertTrue(nextAction instanceof Move);
		
		nextAction.run();
		assertTrue(ftr.getLocation() == sq2);
	}
	
	@Test
	public void testWanderingMonsters() {
		// We try 10 Moves from sq1
		// At least one should have an Adventure
		
		// Then repeat from sq2 - and should not encounter any monsters
		ActionProcessor ap = new ActionProcessor("test", false);
		w.setActionProcessor(ap);

		boolean monsterEncountered = false;
		Action nextAction;
		ftr.setLocation(sq1);
		Action firstAction = ftr.decide();
		firstAction.run();
		for (int loop=0; loop<20 && !ftr.isDead(); loop++) {
			nextAction = ap.getNextUndeletedAction();
			if (ftr.getLocation() == sq1) {
				assertTrue(nextAction instanceof Move);
			} else {
				if (nextAction instanceof Adventure) {
					monsterEncountered = true;
					break;
				}
			}
			nextAction.run();
			ftr.addHp(20, false);
		}
		assertTrue(monsterEncountered);
	}
	
}
