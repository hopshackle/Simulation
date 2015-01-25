package hopshackle.simulation.dnd;


public class WeaponFocus extends Skill implements CombatCondition {
	
	public WeaponFocus(Character skillPossessor, skills name) {
		super(skillPossessor, name);
		this.skillPossessor.addCondition(this);
	}

	public void apply(CombatModifier cm) {
		if (cm.getAttacker().equals(skillPossessor.getCombatAgent()) && skillPossessor.getWeapon() != null) {
			Weapon w = skillPossessor.getWeapon();
			if (w.getFocusSkill() == name)
				cm.setToHit(cm.getToHit()+1);
		}
	}

	public boolean isPermanent() {
		return true;
	}

	public int roundsLeft() {
		return Integer.MAX_VALUE;
	}

}
