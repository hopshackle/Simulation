package hopshackle.simulation;

public interface MovementCostCalculator {

	public <T extends Location> double movementCost(T startLocation, T endLocation);
}
