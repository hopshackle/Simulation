package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;
public class BasicVariablesTest {

	private BasicAgent testAgent, testAgent2;
	private HexMap<BasicHex> hexMap;
	private World w;

	@Before
	public void setUp() {
		w = new World();
		hexMap = new HexMap<BasicHex>(3, 3, BasicHex.getHexFactory());
		setTerrainAtHex(hexMap, 0, 0, TerrainType.OCEAN);
		setTerrainAtHex(hexMap, 0, 1, TerrainType.FOREST);
		w.setLocationMap(hexMap);
		testAgent = new BasicAgent(w);
		testAgent2 = new BasicAgent(w);
	}

	@Test
	public void healthTest() {
		assertEquals(BasicVariables.HEALTH.getValue(testAgent), 1.0, 0.0001);
		testAgent.addHealth(-5);
		assertEquals(BasicVariables.HEALTH.getValue(testAgent), 0.75, 0.0001);
		testAgent.addHealth(-15);
		assertEquals(BasicVariables.HEALTH.getValue(testAgent), 0.00, 0.0001);
		assertTrue(testAgent.isDead());
	}

	@Test
	public void terrainTests() {
		testAgent.setLocation(hexMap.getHexAt(0, 1));
		testAgent.getMapKnowledge().addLocationMap(hexMap);

		assertEquals(BasicVariables.FOREST.getValue(testAgent), 1.0, 0.0001);
		assertEquals(BasicVariables.WATER.getValue(testAgent), 1.0, 0.0001);
		assertEquals(BasicVariables.PLAINS.getValue(testAgent), 0.95, 0.0001);

		testAgent.setLocation(hexMap.getHexAt(2, 2));
		assertEquals(BasicVariables.FOREST.getValue(testAgent), 0.80, 0.0001);
		assertEquals(BasicVariables.WATER.getValue(testAgent), 0.40, 0.0001);
		assertEquals(BasicVariables.PLAINS.getValue(testAgent), 1.0, 0.0001);
	}
	
	@Test
	public void findHut() {
		setTerrainAtHex(hexMap, 0, 1, TerrainType.PLAINS);
		setTerrainAtHex(hexMap, 0, 0, TerrainType.PLAINS);
		testAgent.getMapKnowledge().addLocationMap(hexMap);
		testAgent.setLocation(hexMap.getHexAt(2, 2));
		assertEquals(BasicVariables.OWNS_HUT.getValue(testAgent), 0.0, 0.001);
		new Hut(testAgent);
		assertEquals(BasicVariables.OWNS_HUT.getValue(testAgent), 1.0, 0.001);
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 1.0, 0.001);
		testAgent.setLocation(hexMap.getHexAt(2, 1));
		assertEquals(BasicVariables.OWNS_HUT.getValue(testAgent), 1.0, 0.001);
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.95, 0.001);
		testAgent.setLocation(hexMap.getHexAt(2, 0));
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.9, 0.001);
		testAgent.setLocation(hexMap.getHexAt(0, 2));
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.9, 0.001);
		testAgent.setLocation(hexMap.getHexAt(0, 1));
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.85, 0.001);
		
		new Hut(testAgent);
		new Village(hexMap.getHexAt(0, 1));
		assertEquals(BasicVariables.OWNS_HUT.getValue(testAgent), 1.0, 0.001);
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 1.0, 0.001);
		testAgent.setLocation(hexMap.getHexAt(2, 1));
		assertEquals(BasicVariables.OWNS_HUT.getValue(testAgent), 1.0, 0.001);
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.95, 0.001);
		testAgent.setLocation(hexMap.getHexAt(2, 0));
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.9, 0.001);
		testAgent.setLocation(hexMap.getHexAt(0, 0));
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.95, 0.001);
		testAgent.setLocation(hexMap.getHexAt(0, 2));
		assertEquals(BasicVariables.HUT_PROXIMITY.getValue(testAgent), 0.95, 0.001);
	}

	@Test 
	public void resourceTests() {
		assertEquals(BasicVariables.FOOD_LEVEL.getValue(testAgent), 0.0, 0.0001);
		assertEquals(BasicVariables.WOOD_LEVEL.getValue(testAgent), 0.0, 0.0001);

		testAgent.addItem(Resource.FOOD);
		assertEquals(BasicVariables.FOOD_LEVEL.getValue(testAgent), 0.10, 0.0001);
		assertEquals(BasicVariables.WOOD_LEVEL.getValue(testAgent), 0.0, 0.0001);

		testAgent.addItem(Resource.WOOD);
		assertEquals(BasicVariables.FOOD_LEVEL.getValue(testAgent), 0.10, 0.0001);
		assertEquals(BasicVariables.WOOD_LEVEL.getValue(testAgent), 0.33, 0.01);
	}

	@Test
	public void hexCapacity() {
		BasicHex testHex = hexMap.getHexAt(1, 1);
		testAgent.setLocation(testHex);
		assertEquals(BasicVariables.HEX_CAPACITY.getValue(testAgent), 1.0, 0.001);
		testHex.changeCarryingCapacity(-2);
		assertEquals(BasicVariables.HEX_CAPACITY.getValue(testAgent), 0.8, 0.001);
		testHex.changeCarryingCapacity(-20);
		assertEquals(BasicVariables.HEX_CAPACITY.getValue(testAgent), 0.0, 0.001);
	}

	@Test
	public void civLevel() {
		BasicHex testHex = hexMap.getHexAt(1, 1);
		testAgent.setLocation(testHex);
		testAgent2.setLocation(testHex);
		assertEquals(BasicVariables.CIV_LEVEL.getValue(testAgent), 0.0, 0.001);
		double nd = 0.0;
		for (int n = 1; n < 11; n++) {
			nd = (double)n;
			new Hut(testAgent);
			assertEquals(BasicVariables.CIV_LEVEL.getValue(testAgent), nd/10.0, 0.001);
			assertEquals(BasicVariables.CIV_LEVEL.getValue(testAgent2), nd/10.0, 0.001);
			assertEquals(BasicVariables.OWNS_HUT.getValue(testAgent), 1.0, 0.001);
			assertEquals(BasicVariables.OWNS_HUT.getValue(testAgent2), 0.0, 0.001);
		}
	}

	@Test
	public void population() {
		BasicHex testHex = hexMap.getHexAt(1, 1);
		testAgent.setLocation(testHex);
		assertEquals(BasicVariables.POPULATION.getValue(testAgent), 0.05, 0.001);
		for (int loop = 0; loop < 12; loop++) {
			BasicAgent a = new BasicAgent(w);
			a.setLocation(testHex);
		}
		assertEquals(BasicVariables.POPULATION.getValue(testAgent), 0.65, 0.001);
	}

	@Test
	public void age() {
		double nd = 0.0;
		for (int n = 1; n <= 180; n++) {
			nd = (double)n;
			testAgent.setAge(1000 * n);
			assertEquals(BasicVariables.AGE.getValue(testAgent), nd/180.0, 0.001);
		}
	}

	private void setTerrainAtHex(HexMap<BasicHex> map, int row, int column, TerrainType terrain) {
		Hex hexToUpdate = map.getHexAt(row, column);
		hexToUpdate.setTerrain(terrain);
	}

}
