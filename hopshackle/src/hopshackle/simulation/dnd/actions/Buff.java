package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.Action;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.ArrayList;

public class Buff extends CombatAction {

	private Character buffer;

	public Buff(CombatAgent a) {
		super(a.getAgent(), true);
		buffer = (Character)a.getAgent();
	}
	public Buff(Character c) {
		super(c, true);
		buffer = c;
	}

	protected void doStuff() {
		Spell spellCast = null;
		DnDAgent target = null;

		boolean bless = false, spellChosen = false;
		DnDAgent mainParty;
		mainParty = buffer;
		if (buffer.getParty() != null) mainParty = buffer.getParty();
		if (buffer.getCurrentFight().isInternal()) {mainParty = buffer;}

		boolean inParty = (mainParty != buffer);
		// If internal - then only Buff self
		// Else buff whole party

		// Check to see if Bless has been cast already
		for (CombatCondition cc : buffer.getCombatConditions()) {
			if (cc instanceof Bless) {
				bless = true;
				break;
			}
		}

		// And also check to see if we have any DamageReduction on the opposing side
		boolean vulnerableToMagic = false;
		Fight f = buffer.getCurrentFight();
		if (f != null) {
			ArrayList<CombatAgent> temp = f.getOtherSide(buffer.getCombatAgent());
			if (!temp.isEmpty()) {
				CombatAgent testEnemy = temp.get(0);
				for (CombatCondition cc : testEnemy.getCombatConditions()) 
					if (cc instanceof DamageReduction) vulnerableToMagic = true;
			}
		}

		if (vulnerableToMagic && buffer.hasSpell("Magic Weapon")) {
			// now need to determine target.
			// this should be a member of the party who has target
			// and does not currently have a Magic weapon
			if (!inParty) {
				// must be self
				if (!buffer.getCurrentCM().isMagicAttack())
					target = buffer;
			} else {
				for (Character partyMember : mainParty.getMembers()) {
					if (partyMember.getCurrentTarget() != null) {
						CombatModifier cm = partyMember.getCurrentCM();
						if (!cm.isMagicAttack())
							target = partyMember;
					}
				}
			}
			if (target != null) {
				spellCast = new MagicWeapon();
				spellChosen = true;
			}
		}

		if (bless && !spellChosen) {
			// Bless has already been cast on the party,
			// so instead we see if Bulls Strength or Shield of Faith is appropriate
			// We always go for the higher level spells first
			// TODO: Allow BUFF_HIGH and BUFF_LOW as two strategies. the only difference
			// being a preference for high level spells or low level spells first

			CombatAgent ca = buffer.getCombatAgent();
			if (buffer.hasSpell("Bulls Strength") && buffer.getMp() > 2) {
				if (!inParty) {
					// must be self
					if (ca.getCurrentCM().getStrengthBonus() < 1)
						target = buffer;
				} else {
					for (Character partyMember : mainParty.getMembers()) {
						if (partyMember.getCurrentTarget() != null && 
								partyMember.getCurrentCM().getStrengthBonus() < 1) {
							// Note that for Bulls Strength, we select a target who
							// has a target
							// For shield of Faith is is someone who is being targeted
							target = partyMember;
							break;
						}
					}
				}
				if (target != null)  {
					spellCast = new BullsStrength();
					spellChosen = true;
				}

			}

			if (!spellChosen && buffer.hasSpell("Shield of Faith")) {
				// now need to determine target.
				// this should be a member of the party who is targeted
				// and does not currently have a Deflection bonus
				if (!inParty) {
					// must be self
					if (buffer.getCurrentCM().getDeflection() < 1)
						target = buffer;
				} else {
					for (Character partyMember : mainParty.getMembers()) {
						if (partyMember.isTargetted()) {
							// We cannot use the currentCM - as that assume the target is the attacker
							CombatModifier cm = new CombatModifier(null, partyMember.getCombatAgent(), null);
							if (cm.getDeflection() <  1)
								target = partyMember;
						}
					}
				}
				if (target != null) {
					spellCast = new ShieldOfFaith();
					spellChosen = true;
				}
			}
		}

		if (!spellChosen && !bless && buffer.hasSpell("Bless")) {
			target = mainParty;
			spellCast = new Bless();
			spellChosen = true;
		}


		if (spellCast != null && target != null) {
			// and finally - if not distracted, then cast the spell
			if (!spellCast.isDistracted(buffer)) spellCast.cast(buffer, target);

		} else {	// Did not find any appropriate spell to cast
			Action defaultAction = null;
			CombatAgent ca = buffer.getCombatAgent();
			if (buffer.getCurrentTarget() != null && !buffer.getCurrentTarget().isDead()) {
				defaultAction = new Attack(ca, ca.getCurrentTarget(), false, false);
			} else {
				defaultAction = new Defend(ca, false);
			}
			defaultAction.run();
		}
	}

	public String toString() {return "BUFF";}
}
