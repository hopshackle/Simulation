package hopshackle.simulation;

import java.util.List;

public class JourneyPlan {

	private GoalMatcher locationMatcher;
	private MovementCostCalculator movementCalculator;
	private Agent agent;
	private List<Location> route;
	private int indexOfNextMove;

	public JourneyPlan(Agent trekker, GoalMatcher locationMatcher, MovementCostCalculator movementCalculator) {
		agent = trekker;
		this.locationMatcher = locationMatcher;
		this.movementCalculator = movementCalculator;
		route = AStarPathFinder.findRouteToNearestLocation(agent.getMapKnowledge(), agent.getLocation(), locationMatcher, movementCalculator);
		updatePlan();
	}

	public void updatePlan() {
		Location currentLocation = agent.getLocation();
		if (currentLocation == null || currentLocation.equals(getDestination()) || route.isEmpty()) {
			indexOfNextMove = -1;
			return;
		}
		int count = 0;
		boolean currentLocationIsOnRoute = false;
		for (Location l : route) {
			if (l.equals(currentLocation)) {
				currentLocationIsOnRoute = true;
				break;
			}
			count++;
		}

		indexOfNextMove = count+1;

		if (!currentLocationIsOnRoute) {
			if (currentLocation.getAccessibleLocations().contains(route.get(0))) {
				indexOfNextMove = 0;
			} else {
				indexOfNextMove = -1;
			}
		} 
	}

	public GoalMatcher getLocationMatcher() {
		return locationMatcher;
	}

	public Location getDestination() {
		if (route != null && !route.isEmpty())
			return route.get(route.size()-1);
		return null;
	}

	public Location getNextMove() {
		updatePlan();
		if (indexOfNextMove > -1) {
			return route.get(indexOfNextMove);
		} else {
			return null;
		}
	}

	public boolean isEmpty() {
		return (getDestination() == null);
	}

	public double distance() {
		if (indexOfNextMove == -1)
			return -1;

		int movesToDestination = route.size() - indexOfNextMove;
		if (movementCalculator == null) {
			return (double)movesToDestination;
		}

		double movementCost = 0.00;
		for (int loop = 0; loop < movesToDestination; loop++) {
			Location start = agent.getLocation();
			if (loop > 0) {
				start = route.get(indexOfNextMove + loop - 1);
			}
			Location end = route.get(indexOfNextMove + loop);
			movementCost += movementCalculator.movementCost(start, end);
		}

		return movementCost;
	}

	public MovementCostCalculator getMovementCalculator() {
		return movementCalculator;
	}

}
