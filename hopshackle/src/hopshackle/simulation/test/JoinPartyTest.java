package hopshackle.simulation.test;

import static org.junit.Assert.assertTrue;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;
import hopshackle.simulation.dnd.genetics.BasicActionsI;

import org.junit.*;
public class JoinPartyTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testActionChosen() {
		// check that an action is chosen 
		World w = new World();

		Square here = new Square(0,0);
		here.setParentLocation(w);
		ActionProcessor ap = new ActionProcessor("test", false);

		for (int n=0; n<10; n++) {
			Character fighter = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
			fighter.setDecider(new HardCodedDecider(BasicActionsI.JOIN_PARTY));
			fighter.setLocation(here);

			Action next = fighter.decide();
			assertTrue(next instanceof JoinParty);

			w.setActionProcessor(ap);

			next.run();

			next = ap.getNextUndeletedAction();
			assertTrue(next instanceof JoinParty || next instanceof Adventure);
			fighter.die("Test");
		}

	}

}
