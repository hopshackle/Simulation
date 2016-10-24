package hopshackle.simulation;

import java.awt.Color;

public enum TerrainType {
	
	PLAINS		(Color.white, 1.0),
	FOREST		(Color.green, 2.0),
	HILLS		(Color.lightGray, 2.0),
	DESERT		(Color.yellow, 1.0),
	MOUNTAINS	(Color.black, 5.0),
	OCEAN		(Color.blue, 10.0), 
	SWAMP		(Color.CYAN, 3.0);
	
	private Color colour;
	private double pointsToEnter;
	
	TerrainType(Color c, double movePoints) {
		colour = c;
		pointsToEnter = movePoints;
	}
	
	public Color getDisplayColour() {
		return this.colour;
	}
	public double getPointsToEnter() {
		return pointsToEnter;
	}

}
