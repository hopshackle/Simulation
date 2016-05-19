package hopshackle.simulation.basic;

import java.util.List;

import hopshackle.simulation.*;

public class BasicMove extends BasicAction {

	private GoalMatcher<Location> locationMatcher;
	private Location destination;
	private long duration;

	public BasicMove(ActionEnum<BasicAgent> type, BasicAgent agent, GoalMatcher<Location> locationMatcher) {
		super(type, agent, 1000, false);	// we override the finishing time during initialisation
		this.locationMatcher = locationMatcher;
		world.recordAction(this); // bit of a hack - as locationMatcher provides part of the name of the action to be recorded
	}
	@Override
	public void initialisation() {
		destination = getNextMove();
		TerrainType terrain = TerrainType.MOUNTAINS;
		if (destination instanceof Hex) {
			terrain = ((Hex)destination).getTerrainType();
		}
		duration = (long) (terrain.getPointsToEnter() * 1000);
		plannedEndTime = plannedStartTime + duration;
	}


	@Override
	public void doStuff() {
		for (BasicAgent ba : getAllConfirmedParticipants()) {
			ba.recordSpendOfMovementPoints((int) duration/1000);
			ba.setLocation(destination);
			ba.addHealth(- (double)duration / 1000.0);
		}
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
			GoalMatcher<?> defaultMatcher = new UnexploredLocationMatcher(mover.getMapKnowledge());
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
		return "MOVE_" + getType() + " (" + plannedStartTime + "-" + plannedEndTime + ")";
	}
}
