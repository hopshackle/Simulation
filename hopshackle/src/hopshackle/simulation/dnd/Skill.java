package hopshackle.simulation.dnd;

import java.util.logging.Logger;

public class Skill {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	protected Character skillPossessor;
	protected int level;
	public static enum skills {
		CRAFT, 
		UNARMED_COMBAT,
		WEAPON_PROFICIENCY_SIMPLE,
		WEAPON_PROFICIENCY_MARTIAL,
		WEAPON_PROFICIENCY_BASTARD_SWORD,
		WEAPON_FOCUS_MARTIAL,
		WEAPON_FOCUS_BASTARD_SWORD,
		WEAPON_SPECIALIZATION,
		DODGE};
	protected skills name;

	public Skill(Character skillPossessor, skills name) {
		if (skillPossessor.hasSkill(name)) {
			// Should not be here
			logger.severe(name + " skill being added to Character who already has it");
		} else {
			this.skillPossessor = skillPossessor;
			this.name = name;
			skillPossessor.addSkill(this);
		}
	}

	public Character getSkillPossessor() {
		return skillPossessor;
	}

	public int getLevel() {
		return level;
	}

	public skills getName() {
		return name;
	}
	public String toString() {
		return name.toString();
	}
	
}
