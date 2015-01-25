package hopshackle.simulation.test;

import static org.junit.Assert.assertEquals;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.TradeDecider;

import java.io.File;

import org.junit.*;
public class TradeDeciderValuationTest {

	private Character testFighter;
	private World w;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	
	@Before
	public void setup() {
		w = new World();
		new Market(new File(baseDir + "\\logs\\Market_test.txt"), w);
		// normally the market thread will be runnning - here it won't be
		CharacterClass.FIGHTER.setDefaultTradeDecider(new TradeDecider());
		testFighter = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		testFighter.setGenome(new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_FTR.txt")));
		testFighter.setDexterity(new Attribute(10));
		testFighter.setStrength(new Attribute(12));
		testFighter.addGold(100);	
	}
	
	@Test
	public void priceOfArmour() {
		double leatherArmour = testFighter.getValue(Armour.LEATHER_ARMOUR);
		assertEquals(leatherArmour, 40, 0.01);
		// 100 gold = 20, +2 AC = 20
	}
	
	@Test
	public void priceOfShield() {
		double smallShield = testFighter.getValue(Shield.SMALL_SHIELD);
		assertEquals(smallShield, 20, 0.01);
		// 100 gold = 10; +1 AC = 10
	}
	
	@Test
	public void priceOfCureLightWounds() {
		double cureLightPotion = testFighter.getValue(Potion.CURE_LIGHT_WOUNDS);
		assertEquals(cureLightPotion, 2.75, 0.01);
		// 0.50 per hit point and average of 5.5
	}
	
	
	@Test
	public void priceOfWeapon() {
		testFighter.addItem(Weapon.SHORT_SWORD);
		CombatAgent ftr = testFighter.getCombatAgent();
		double dmgDifference = ftr.getAvgDmgPerRound(ftr, Weapon.LONG_SWORD) - ftr.getAvgDmgPerRound(ftr, Weapon.SHORT_SWORD);
		
		double longSword = testFighter.getValue(Weapon.LONG_SWORD);
		assertEquals(longSword, dmgDifference * (5.0 + 10.0), 0.01);
		// 5 plus 10% of gold per point of damage
		
		assertEquals(testFighter.getValue(Weapon.LIGHT_MACE), 0.00, 0.0001);
	}
	
	@Test
	public void priceOfMagicWeapon() {
		testFighter.addItem(Weapon.LONG_SWORD);
		CombatAgent ftr = testFighter.getCombatAgent();
		double dmgDifference = ftr.getAvgDmgPerRound(ftr, Weapon.LONG_SWORD_PLUS_1) - ftr.getAvgDmgPerRound(ftr, Weapon.LONG_SWORD);
		
		double longSword = testFighter.getValue(Weapon.LONG_SWORD_PLUS_1);
		assertEquals(longSword, dmgDifference * (5.0 + 10.0) + 50, 0.01);
		// 5 plus 10% of gold per point of damage; plus 50 for the magic
		
		testFighter.addItem(Weapon.LONG_SWORD_PLUS_1);
		assertEquals(testFighter.getValue(Weapon.LONG_SWORD_PLUS_1), 0.00, 0.01);
	}

}
