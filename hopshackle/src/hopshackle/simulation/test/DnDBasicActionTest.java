package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.BasicActionsI;

import org.junit.*;
public class DnDBasicActionTest {

	private Character ftr, ftr2;
	private World w;
	private Square sq1, sq0, sq10;
	@Before
	public void setUp() throws Exception {
		w = new World();
		ftr = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		sq1 = new Square(0, 1);
		sq0 = new Square(0, 0);
		sq10 = new Square(0, 10);
		
		ftr.setLocation(sq1);
		sq1.setParentLocation(w);
		sq10.setParentLocation(w);
		sq0.setParentLocation(w);
		ftr2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
	}

	@Test
	public void testNormal(){
		ftr2.setLocation(sq1);
		
		// all actions should be available
		assertTrue(BasicActionsI.ADVENTURE.isChooseable(ftr));
		assertTrue(BasicActionsI.MOVE_HIGH_CR.isChooseable(ftr));
		assertTrue(BasicActionsI.MOVE_LOW_CR.isChooseable(ftr));
		assertTrue(BasicActionsI.MOVE_RANDOM.isChooseable(ftr));
		assertTrue(BasicActionsI.REST.isChooseable(ftr));
		assertTrue(BasicActionsI.JOIN_PARTY.isChooseable(ftr));
		assertFalse(BasicActionsI.STAY.isChooseable(ftr));
		assertFalse(BasicActionsI.LEAVE.isChooseable(ftr));
		assertFalse(BasicActionsI.CHALLENGE.isChooseable(ftr));
		
	}
	
	@Test
	public void testCRZero(){
		ftr.setLocation(sq0);
		
		assertFalse(BasicActionsI.ADVENTURE.isChooseable(ftr));
		assertTrue(BasicActionsI.MOVE_HIGH_CR.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_LOW_CR.isChooseable(ftr));
		assertTrue(BasicActionsI.MOVE_RANDOM.isChooseable(ftr));
		assertTrue(BasicActionsI.REST.isChooseable(ftr));
		assertTrue(BasicActionsI.JOIN_PARTY.isChooseable(ftr));
		assertFalse(BasicActionsI.STAY.isChooseable(ftr));
		assertFalse(BasicActionsI.LEAVE.isChooseable(ftr));
		assertFalse(BasicActionsI.CHALLENGE.isChooseable(ftr));
	}
	
	@Test
	public void testCRTen(){
		ftr.setLocation(sq10);
		
		assertTrue(BasicActionsI.ADVENTURE.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_HIGH_CR.isChooseable(ftr));
		assertTrue(BasicActionsI.MOVE_LOW_CR.isChooseable(ftr));
		assertTrue(BasicActionsI.MOVE_RANDOM.isChooseable(ftr));
		assertTrue(BasicActionsI.REST.isChooseable(ftr));
		assertTrue(BasicActionsI.JOIN_PARTY.isChooseable(ftr));
		assertFalse(BasicActionsI.STAY.isChooseable(ftr));
		assertFalse(BasicActionsI.LEAVE.isChooseable(ftr));
		assertFalse(BasicActionsI.CHALLENGE.isChooseable(ftr));
	}
	
	@Test
	public void testInParty(){
		ftr.setLocation(sq1);
		ftr2.setLocation(sq1);
		Party p = new Party(ftr2);
		p.addMember(ftr);
		
		assertFalse(BasicActionsI.ADVENTURE.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_HIGH_CR.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_LOW_CR.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_RANDOM.isChooseable(ftr));
		assertFalse(BasicActionsI.REST.isChooseable(ftr));
		assertFalse(BasicActionsI.JOIN_PARTY.isChooseable(ftr));
		assertTrue(BasicActionsI.STAY.isChooseable(ftr));
		assertTrue(BasicActionsI.LEAVE.isChooseable(ftr));
		assertTrue(BasicActionsI.CHALLENGE.isChooseable(ftr));
	}
	
	@Test
	public void testLeader(){
		ftr.setLocation(sq1);
		ftr2.setLocation(sq1);
		Party p = new Party(ftr);
		p.addMember(ftr2);
		
		assertFalse(BasicActionsI.ADVENTURE.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_HIGH_CR.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_LOW_CR.isChooseable(ftr));
		assertFalse(BasicActionsI.MOVE_RANDOM.isChooseable(ftr));
		assertFalse(BasicActionsI.REST.isChooseable(ftr));
		assertFalse(BasicActionsI.JOIN_PARTY.isChooseable(ftr));
		assertTrue(BasicActionsI.STAY.isChooseable(ftr));
		assertTrue(BasicActionsI.LEAVE.isChooseable(ftr));
		assertFalse(BasicActionsI.CHALLENGE.isChooseable(ftr));
	}
	
	@Test
	public void testParty() {
		ftr.setLocation(sq1);
		ftr2.setLocation(sq1);
		Party p = new Party(ftr);
		p.addMember(ftr2);
		
		assertTrue(BasicActionsI.ADVENTURE.isChooseable(p));
		assertTrue(BasicActionsI.MOVE_HIGH_CR.isChooseable(p));
		assertTrue(BasicActionsI.MOVE_LOW_CR.isChooseable(p));
		assertTrue(BasicActionsI.MOVE_RANDOM.isChooseable(p));
		assertTrue(BasicActionsI.REST.isChooseable(p));
		assertFalse(BasicActionsI.JOIN_PARTY.isChooseable(p));
		assertFalse(BasicActionsI.STAY.isChooseable(p));
		assertFalse(BasicActionsI.LEAVE.isChooseable(p));
		assertFalse(BasicActionsI.CHALLENGE.isChooseable(p));
	}

}
