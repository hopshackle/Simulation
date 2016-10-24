package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import org.junit.Test;
public class TerrainMatcherTest {

	@Test 
	public void equalsWorksForSameTerrainType() {
		TerrainMatcher tm1 = new TerrainMatcher(TerrainType.DESERT);
		TerrainMatcher tm2 = new TerrainMatcher(TerrainType.DESERT);
		assertTrue(tm1.equals(tm2));
		
		assertEquals(tm1.hashCode(), tm2.hashCode());
	}
	
	@Test
	public void equalsWorksForDifferentTerrainTypes() {
		TerrainMatcher tm1 = new TerrainMatcher(TerrainType.DESERT);
		TerrainMatcher tm2 = new TerrainMatcher(TerrainType.PLAINS);
		assertFalse(tm1.equals(tm2));
		
		assertFalse(tm1.hashCode() == tm2.hashCode());
	}
	
	
}
