package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class MCTreeTest {


	List<TestActionEnum> allActions = new ArrayList<TestActionEnum>(EnumSet.allOf(TestActionEnum.class));
	List<TestActionEnum> leftRightOnly = new ArrayList<TestActionEnum>(EnumSet.allOf(TestActionEnum.class));
	MonteCarloTree<TestActionEnum> tree;

	@Before 
	public void setup() {
		leftRightOnly.remove(TestActionEnum.TEST);
		SimProperties.setProperty("MonteCarloUCTC", "1");
	}
	
	@Test
	public void settingUpdatesLeft() {
		tree = new MonteCarloTree<TestActionEnum>();
		assertEquals(tree.updatesLeft(), 0);
		assertFalse(tree.containsState("TEST"));
		tree.setUpdatesLeft(1);
		assertEquals(tree.updatesLeft(), 1);
	}
	
	@Test
	public void addANode() {
		tree = new MonteCarloTree<TestActionEnum>();
		tree.setUpdatesLeft(1);
		tree.insertState("TEST", leftRightOnly);
		assertTrue(tree.containsState("TEST"));
		assertFalse(tree.containsState("OTHER"));
		assertEquals(tree.updatesLeft(), 0);
	}
	
	@Test
	public void seriesOfActionsFromSameStateThroughUCTLogic() {
		// This uses the same numeric test case as in MCStatisticsTest (but one level higher in 
		// the object hierarchy)
		tree = new MonteCarloTree<TestActionEnum>();
		tree.insertState("START", leftRightOnly);
		TestActionEnum next = tree.getNextAction("START");
		boolean firstLeft = false;
		if (next == TestActionEnum.LEFT) 
			firstLeft = true;
		if (firstLeft) 
			tree.updateState("START", TestActionEnum.LEFT, 2.0);
		else
			tree.updateState("START", TestActionEnum.RIGHT, 1.0);
		next = tree.getNextAction("START");
		if (firstLeft) {
			assertTrue(next == TestActionEnum.RIGHT);
			tree.updateState("START", TestActionEnum.RIGHT, 1.0);
		} else {
			assertTrue(next == TestActionEnum.LEFT);
			tree.updateState("START", TestActionEnum.LEFT, 2.0);
		}
		
		next = tree.getNextAction("START");
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState("START", TestActionEnum.LEFT, 2.0);
		next = tree.getNextAction("START");
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState("START", TestActionEnum.LEFT, 0.5);
		next = tree.getNextAction("START");
		assertTrue(next == TestActionEnum.LEFT);
		tree.updateState("START", TestActionEnum.LEFT, 1.5);
		next = tree.getNextAction("START");
		assertTrue(next == TestActionEnum.RIGHT);
		tree.updateState("START", TestActionEnum.RIGHT, 1.0);
		next = tree.getNextAction("START");
		assertTrue(next == TestActionEnum.LEFT);
		
	}
	
}
