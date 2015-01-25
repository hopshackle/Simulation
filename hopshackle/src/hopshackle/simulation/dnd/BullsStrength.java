package hopshackle.simulation.dnd;

import java.util.ArrayList;

public class BullsStrength extends DurationCombatSpell implements CombatCondition {
	
	public BullsStrength() {
		level = 2;
		name = "Bulls Strength";
	}
	public void implementEffect(Character caster, DnDAgent tgt) {
		super.implementEffect(caster, tgt);
		duration = (int)caster.getLevel() * 10;
		Character c = (Character)target;
		CombatAgent ca = c.getCombatAgent();
		// confirm there is no Bulls Strength already in place
		ArrayList<CombatCondition> currentConditions = ca.getCombatConditions();
		for (CombatCondition cc : currentConditions) {
			if (cc instanceof BullsStrength) return;
		}
		ca.addCondition(this);
		if (caster != target) {
			Reputation.BUFF_ATTACK.apply(caster, 2);
		}
	}
	public void apply(CombatModifier cm) {
		if (cm.getAttacker().getAgent().equals(target)) {
			cm.setStrengthBonus(Math.max(4, cm.getStrengthBonus()));
		}
	}
}
