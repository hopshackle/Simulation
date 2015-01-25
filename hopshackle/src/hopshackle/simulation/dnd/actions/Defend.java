package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public class Defend extends CombatAction {

	private CombatAgent defender;
	
	public Defend(CombatAgent a, boolean recordAction) {
		super(a.getAgent(), recordAction);
		defender = a;
	}
	
	protected void doStuff() {
		defender.addCondition(new FullDefense(defender));
	}

	class FullDefense implements CombatCondition {

		private CombatAgent defender;
		
		public FullDefense (CombatAgent defender) {
			this.defender = defender;
		}

		public void apply(CombatModifier cm) {
			/* Full Defense will add 2 to AC, at the expense of 
			 * subtracting four from to hit (i.e. for A of O)
			 */
			if (cm.getDefender() == defender) {
				cm.setDodge(cm.getDodge()+2);
			}
			if (cm.getAttacker() == (Character)actor) {
				cm.setToHit(cm.getToHit()-4);
			}
		}

		public boolean isPermanent() {return false;}

		public String toString() {return "FullDefense";}
		public int roundsLeft() {
			return 0;
			// always lasts until next action by the character
		}
		
	}
	public String toString() {return "DEFEND";}
}
