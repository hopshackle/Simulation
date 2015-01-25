package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class BasicMove extends Action {

	private GoalMatcher locationMatcher;
	private Location nextMove;

	public BasicMove(Agent agent, GoalMatcher locationMatcher) {
		super(agent, 0, false);
		this.locationMatcher = locationMatcher;
		world.recordAction(this); // bit of a hack - as locationMatcher provides part of the name of the action to be recorded
	}

	@Override
	public void doStuff() {
		BasicAgent mover = (BasicAgent) actor;
		JourneyPlan currentJourneyPlan = mover.getJourneyPlan();
		if (currentJourneyPlan != null && currentJourneyPlanIsValid(currentJourneyPlan)) {
			Location nextLoc = currentJourneyPlan.getNextMove();
			if (nextLoc != null) {
				nextMove = nextLoc;
				return;
			}
		} 

		JourneyPlan jPlan = new JourneyPlan(mover, locationMatcher, new TerrainMovementCalculator());
		if (jPlan.isEmpty() && mover.hasUnexploredLocations()) {
			GoalMatcher defaultMatcher = new UnexploredLocationMatcher(mover.getMapKnowledge());
			jPlan = new JourneyPlan(mover, defaultMatcher, new TerrainMovementCalculator());
		}
		if (!jPlan.isEmpty()) {
			mover.setJourneyPlan(jPlan);
			nextMove = jPlan.getNextMove();
		} else {
			mover.setHasUnexploredLocations(false);
		}
	}

	private boolean currentJourneyPlanIsValid(JourneyPlan currentJourneyPlan) {
		GoalMatcher currentPlanMatcher = currentJourneyPlan.getLocationMatcher();
		if (currentPlanMatcher.equals(locationMatcher) || currentPlanMatcher.supercedes(locationMatcher)) {
			return true;
		}
		return false;
	}

	@Override
	public void doNextDecision() {
		if (nextMove == null) {
			super.doNextDecision();
			return;
		}
		TerrainType terrain = TerrainType.MOUNTAINS;
		if (nextMove instanceof Hex) {
			terrain = ((Hex)nextMove).getTerrainType();
		}
		int timeTakenToMove = (int) (terrain.getPointsToEnter() * 1000);
		Action moveAction = new Move(actor, timeTakenToMove, nextMove);
		actor.addAction(moveAction);
	}

	public String toString() {
		if (locationMatcher != null) 
			return "MOVE_" + locationMatcher.toString();
		else 
			return "MOVE_NULL";

	}
}
