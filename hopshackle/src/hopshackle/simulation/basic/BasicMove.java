package hopshackle.simulation.basic;

import java.util.List;

import hopshackle.simulation.*;

public class BasicMove extends Action {

	private GoalMatcher locationMatcher;

	public BasicMove(Agent agent, GoalMatcher locationMatcher) {
		super(agent, 500, false);
		this.locationMatcher = locationMatcher;
		world.recordAction(this); // bit of a hack - as locationMatcher provides part of the name of the action to be recorded
	}
	@Override
	public void doStuff() {
		Location nextMove = getNextMove();
		
		TerrainType terrain = TerrainType.MOUNTAINS;
		if (nextMove instanceof Hex) {
			terrain = ((Hex)nextMove).getTerrainType();
		}
		int timeTakenToMove = (int) (terrain.getPointsToEnter() * 1000);
		Action moveAction = new Move(actor, 0, timeTakenToMove - 500, nextMove);
		moveAction.addToAllPlans();
	}

	private Location getNextMove() {
		BasicAgent mover = (BasicAgent) actor;
		JourneyPlan currentJourneyPlan = mover.getJourneyPlan();
		if (currentJourneyPlan != null && currentJourneyPlanIsValid(currentJourneyPlan)) {
			Location nextLoc = currentJourneyPlan.getNextMove();
			if (nextLoc != null) {
				return nextLoc;
			}
		} 

		JourneyPlan jPlan = new JourneyPlan(mover, locationMatcher, new TerrainMovementCalculator());
		if (jPlan.isEmpty() && mover.hasUnexploredLocations()) {
			GoalMatcher defaultMatcher = new UnexploredLocationMatcher(mover.getMapKnowledge());
			jPlan = new JourneyPlan(mover, defaultMatcher, new TerrainMovementCalculator());
		}
		if (!jPlan.isEmpty()) {
			mover.setJourneyPlan(jPlan);
			return jPlan.getNextMove();
		} else {
			mover.setHasUnexploredLocations(false);
			List<Location> possibleMoves = mover.getLocation().getAccessibleLocations();
			return possibleMoves.get(Dice.roll(1, possibleMoves.size()) - 1);
		}
	}

	private boolean currentJourneyPlanIsValid(JourneyPlan currentJourneyPlan) {
		GoalMatcher currentPlanMatcher = currentJourneyPlan.getLocationMatcher();
		if (currentPlanMatcher.equals(locationMatcher) || currentPlanMatcher.supercedes(locationMatcher)) {
			return true;
		}
		return false;
	}

	public String toString() {
		if (locationMatcher != null) 
			return "MOVE_" + locationMatcher.toString();
		else 
			return "MOVE_NULL";

	}
}
