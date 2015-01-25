package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.dnd.CombatAgent;

public class Attack extends CombatAction {

	private CombatAgent opponent;
	private CombatAgent attacker;
	private boolean subdualAttack;

	public Attack(CombatAgent a, CombatAgent target, boolean toStun, boolean recordAction) {
		super(a.getAgent(), recordAction);
		attacker = a;
		opponent = target;
		if (opponent == null) opponent = a.getCurrentTarget();
		// if the target is a member of the same party, then always subdue
		subdualAttack = toStun;
		if (a.getCurrentFight().isInternal()) subdualAttack = true;
	}

	protected void doStuff() {
		// check we have a valid opponent. If not, find one.
		// then attack.
		// If Opponent is hors de combat as a result, then remove them from the
		// fight

		if (attacker.isDead() || attacker.isStunned()) return;
		if (opponent == null || opponent.isDead() || (opponent.isStunned() && subdualAttack)) {
			// need to find a target 
			// if stunned - still a valid target unless this is subdual
			opponent = attacker.getCurrentFight().getOpponent(attacker, true);
		}
		if (opponent !=null && !opponent.isDead()) {
			attacker.attack(opponent, subdualAttack);		
			if (opponent.isDead() || (opponent.isStunned() && subdualAttack))
				attacker.getCurrentFight().removeCombatant(opponent);
		}
		
		attacker = null;
		opponent = null;
	}
	
	public String toString() {return "ATTACK";}
}
