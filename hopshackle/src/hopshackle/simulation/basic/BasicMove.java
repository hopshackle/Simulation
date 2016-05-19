package hopshackle.simulation.basic;

import java.util.List;

import hopshackle.simulation.*;

public class BasicMove extends BasicAction {

	private GoalMatcher locationMatcher;

	public BasicMove(ActionEnum<BasicAgent> type, BasicAgent agent, GoalMatcher locationMatcher) {
		super(type, agent, 400, false);
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
		BasicAction moveAction = new Move(actor, 100, timeTakenToMove - 500, nextMove);
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
			if (mover.getLocation() == null) logger.warning("Null location for " + mover);
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
			return "MOVE_" + locationMatcher.toString() + " (" + plannedStartTime + "-" + plannedEndTime + ")";
		else 
			return "MOVE_NULL";

	}
}
