package hopshackle.simulation.test;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

public class LocationTest {
	
	public World world;
	public Location loc1, loc2, loc3;
	public Organisation<Agent> org1, org2;
	public BasicHex villageHex;
	public Village village;
	
	@Before
	public void setup() {
		world = new World();
		loc1 = new Location(world);
		loc2 = new Location(world);
		loc3 = new Location(loc1);
		org1 = new Organisation<Agent>("Org1", world, null);
		org2 = new Organisation<Agent>("Org2", loc2, null);
		villageHex = new BasicHex(0, 0);
		villageHex.setParentLocation(world);
		village = new Village(villageHex);
	}

	@Test
	public void getLocationOfTypeFindsAllChildren() {
		Set<Location> allLocations = world.getAllChildLocationsOfType(loc1);
		assertEquals(allLocations.size(), 3);
		Set<Organisation<Agent>> allOrganisations = world.getAllChildLocationsOfType(org1);
		assertEquals(allOrganisations.size(), 2);
		allOrganisations = loc2.getAllChildLocationsOfType(org1);
		assertEquals(allOrganisations.size(), 1);
		assertTrue(allOrganisations.toArray()[0] == org2);
		Set<Village> allVillages = world.getAllChildLocationsOfType(village);
		assertEquals(allVillages.size(), 1);
		allVillages = loc3.getAllChildLocationsOfType(village);
		assertTrue(allVillages.isEmpty());
	}

}
