package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;

import java.util.List;

import org.junit.*;

public class AStarPathFinderTest {

	private MapKnowledge map;
	private HexMap<Hex> hexMap;
	private Location startLocation;
	private Location targetLocation;

	@Before 
	public void setUp() {
		initialiseMap();
		startLocation = new Location();
		targetLocation = new Location();
		hexMap = new HexMap<Hex>(5, 5, Hex.getHexFactory());
		map.addLocationMap(hexMap);
	}

	@Test
	public void emptyRouteReturnedIfNoRouteAvailable() {
		map.addLocation(startLocation);
		map.addLocation(targetLocation);
		List<Location> route = AStarPathFinder.findRoute(map, startLocation, targetLocation, null);
		assertTrue(route.isEmpty());
	}

	@Test
	public void singleMoveIfTargetLocationIsNextToStartLocation() {
		map.addLocation(startLocation);
		map.addLocation(targetLocation);
		startLocation.addAccessibleLocation(targetLocation);
		List<Location> route = AStarPathFinder.findRoute(map, startLocation, targetLocation, null);
		assertEquals(route.size(), 1);
		assertTrue(route.get(0) == targetLocation);
	}

	@Test
	public void emptyRouteIfStartLocationIsTargetLocation() {
		map.addLocation(startLocation);
		map.addLocation(targetLocation);
		startLocation.addAccessibleLocation(targetLocation);
		List<Location> route = AStarPathFinder.findRoute(map, targetLocation, targetLocation, null);
		assertTrue(route.isEmpty());
	}

	@Test
	public void simpleTestToFindRouteOnSquareMap() {
		SquareMap squareMap = new SquareMap(5, 5);
		map.addLocationMap(squareMap);

		startLocation = squareMap.getSquareAt(1, 1);
		targetLocation = squareMap.getSquareAt(3, 4);
		List<Location> route = AStarPathFinder.findRoute(map, startLocation, targetLocation, null);
		assertEquals(route.size(), 5);
		checkRouteValidity(route, targetLocation);
	}

	@Test
	public void simpleTestToFindRouteOnHexMapByTraversingColumn() {
		startLocation = hexMap.getHexAt(0, 1);
		targetLocation = hexMap.getHexAt(4, 1);
		List<Location> route = AStarPathFinder.findRoute(map, startLocation, targetLocation, null);
		assertEquals(route.size(), 4);
		checkRouteValidity(route, targetLocation);
	}

	@Test
	public void findRouteOnHexMapAcrossOneColumn() {
		startLocation = hexMap.getHexAt(0, 2);
		targetLocation = hexMap.getHexAt(4, 1);
		List<Location> route = AStarPathFinder.findRoute(map, startLocation, targetLocation, null);
		assertEquals(route.size(), 4);
		checkRouteValidity(route, targetLocation);
	}

	@Test
	public void findRouteOnHexMapAcrossTwoColumns() {
		startLocation = hexMap.getHexAt(0, 3);
		targetLocation = hexMap.getHexAt(4, 1);
		List<Location> route = AStarPathFinder.findRoute(map, startLocation, targetLocation, null);
		assertEquals(route.size(), 5);
		checkRouteValidity(route, targetLocation);
	}

	@Test
	public void findRouteWithTerrainCosts() {
		setTerrainAtHex(hexMap, 2, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 3, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 4, 2, TerrainType.MOUNTAINS);
		
		startLocation = hexMap.getHexAt(3, 0);
		targetLocation = hexMap.getHexAt(4, 4);
		List<Location> route = AStarPathFinder.findRoute(map, startLocation, targetLocation, new TerrainMovementCalculator());
		assertEquals(route.size(), 7);
		checkRouteValidity(route, targetLocation);
	}

	@Test
	public void findNearestUnexploredLocation() {
		Location interimLocation = new Location();
		startLocation.addAccessibleLocation(interimLocation);
		interimLocation.addAccessibleLocation(targetLocation);
		map.addLocation(startLocation);
		map.addLocation(interimLocation);
		GoalMatcher locMatcher = new UnexploredLocationMatcher(map);
		List<Location> route = AStarPathFinder.findRouteToNearestLocation(map, startLocation, locMatcher, null);
		assertTrue(route.get(0).equals(interimLocation));
		assertEquals(route.size(), 1);
	}

	@Test
	public void failsToFindRouteIfThereAreNoUnexploredHexes() {
		startLocation = hexMap.getHexAt(3, 0);
		GoalMatcher locMatcher = new UnexploredLocationMatcher(map);
		List<Location> route = AStarPathFinder.findRouteToNearestLocation(map, startLocation, locMatcher, null);
		assertTrue(route.isEmpty());
	}
	
	@Test
	public void findsUnexploredHex() {
		initialiseMap();
		startLocation = hexMap.getHexAt(3, 0);
		map.addLocation(startLocation);
		for (Location l : startLocation.getAccessibleLocations()) {
			map.addLocation(l);
		}
		
		GoalMatcher locMatcher = new UnexploredLocationMatcher(map);
		List<Location> route = AStarPathFinder.findRouteToNearestLocation(map, startLocation, locMatcher, null);
		assertEquals(route.size(), 1);
	}

	@Test
	public void findNearestMountainHex() {
		setTerrainAtHex(hexMap, 2, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 3, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 4, 2, TerrainType.MOUNTAINS);

		startLocation = hexMap.getHexAt(3, 0);
		GoalMatcher mountainFinder = new TerrainMatcher(TerrainType.MOUNTAINS);
		List<Location> route = AStarPathFinder.findRouteToNearestLocation(map, startLocation, mountainFinder, new TerrainMovementCalculator());
		assertEquals(route.size(), 2);
		assertTrue(((Hex)route.get(1)).getTerrainType().equals(TerrainType.MOUNTAINS));
		checkRouteValidity(route, null);
	}
	
	@Test
	public void failsTofindMountainHexIfNotKnown() {
		initialiseMap();
		setTerrainAtHex(hexMap, 2, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 3, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 4, 2, TerrainType.MOUNTAINS);

		startLocation = hexMap.getHexAt(3, 0);
		
		GoalMatcher mountainFinder = new TerrainMatcher(TerrainType.MOUNTAINS);
		List<Location> route = AStarPathFinder.findRouteToNearestLocation(map, startLocation, mountainFinder, new TerrainMovementCalculator());
		assertTrue(route.isEmpty());
	}
	
	@Test
	public void findsNearestUnexploredHexTakingMovementPointsIntoAccount() {
		initialiseMap();
		setTerrainAtHex(hexMap, 2, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 3, 2, TerrainType.MOUNTAINS);
		setTerrainAtHex(hexMap, 4, 2, TerrainType.MOUNTAINS);
		
		map.addLocation(hexMap.getHexAt(2, 2));
		map.addLocation(hexMap.getHexAt(3, 2));
		map.addLocation(hexMap.getHexAt(4, 2));
		map.addLocation(hexMap.getHexAt(2, 3));
		map.addLocation(hexMap.getHexAt(3, 3));
		map.addLocation(hexMap.getHexAt(4, 3));
		map.addLocation(hexMap.getHexAt(2, 4));
		map.addLocation(hexMap.getHexAt(3, 4));
		map.addLocation(hexMap.getHexAt(4, 4));
		map.addLocation(hexMap.getHexAt(1, 3));
		map.addLocation(hexMap.getHexAt(1, 4));
		
		startLocation = hexMap.getHexAt(4, 3);
		
		GoalMatcher explorer = new UnexploredLocationMatcher(map);
		List<Location> route = AStarPathFinder.findRouteToNearestLocation(map, startLocation, explorer, new TerrainMovementCalculator());
		assertEquals(route.size(), 2);
		targetLocation = hexMap.getHexAt(2, 3);
		checkRouteValidity(route, null);
	}


	private void checkRouteValidity(List<Location> route, Location targetLocation) {
		Location previousLocation = startLocation;
		for (Location nextLocation : route) {
			assertTrue(previousLocation.hasRouteTo(nextLocation));
			previousLocation = nextLocation;
		}
		if (targetLocation != null)
			assertTrue(route.get(route.size()-1).equals(targetLocation));
	}

	private void setTerrainAtHex(HexMap<Hex> map, int row, int column, TerrainType terrain) {
		Hex hexToUpdate = map.getHexAt(row, column);
		hexToUpdate.setTerrain(terrain);
	}
	
	private void initialiseMap() {
		map = new MapKnowledge(new BasicAgent(new World()));
	}
}
