package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;
import hopshackle.simulation.dnd.genetics.*;

import java.util.*;

import org.junit.*;

public class PartyTest {

	World w;
	Character ftr, ftr2, ftr3;
	Square sq;
	Party p;
	StateDecider stayDecider, leaveDecider;
	
	@Before
	public void setUp() throws Exception {
		w = new World();
		SimProperties.setProperty("StateBaseValue", "10000");
		SimProperties.setProperty("Alpha", "0.02");
		SimProperties.setProperty("Gamma", "0.98");
		ftr = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		sq = new Square(0, 0);

		ftr.setLocation(sq);
		sq.setParentLocation(w);
		ftr2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		ftr2.setLocation(sq);
		
		ftr3 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		ftr3.setLocation(sq);

		stayDecider = new StateDecider(null, null);
		leaveDecider = new StateDecider(null, null);
		ArrayList<ActionEnum> actionList = new ArrayList<ActionEnum>();
		actionList.add(BasicActionsI.STAY);
		actionList.add(BasicActionsI.REST);
		ArrayList<GeneticVariable> variableList = new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class));
		variableList.remove(GeneticEnum.AGE);
		variableList.remove(GeneticEnum.AC);
		variableList.remove(GeneticEnum.DMG);
		stayDecider.setActions(actionList);
		stayDecider.setVariables(variableList);
		stayDecider.setTeacher(new AgentTeacher());
		
		actionList.remove(BasicActionsI.STAY);
		actionList.add(BasicActionsI.LEAVE);
		leaveDecider.setActions(actionList);
		leaveDecider.setVariables(variableList);
		leaveDecider.setTeacher(new AgentTeacher());

		
		ftr.setDecider(stayDecider);
		ftr2.setDecider(stayDecider);
		ftr3.setDecider(stayDecider);
		p = new Party(ftr);
		assertFalse(p.isDead());
		p.addMember(ftr2);
		
		leaveDecider.setStateType("PTEST1");
		stayDecider.setStateType("PTEST1");
	}
	
	@After
	public void tearDown() {
		HopshackleState.clear();
	}
	
	@Test
	public void testBasicDecision() {
		/*
		 *  Set up a Party with a Leader and then run through three decisions, before the
		 *  party expires.
		 *  At each stage we want to make sure that a relevant decision is taken, and that the learning callback is correct
		 *  (We check this seeing how often the State has been visited)
		 */

		HopshackleState ftrState = stayDecider.getState(ftr, null);
		HopshackleState ftr2State = stayDecider.getState(ftr2, null);
		HopshackleState partyState = stayDecider.getState(p, ftr);

		assertTrue(ftrState != partyState);
		assertTrue(ftrState != ftr2State);
		assertEquals(ftrState.getValue(), 10000, 1);
		assertEquals(ftrState.getVisited(), 0);
		oneAction();
		assertEquals(ftrState.getVisited(), 1);
		assertEquals(partyState.getVisited(), 0);
		assertEquals(ftr2State.getVisited(), 1);
		
		oneAction();
		assertEquals(ftrState.getVisited(), 2);
		assertEquals(partyState.getVisited(), 1);
		assertEquals(ftr2State.getVisited(), 2);
		
		ftr.setDecider(leaveDecider);
		ftr.purgeActions();
		Action leave = ftr.decide();
		assertEquals(ftrState.getVisited(), 3);
		leave.run(); // this does *not* call decide()
		assertEquals(ftrState.getVisited(), 3);
		assertEquals(partyState.getVisited(), 1);
		assertEquals(ftr2State.getVisited(), 2);

		assertFalse(p.isDisbanded());
		p.maintenance();
		assertTrue(p.isDisbanded());
		assertTrue(stayDecider.getState(p, p).toString().equals("PTEST1:DEAD"));
		
		assertEquals(ftrState.getVisited(), 4);
		assertEquals(partyState.getVisited(), 2);
		assertEquals(ftr2State.getVisited(), 3);
		
		ftrState = leaveDecider.getState(ftr, null);
		ftr2State = stayDecider.getState(ftr2, null);
		
		assertTrue(ftrState == ftr2State);

	}
	
	private void oneAction() {
		/*
		 *  This will for each of p, ftr, ftr2 execute decide(), 
		 *  then run the action. This act of running the action will
		 *  cause decide() to be re-invoked; but with no impact given that
		 *  there is no actionQueue set up to receive the actions.
		 *  
		 *  The net result is that on each of the three agent, decide() (and hence teach) is run twice.
		 */
		
		Action nextAction;
		nextAction = p.getNextAction();
		assertTrue(nextAction instanceof Rest);
		nextAction.run();
		
		nextAction = ftr.getNextAction();
		if (nextAction == null)
			nextAction = ftr.decide();
		assertTrue(nextAction instanceof DoNothing);
		nextAction.run();
		nextAction = ftr2.getNextAction();
		if (nextAction == null)
			nextAction = ftr2.decide();
		assertTrue(nextAction instanceof DoNothing);
		nextAction.run();

	}
	
	@Test
	public void testLeaderChange() {
		/*
		 *  Set up a Party with a Leader, make a decision, change the Leader, make another decision, disband party
		 *  At each stage we want to make sure that a relevant decision is taken, and that the learning callback is correct
		 *  (We check this seeing how often the State has been visited)
		 */
		
		oneAction();
		HopshackleState partyState = stayDecider.getState(p, null);
		HopshackleState deadState = HopshackleState.getState("PTEST1:DEAD");
		HopshackleState ftrState = stayDecider.getState(ftr, null);
		HopshackleState ftr2State = stayDecider.getState(ftr2, null);
		HopshackleState ftr3State = stayDecider.getState(ftr3, null);
		
		assertEquals(partyState.getCountOfNextState(BasicActionsI.REST, deadState), 0);

		p.addMember(ftr3);
		Action nextAction = ftr3.decide();
		assertTrue(nextAction instanceof DoNothing);
		nextAction.run();
		
		ftr.setDecider(leaveDecider);
		
		p.getNextAction().run();
		ftr.getNextAction().run();
		ftr2.getNextAction().run();
		ftr3.getNextAction().run();

		assertFalse(p.isDisbanded());
		assertEquals(partyState.getValue(), 9996, 1);
		assertEquals(partyState.getVisited(), 1);
		assertEquals(partyState.getCountOfNextState(BasicActionsI.REST, deadState), 0);
		assertEquals(partyState.getCountOfNextState(BasicActionsI.REST, partyState), 1);
		
		ftr.getNextAction().run();
		assertFalse(p.isDisbanded());
		assertTrue(p.getLeader() == ftr);
		p.maintenance();
		assertFalse(p.isDisbanded());
		assertFalse(p.getLeader() == ftr);
		// and check that we visited 'DEAD' state given change of leader
		assertEquals(partyState.getCountOfNextState(BasicActionsI.REST, deadState), 1);
		assertEquals(partyState.getCountOfNextState(BasicActionsI.REST, partyState), 1);
		assertEquals(partyState.getValue(), 9796, 1);
		// Having visited Death, value will be reduced by 2% (Alpha setting)
		assertEquals(partyState.getVisited(), 2);
		
		partyState = stayDecider.getState(p, null);
		
		assertTrue(p.getLeader() == ftr2);

		assertTrue(p.getNextAction() instanceof Rest);
		assertTrue(ftr.getNextAction() instanceof Rest);
		assertTrue(ftr2.getNextAction() instanceof DoNothing);
		assertTrue(ftr3.getNextAction() instanceof DoNothing);
		
		assertTrue(partyState == stayDecider.getState(p, null));
		assertTrue(ftrState ==  stayDecider.getState(ftr2, null));
		assertTrue(ftr2State ==  stayDecider.getState(ftr3, null));
		assertTrue(ftr3State ==  stayDecider.getState(ftr, null));
		
	}
	
	@Test
	public void testDoubleLeaderChange() {
		/*
		 *  Change leaders twice before making a decision
		 */
		
		HopshackleState ftr3State = stayDecider.getState(ftr3, null);
		
		testLeaderChange();
		
		HopshackleState partyState = stayDecider.getState(p, null);
		HopshackleState deadState = HopshackleState.getState("PTEST1:DEAD");

		assertTrue(p.getLeader() == ftr2);
		
		ftr2.die("test");
	
		assertEquals(partyState.getCountOfNextState(BasicActionsI.REST, deadState), 1);
		assertEquals(partyState.getCountOfNextState(BasicActionsI.REST, partyState), 1);
		
		assertTrue(p.getLeader() == ftr3);
		assertTrue(!p.isDead());
		
		Action nextAction = p.getNextAction();
		nextAction.run();
		assertTrue(p.getNextAction() == null);

		assertTrue(ftr.getNextAction() instanceof Rest);
		assertTrue(ftr3.getNextAction() instanceof Rest);

		assertTrue(p.isDead());
	
		HopshackleState newftr3State = stayDecider.getState(ftr3, null);
		HopshackleState ftrState = stayDecider.getState(ftr, null);
		
		assertTrue(ftr3State == newftr3State);	
		assertTrue(ftr3State == ftrState);		
	}

}
