package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.Action;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public class Heal extends CombatAction {

	private Character healer;

	public Heal(CombatAgent a, boolean recordAction) {
		super(a.getAgent(), recordAction);
		healer = (Character)a.getAgent();
	}
	public Heal(Character c, boolean recordAction)	{
		super(c, recordAction);
		healer = c;
	}

	protected void doStuff() {
		Character target;
		Fight f = healer.getCurrentFight();
		boolean healedSomeone = false;

		DnDAgent mainParty = (Character)actor;
		if (healer.getParty() != null) mainParty = healer.getParty();
		if (healer.isInCombat()) 
			if (f.isInternal()) {mainParty = healer;}
		// If internal - then only Buff self
		// Else buff whole party

		do {	// on the basis that if not in combat, a single Heal action is
			// not restricted to a single round
			// TODO: make this a separate Action to remove spaghetti
			target = healer;
			healedSomeone = false;
			double mostWounded = healer.getWound();
			for (Character partyMember : mainParty.getMembers()) {
				if (!partyMember.isDead() && partyMember.getWound() > mostWounded) {
					target = partyMember;
					mostWounded = partyMember.getWound();
				}
			}

			Spell spellChosen = healer.getSpell("Cure Light Wounds");
			if ( healer.getMp() > 0 && mostWounded > 0.01 && spellChosen != null) {
				// First of all we have to make a Concentration roll
				//  if we are doing this in a fight and cannot take a 5' step

				// If no one is wounded, there are no MP left, or if they know no healing spells
				// then default to DEFEND

				// CLW cures average of 7.5 hp at 3rd level
				// CMW cure average of 12 hp at 3rd
				// therefore if hp gap is 12 or higher, use CMW (in combat)
				// outside combat ALWAYS use CLW
				if (target.getMaxHp() - target.getHp() > 11 && healer.hasSpell("Cure Moderate Wounds") 
						&& healer.getMp() > 2)
					spellChosen = healer.getSpell("Cure Moderate Wounds");

				// and finally - if not distracted, then cast the spell
				if (!spellChosen.isDistracted(healer)) {
					spellChosen.cast(healer, target);
					healedSomeone = true;
				}

			} else if (healer.isInCombat()) {
				Action defaultAction = null;
				if (healer.getCurrentTarget() != null && !healer.getCurrentTarget().isDead()) {
					defaultAction = new Attack(healer.getCombatAgent(), healer.getCurrentTarget(), false, false);
				} else {
					defaultAction = new Defend(healer.getCombatAgent(), false);
				}
				defaultAction.run();
			}
		} while(!healer.isInCombat() && healedSomeone);
	}
	public String toString() {return "HEAL";}
}
