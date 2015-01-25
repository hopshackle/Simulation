package hopshackle.simulation.dnd;

import java.util.ArrayList;

public class ShieldOfFaith extends DurationCombatSpell implements CombatCondition {
	
	public ShieldOfFaith() {
		level = 1;
		name = "Shield of Faith";
	}
	
	public void implementEffect(Character caster, DnDAgent tgt) {
		super.implementEffect(caster, tgt);
		Character targetC = (Character) target;
		duration = (int)caster.getLevel() * 10;

		// confirm there is no Shield Of Faith already in place
		ArrayList<CombatCondition> currentConditions = targetC.getCombatConditions();
		for (CombatCondition cc : currentConditions) {
			if (cc instanceof ShieldOfFaith) return;
		}
		targetC.addCondition(this);
		if (caster != target) {
			Reputation.BUFF_AC.apply(caster, 2);
		}
	}
	
	public void apply(CombatModifier cm) {
		if (cm.getDefender().getAgent().equals(target)) {
			cm.setDeflection(Math.min(cm.getDeflection()+2, 2));
		}
	}
}
