package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.Action;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.ArrayList;

public class SpellAttack extends CombatAction {

	private Character caster;

	public SpellAttack(CombatAgent a) {
		super(a.getAgent(), true);
		caster = (Character)a.getAgent();
	}
	public SpellAttack(Character c) {
		super(c, true);
		caster = c;
	}

	protected void doStuff() {
		Spell spellCast = null;
		DnDAgent target = null;

		if (caster.getChrClass()==CharacterClass.CLERIC && caster.getLevel()>3) {
			if (caster.hasSpell("Spiritual Hammer") && caster.getMp()>2) {
				spellCast = new SpiritualHammer();
				// now check that we do not have an active Spiritual Hammer in play
				// if so - then we do not cast this spell
				ArrayList<Spell> spellsCast = caster.getActiveSpells();
				for (Spell s : spellsCast) {
					if (s.isActive() && (s instanceof SpiritualHammer))
						spellCast = null;
				}
			}
		}

		if (spellCast != null) {
			// Now  we have determined the spell to be cast
			// and finally - if not distracted, then cast the spell
			if (!spellCast.isDistracted(caster)) 
				spellCast.cast(caster, target);

		} else {	// Did not find any appropriate spell to cast
			Action defaultAction = null;
			CombatAgent ca = caster.getCombatAgent();
			if (caster.getCurrentTarget() != null && !caster.getCurrentTarget().isDead()) {
				defaultAction = new Attack(ca, ca.getCurrentTarget(), false, false);
			} else {
				defaultAction = new Defend(ca, false);
			}
			defaultAction.run();
		}
	}
	public String toString() {return "SPELL_ATTACK";}
}
