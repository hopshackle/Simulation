package hopshackle.simulation;

import java.awt.Point;
import java.util.*;

public class SquareMap extends LocationMap {

	private int x;
	private int y;
	private HashMap<Point, Square> locationMap;
	
	public SquareMap(int x, int y) {
		this.x = x;
		this.y = y;
		createSquares();
	}
	
	private void createSquares() {
		// This method sets up all the child squares that make up the world
		locationMap = new HashMap<Point, Square>(x * y);
		for (int i = 0; i < x; i++) {
			for (int j = 0; j < y; j++) {
				Square s = new Square(i,j);
				locationMap.put(new Point(i,j), s);
			}
		}

		for (Square l1 : locationMap.values()) {
			for (Square l2 : locationMap.values()) {
				if (!l1.equals(l2)) {
					if (((Square)l1).distanceFrom((Square)l2) == 1.0) {
						l1.addAccessibleLocation(l2);
					}
				}
			}
		}
	}
	
	@Override
	public void registerWorld(World world) {
		super.registerWorld(world);
		for (Square s : locationMap.values())
			s.setParentLocation(world);
	}
	
	public Square getSquareAt(int x, int y) {
		return locationMap.get(new Point(x, y));
	}

	@Override
	public List<Square> getAllLocations() {
		return HopshackleUtilities.cloneList(locationMap.values());
	}
}
