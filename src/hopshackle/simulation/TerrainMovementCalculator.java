package hopshackle.simulation;


public class TerrainMovementCalculator implements MovementCostCalculator {

	@Override
	public <T extends Location> double movementCost(T startLocation, T endLocation) {
		if (startLocation.hasRouteTo(endLocation)) {
			if (endLocation instanceof Hex) {
				Hex hexToEnter = (Hex) endLocation;
				return hexToEnter.getTerrainType().getPointsToEnter();
			}
			return 10.0;	
		}
		return Double.MAX_VALUE;
	}
}
