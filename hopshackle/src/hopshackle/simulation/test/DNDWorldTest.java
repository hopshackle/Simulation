package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import org.junit.*;
public class DNDWorldTest {
	
	private World world;
	private DNDWorld dw;
	private Character ftr1, ftr2, ftr3, clr1, clr2, exp1;

	@Before
	public void setUp() throws Exception {
		world = new World();
		ftr1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, world);
		ftr2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, world);
		ftr3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, world);
		clr1 = new Character(Race.HUMAN, CharacterClass.CLERIC, world);
		clr2 = new Character(Race.HUMAN, CharacterClass.CLERIC, world);

		ftr1.setLocation(world);
		ftr2.setLocation(world);
		ftr3.setLocation(world);
		clr1.setLocation(world);
		clr2.setLocation(world);
	}

	@Test
	public void testBasicDNDWorld() {
		dw = DNDWorld.getDNDWorld(world);
		assertEquals(dw.getPercentagePop(CharacterClass.FIGHTER), 0.6, 0.001);
		assertEquals(dw.getPercentagePop(CharacterClass.CLERIC), 0.4, 0.001);
		assertEquals(dw.getPercentagePop(CharacterClass.EXPERT), 0.0, 0.001);
		
		exp1 = new Character(Race.HUMAN, CharacterClass.EXPERT, world);
		exp1.setLocation(world);
		
		world.maintenance();
		
		assertEquals(dw.getPercentagePop(CharacterClass.FIGHTER), 0.5, 0.001);
		assertEquals(dw.getPercentagePop(CharacterClass.CLERIC), 0.3333333, 0.001);
		assertEquals(dw.getPercentagePop(CharacterClass.EXPERT), 0.1666666, 0.001);
	}
	
	@Test
	public void testWorldDeath() {
		testBasicDNDWorld();
		
		DNDWorld newDW = DNDWorld.getDNDWorld(world);
		assertTrue(newDW == dw);
		
		Agent a = new BasicAgent(world);
		Location sodom = new Location(world);
		a.setLocation(sodom);
	
		assertFalse(a.isDead());
		
		world.worldDeath();
		
		assertTrue(a.isDead());
		
		newDW = DNDWorld.getDNDWorld(world);
		assertFalse(newDW == dw);

	}
}
