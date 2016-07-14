package hopshackle.simulation;

import java.util.*;

public final class AStarPathFinder {

	public static List<Location> findRoute(Agent agent, Location startLocation, Location targetLocation,
			MovementCostCalculator movementCalculator) {
		if (agent != null) {
			MapKnowledge map = agent.getMapKnowledge();
			if (map != null) {
				return findRoute(map, startLocation, targetLocation, movementCalculator);
			} else {
				return new ArrayList<Location>();
			}
		} else {
			return new ArrayList<Location>();
		}
	}

	public static List<Location> findRoute(MapKnowledge map, Location startLocation, final Location targetLocation, 
			MovementCostCalculator movementCalculator) {
		GoalMatcher<Location> matcher = new GoalMatcher<Location>() {
			@Override
			public boolean matches(Location loc) {
				if (loc.equals(targetLocation)) return true;
				return false;
			}

			@Override
			public boolean supercedes(GoalMatcher<Location> competitor) {
				return false;
			}
		};

		return findRouteToNearestLocation(map, startLocation, matcher, movementCalculator);
	}

	public static List<Location> findRouteToNearestLocation(MapKnowledge map, Location startLocation, 
			GoalMatcher matcher, MovementCostCalculator movementCalculator) {

		if (map == null || matcher == null) return new ArrayList<Location>();
		if (!map.isKnown(startLocation)) return new ArrayList<Location>();
		if (matcher.matches(startLocation)) return new ArrayList<Location>();

		if (movementCalculator == null) {
			movementCalculator = new MovementCostCalculator() {
				@Override
				public <T extends Location> double movementCost(T startLocation, T endLocation) {
					return 1.0;
				}
			};
		}

		List<Location> openList = new ArrayList<Location>();
		openList.add(startLocation);
		List<Location> closedList = new ArrayList<Location>();
		HashMap<Location, Location> parentLocations = new HashMap<Location, Location>();
		HashMap<Location, Double> locationCost = new HashMap<Location, Double>();
		locationCost.put(startLocation, 0.00);

		boolean routeFound = false;
		Location destination = null;
		while (!openList.isEmpty() && !routeFound) {
			Location currentNode = getLocationWithLowestCost(openList, locationCost);
			closedList.add(currentNode);
			openList.remove(currentNode);
			if (matcher.matches(currentNode)) {
				routeFound = true;
				destination = currentNode;
				break;
			}
			for (Location l : currentNode.accessibleLocations) {
				if (map.isKnown(l) && !openList.contains(l) && !closedList.contains(l)) {
					openList.add(l);
					parentLocations.put(l, currentNode);
					double costOfLocation = locationCost.get(currentNode);
					costOfLocation += movementCalculator.movementCost(currentNode, l);
					costOfLocation += calculateLocationHeuristic(l, currentNode, matcher);
					locationCost.put(l, costOfLocation);
				}
			}
		}

		if (routeFound) {
			return calculateFinalRoute(startLocation, destination, parentLocations);
		}

		return new ArrayList<Location>();
	}

	private static Location getLocationWithLowestCost(List<Location> openList, HashMap<Location, Double> locationCost) {
		List<Location> cheapestLocation = new ArrayList<Location>();
		double cheapestLocationValue = Double.MAX_VALUE;
		for (Location openLocation : openList) {
			if (locationCost.get(openLocation) - 0.01 <= cheapestLocationValue) 
				cheapestLocationValue = locationCost.get(openLocation);
		}
		for (Location openLocation : openList) {
			if (locationCost.get(openLocation) - 0.01 <= cheapestLocationValue) 
				cheapestLocation.add(openLocation);
		}
		return cheapestLocation.get(Dice.roll(1, cheapestLocation.size())-1);
	}

	private static double calculateLocationHeuristic(Location locationToCost, Location parentLocation, GoalMatcher matcher) {
		// TODO: Need to implement before A* actually does what it says on the tin.
		return 0.0;
	}

	private static List<Location> calculateFinalRoute(Location startLocation, Location targetLocation, HashMap<Location, Location> parentLocations) {
		List<Location> routeBackwards = new ArrayList<Location>();
		Location nextMove = targetLocation;
		while (!nextMove.equals(startLocation)) {
			routeBackwards.add(nextMove);
			nextMove = parentLocations.get(nextMove);
		}

		List<Location> routeForwards = new ArrayList<Location>();
		for (int loop = routeBackwards.size(); loop > 1; loop--) {
			routeForwards.add(routeBackwards.get(loop-1));
		}
		routeForwards.add(targetLocation);
		return routeForwards;
	}

}
