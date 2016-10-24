package hopshackle.simulation;

import java.util.List;

/**
 * LocationMap provides the internal logic for grouping of sub-Locations within a parent Location. For example a World could 
 * be divided into Squares (a SquareMap) or Hexes (a HexMap)
 * 
 * @author James Goodman
 * @see #HexMap HexMap
 * @see #SquareMap SquareMap
 *
 */
public abstract class LocationMap {
	
	protected World world;
	
	public boolean isRegistered() {
		return (world != null);
	}

	public void registerWorld(World w) {
		world = w;
	}
	
	public World getWorld() {
		return world;
	}
	
	public abstract List<? extends Location> getAllLocations();
}
