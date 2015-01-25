package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.dnd.*;

public class Disengage extends CombatAction {

	private CombatAgent disengager;

	public Disengage(CombatAgent a, boolean recordAction) {
		super(a.getAgent(), recordAction);
		disengager = a;
	}

	protected void doStuff() {
		// First of all we provoke attacks of opportunity from
		// all attackers as well as current target
		Fight f = disengager.getCurrentFight();
		f.provokesAO(disengager);
		
		if (!disengager.isDead() && !disengager.isStunned()) {
			// has successfully made it out
			disengager.getCurrentFight().moveToReserve(disengager);

			// and then choose new follow action
			// for the moment we default to charging back in (but maybe 
			// with new opponent)
			Attack followOnAttack = new Attack(disengager, f.getOpponent(disengager, true), false, false);
			followOnAttack.run();
		}
	}

	public String toString() {return "DISENGAGE";}

}
