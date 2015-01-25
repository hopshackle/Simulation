package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;

public class BasicFarmTest {

	private BasicAgent testAgent;
	private World world;
	private HexMap<BasicHex> hexMap;
	private BasicHex plainsHex, forestHex, otherPlainsHex;
	
	@Before
	public void setUp() {
		world = new World();
		hexMap = new HexMap<BasicHex>(5, 5, BasicHex.getHexFactory());
		world.setLocationMap(hexMap);
		testAgent = new BasicAgent(world);
		plainsHex = hexMap.getHexAt(0, 0);
		otherPlainsHex = hexMap.getHexAt(0, 1);
		forestHex = hexMap.getHexAt(1, 1);
		forestHex.setTerrain(TerrainType.FOREST);
		testAgent.setLocation(plainsHex);
	}
	
	@Test
	public void onlyAbleToFarmWhenInAPlainsHexWithAHutThatYouOwn() {
		assertFalse(BasicActions.FARM.isChooseable(testAgent));
		new Hut(testAgent);
		assertTrue(BasicActions.FARM.isChooseable(testAgent));
		testAgent.setLocation(otherPlainsHex);
		assertFalse(BasicActions.FARM.isChooseable(testAgent));
		
		BasicAgent otherAgent = new BasicAgent(world);
		otherAgent.setLocation(otherPlainsHex);
		new Hut(otherAgent);
		assertFalse(BasicActions.FARM.isChooseable(testAgent));
		assertTrue(BasicActions.FARM.isChooseable(otherAgent));
		new Village(otherPlainsHex);
		assertTrue(BasicActions.FARM.isChooseable(otherAgent));
		
		testAgent.setLocation(forestHex);
		new Hut(testAgent);
		assertFalse(BasicActions.FARM.isChooseable(testAgent));
	}
	
	@Test
	public void farmingProvidesMoreFoodAndDoesNotAffectCarryingCapacity() {
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(plainsHex.getCarryingCapacity(), 10);
		new BuildHut(testAgent).doStuff();
		assertEquals(plainsHex.getCarryingCapacity(), 9);
		Action farmAction = new Farm(testAgent);
		farmAction.run();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 2);
		assertEquals(plainsHex.getCarryingCapacity(), 9);
	}
	
	@Test
	public void carryingCapacityDoesNotAffectAbilityToFarm() {
		plainsHex.changeCarryingCapacity(-20);
		new Hut(testAgent);
		assertTrue(BasicActions.FARM.isChooseable(testAgent));
	}	
}
