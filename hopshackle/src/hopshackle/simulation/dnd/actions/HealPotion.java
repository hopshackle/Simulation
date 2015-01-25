package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public class HealPotion extends CombatAction {

	public HealPotion(CombatAgent ca, boolean recordAction)	{
		super(ca.getAgent(), recordAction);
	}

	public void doStuff() {
		Character c = (Character) actor;

		if (Potion.CURE_LIGHT_WOUNDS.drink(c)) {
			// TODO: Once we have more than one Heal potion, this should be modified
			// to look for the best one given current damage

			// Now determine if vulnerable to Attacks of Opportunity
			if (c.getCurrentFight() != null) {
				Fight f = c.getCurrentFight();
				// If more than 2 attackers, then 5' step not possible
				if (f.getPossibleAO(c.getCombatAgent()) > 2)
					f.provokesAO(c.getCombatAgent());
			}
		} else {
			// nothing - as has no Heal Potion
		}
	}
	public String toString() {return "HEAL_POTION";}
}
