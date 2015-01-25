package hopshackle.simulation.dnd;


public class WeaponSpecialization extends Skill implements CombatCondition {

	public WeaponSpecialization(Character skillPossessor, skills name) {
		super(skillPossessor, name);
		if (this.skillPossessor == skillPossessor)
			// only not true if the Character already has the skill
			this.skillPossessor.addCondition(this);
	}

	public void apply(CombatModifier cm) {
		if (cm.getAttacker().equals(skillPossessor.getCombatAgent())) {
			cm.setDamage(cm.getDamage()+2);
		}
	}

	public boolean isPermanent() {
		return true;
	}

	public int roundsLeft() {
		return Integer.MAX_VALUE;
	}

}
