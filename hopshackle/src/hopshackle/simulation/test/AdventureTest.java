package hopshackle.simulation.test;

import static org.junit.Assert.assertTrue;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.Adventure;
import hopshackle.simulation.dnd.genetics.BasicActionsI;

import org.junit.*;
public class AdventureTest {

	World w;
	Character ftr, ftr2;
	Square sq;
	Party p;

	@Before
	public void setUp() throws Exception {
		w = new World();
		ftr = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		sq = new Square(0, 1);

		ftr.setLocation(sq);
		sq.setParentLocation(w);
		ftr2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		ftr2.setLocation(sq);

		ftr.setCombatDecider(TestFight.alwaysAttack);
		ftr2.setCombatDecider(TestFight.alwaysAttack);

		ftr.levelUp();
		ftr2.levelUp();
		ftr.levelUp();
		ftr2.levelUp();

		ftr.addItem(Weapon.BASTARD_SWORD);
		ftr2.addItem(Weapon.BASTARD_SWORD);
		ftr.addItem(Armour.CHAIN_SHIRT);
		ftr2.addItem(Armour.CHAIN_MAIL);
		ftr.addItem(Shield.LARGE_SHIELD);
		ftr2.addItem(Shield.LARGE_SHIELD);

		p = new Party(ftr);
		p.addMember(ftr2);
		p.setDecider(new HardCodedDecider(BasicActionsI.ADVENTURE));
	}

	@Test
	public void testOrcHeartsDropped() {
		for (int n = 0; n < 20; n++) {
			haveAdventure(p);
			if (ftr.getInventory().contains(Component.ORC_HEART))
				return;
			if (ftr2.getInventory().contains(Component.ORC_HEART))
				return;
			ftr.addHp(100, false);
			ftr2.addHp(100, false);
		}
		assertTrue(false);
	}

	private void haveAdventure(DnDAgent p) {
		Adventure a = new Adventure(p, false);
		a.run();
	}
}


