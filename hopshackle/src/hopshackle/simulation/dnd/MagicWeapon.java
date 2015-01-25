package hopshackle.simulation.dnd;

import java.util.ArrayList;

public class MagicWeapon extends DurationCombatSpell implements CombatCondition {

	public MagicWeapon() {
		level = 1;
		name = "Magic Weapon";
	}

	public void implementEffect(Character caster, DnDAgent tgt) {
		super.implementEffect(caster, tgt);
		Character targetC = (Character) target;
		duration = (int)caster.getLevel() * 10;

		// confirm there is no Magic Weapon already in place
		ArrayList<CombatCondition> currentConditions = targetC.getCombatConditions();
		for (CombatCondition cc : currentConditions) {
			if (cc instanceof MagicWeapon) return;
		}
		targetC.addCondition(this);
	}

	@Override
	public void apply(CombatModifier cm) {
		// if we are the attacker, then...
		if (cm.getAttacker().equals(((Character) target).getCombatAgent())) {
			cm.setMagicAttack(true);

			Weapon weaponUsed = ((Character) target).getWeapon();
			if (weaponUsed != null) {
				if (weaponUsed.getHitBonus() < 1) cm.setToHit(cm.getToHit()+1);
				if (weaponUsed.getDmgBonus() < 1) cm.setDamage(cm.getDamage()+1);
			}
		}
	}

}
