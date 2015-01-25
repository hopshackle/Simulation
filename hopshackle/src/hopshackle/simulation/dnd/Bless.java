package hopshackle.simulation.dnd;

import java.util.ArrayList;

public class Bless extends DurationCombatSpell implements CombatCondition {

	public Bless() {
		level = 1;
		name = "Bless";
	}
	
	public void implementEffect(Character caster, DnDAgent tgt) {
		super.implementEffect(caster, tgt);
		
		duration = (int)caster.getLevel() * 10;

		ArrayList<Character> targetList = target.getMembers();

		for (Character c : targetList) {
			// confirm there is no Bless already in place
			CombatAgent ca = c.getCombatAgent();
			ArrayList<CombatCondition> currentConditions = ca.getCombatConditions();
			for (CombatCondition cc : currentConditions) {
				if (cc instanceof Bless) continue;
			}
			ca.addCondition(this);
		}
		if (caster != target) {
			int affected = targetList.size();
			affected = Math.max(affected, contextFight.getOtherSideSize(caster.getCombatAgent())*4);
			// on the basis that maximum number to benefit is the number who can attack
			Reputation.BUFF_ATTACK.apply(caster, affected);
		}
	}
	
	public void apply(CombatModifier cm) {
		// target is either the individual, or the party to which they belong
		Character c = (Character) cm.getAttacker().getAgent();
		if (c.equals(target) || 
				 (c.getParty() != null && c.getParty().equals(target))) {
			
			cm.setMorale(Math.min(cm.getMorale()+1, 1));
		}
	}
}
