package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class Move extends Action {

	private Location destination;
	private long duration;

	public Move(Agent a, long start, Location destination) {
		super(a, start, false);
		duration = start;
		this.destination = destination;
		if (!actor.getLocation().hasRouteTo(destination)) {
			throw new AssertionError(actor + " cannot get to " + destination + " from " + actor.getLocation());
		}
	}

	@Override
	public void doStuff() {
		super.doStuff();
		BasicAgent ba = (BasicAgent) actor;
		doMoveStuff(ba);
		if (ba.isMarried())
			doMoveStuff(ba.getPartner());
	}

	private void doMoveStuff(BasicAgent ba) {
		ba.recordSpendOfMovementPoints((int) duration/1000);
		ba.setLocation(destination);
		ba.addHealth(- (double)duration / 1000.0);
	}

}