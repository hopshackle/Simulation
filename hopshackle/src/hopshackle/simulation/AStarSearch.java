package hopshackle.simulation;

import java.util.*;

public final class AStarSearch<T extends State> {

	private static boolean debug = false;

	public static <T extends State> List<StateAction> findPlan(T startState, GoalMatcher<T> goalTest, ValuationFunction<T> heuristic) {	
		if (heuristic == null)
			heuristic = new ValuationFunction<T>() {
			@Override
			public double getValue(T item) {return 0;}
		};

		if (goalTest == null)
			goalTest = new GoalMatcher<T>() {
				@Override
				public boolean matches(T state) {return false;}
				@Override
				public boolean supercedes(GoalMatcher<T> competitor) {return false;}
		};

		PriorityQueue<PrioritisedState> fringe = new PriorityQueue<PrioritisedState>();
		fringe.add(new PrioritisedState(startState, 0, (int)heuristic.getValue(startState)));
		HashSet<T> closedList = new HashSet<T>();
		HashMap<T, StateActionPair<T>> parentLocations = new HashMap<T, StateActionPair<T>>();

		boolean routeFound = false;
		int currentCost = 0;
		T destination = null;
		while (!fringe.isEmpty() && !routeFound) {
			PrioritisedState ps = fringe.poll();
			if (ps.costToHere > currentCost) {
				System.out.println("Total of " + closedList.size() + " states examined  with cost less than or equal to " + currentCost);
				currentCost = ps.costToHere;
			}
			T currentNode = (T) ps.state;
			if (debug)
				System.out.println(currentNode + " taken off fringe with " + ps.costToHere + " moves taken, " + ps.heuristicFromHere + " heuristic estimate");
			closedList.add(currentNode);
			if (goalTest.matches(currentNode)) {
				routeFound = true;
				destination = currentNode;
			} else {
				for (StateAction a : currentNode.getValidActions()) {
					T newState = (T) currentNode.applyAction(a);
					if (!closedList.contains(newState)) {
						parentLocations.put(newState, new StateActionPair<T>(currentNode, a));
						if (debug) 
							System.out.println("Action" + a + " moves to " + newState);
						fringe.add(new PrioritisedState(newState, ps.costToHere+1, (int)heuristic.getValue(newState)));
					}
				}
			}
		}

		if (routeFound) {
			return calculateFinalRoute(startState, destination, parentLocations);
		}

		return new ArrayList<StateAction>();
	}

	private static <T extends State> List<StateAction> calculateFinalRoute(T startLocation, T targetLocation, HashMap<T, StateActionPair<T>> parentLocations) {
		List<StateAction> routeBackwards = new ArrayList<StateAction>();
		T nextMove = targetLocation;
		while (!nextMove.equals(startLocation)) {
			StateActionPair<T> previousStep = parentLocations.get(nextMove);
			routeBackwards.add(previousStep.action);
			nextMove = previousStep.state;
		}

		List<StateAction> routeForwards = new ArrayList<StateAction>();
		for (int loop = routeBackwards.size(); loop > 0; loop--) {
			routeForwards.add(routeBackwards.get(loop-1));
		}
		return routeForwards;
	}

}

class PrioritisedState implements Comparable<PrioritisedState> {
	State state;
	int costToHere, heuristicFromHere;
	public PrioritisedState(State s, int cost, int heuristic) {
		state = s;
		costToHere = cost;
		heuristicFromHere = heuristic;
	}
	@Override
	public int compareTo(PrioritisedState other) {
		return costToHere + heuristicFromHere - other.costToHere - other.heuristicFromHere;
	}
}

class StateActionPair<T extends State> {
	T state;
	StateAction action;
	public StateActionPair(T s, StateAction a) {
		state = s;
		action = a;
	}
}
