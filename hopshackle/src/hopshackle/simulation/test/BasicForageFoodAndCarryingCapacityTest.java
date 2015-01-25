package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;

public class BasicForageFoodAndCarryingCapacityTest {

	private BasicAgent testAgent;
	private HexMap<BasicHex> hexMap;
	BasicHex forestHex, plainsHex, hillsHex;
	private World w;
	private FastCalendar stasisCalendar;
	
	@Before 
	public void setUp() {
		w = new World();
		stasisCalendar = new FastCalendar(0);
		stasisCalendar.setTime(0);
		w.setCalendar(stasisCalendar);
		hexMap = new HexMap<BasicHex>(5,5, BasicHex.getHexFactory());
		w.setLocationMap(hexMap);
		forestHex = hexMap.getHexAt(0, 0);
		hillsHex = hexMap.getHexAt(0, 1);
		plainsHex = hexMap.getHexAt(0, 2);
		forestHex.setTerrain(TerrainType.FOREST);
		hillsHex.setTerrain(TerrainType.HILLS);
		testAgent = new BasicAgent(w);
	}
	
	@Test
	public void chooseableInForestAndPlainsOnly() {
		testAgent.setLocation(forestHex);
		assertTrue(BasicActions.FORAGE.isChooseable(testAgent));
		testAgent.setLocation(hillsHex);
		assertFalse(BasicActions.FORAGE.isChooseable(testAgent));
		testAgent.setLocation(plainsHex);
		assertTrue(BasicActions.FORAGE.isChooseable(testAgent));
	}
	
	@Test
	public void forageInForest() {
		int startingCapacity = forestHex.getCarryingCapacity();
		testAgent.setLocation(forestHex);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 0);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		Forage testForage = new Forage(testAgent);
		testForage.run();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 1);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		int newCapacity = forestHex.getCarryingCapacity();
		assertEquals(startingCapacity - newCapacity, 1);
	}
	
	@Test
	public void forageInPlains() {
		int startingCapacity = plainsHex.getCarryingCapacity();
		testAgent.setLocation(plainsHex);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 0);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		Forage testForage = new Forage(testAgent);
		testForage.run();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 0);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 1);
		int newCapacity = plainsHex.getCarryingCapacity();
		assertEquals(startingCapacity - newCapacity, 1);
	}
	
	@Test
	public void forageWithExhaustedCarryingCapacity() {
		plainsHex.changeCarryingCapacity(-plainsHex.getCarryingCapacity());
		assertEquals(plainsHex.getCarryingCapacity(), 0);
		testAgent.setLocation(plainsHex);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 0);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		Forage testForage = new Forage(testAgent);
		testForage.run();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.WOOD), 0);
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(plainsHex.getCarryingCapacity(), 0);
		assertFalse(BasicActions.FORAGE.isChooseable(testAgent));
	}
	
	@Test
	public void forageLastPieceOfWoodFromForestChangesTerrainToPlains() {
		forestHex.changeCarryingCapacity(-forestHex.getCarryingCapacity());
		assertEquals(forestHex.getCarryingCapacity(), 10);
		assertTrue(forestHex.getTerrainType() == TerrainType.PLAINS);
	}
	
	@Test
	public void recoveryOfCarryingCapacityOverTime() {
		plainsHex.changeCarryingCapacity(-plainsHex.getCarryingCapacity());
		assertEquals(plainsHex.getCarryingCapacity(), 0);
		stasisCalendar.setTime(4995);
		assertEquals(plainsHex.getCarryingCapacity(), 0);
		stasisCalendar.setTime(5005);
		assertEquals(plainsHex.getCarryingCapacity(), 1);
	}
	
	@Test
	public void agentConsumesFoodOverTimeWithNoLossOfHealth() {
		testAgent.addItem(Resource.FOOD);
		testAgent.addItem(Resource.FOOD);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 2);
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
		stasisCalendar.setTime(9995);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 2);
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
		stasisCalendar.setTime(10005);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 1);
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
	}
	
	@Test
	public void agentLosesHealthWhenThereIsNoFood() {
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
		stasisCalendar.setTime(9995);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
		stasisCalendar.setTime(10005);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(testAgent.getHealth(), 15.0, 0.001);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(testAgent.getHealth(), 15.0, 0.001);
	}
	
	@Test
	public void agentConsumesFoodToHealDuringMaintenance() {
		testAgent.addHealth(-5.0);
		testAgent.addItem(Resource.FOOD);
		testAgent.addItem(Resource.FOOD);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 1);
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
		stasisCalendar.setTime(9995);
		testAgent.addHealth(-3.0);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 1);
		assertEquals(testAgent.getHealth(), 17.0, 0.001);
		stasisCalendar.setTime(10005);
		testAgent.maintenance();
		assertEquals(testAgent.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(testAgent.getHealth(), 17.0, 0.001);
	}
	
}
