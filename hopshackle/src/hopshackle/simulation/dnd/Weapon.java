package hopshackle.simulation.dnd;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Skill.skills;

import java.util.ArrayList;

public enum Weapon implements DnDArtefact, HasBaseCost {

	FIST					(4, 0, 0, 20, 0, 100,
							skills.UNARMED_COMBAT,
							null, 0, false),
	LIGHT_MACE 				(6, 0, 0, 20, 12, 20,
							skills.WEAPON_PROFICIENCY_SIMPLE,
							null, 5, false),
	HEAVY_MACE 				(8, 0, 0, 20, 12, 30,
							skills.WEAPON_PROFICIENCY_SIMPLE,
							null, 12, false),
	SHORT_SWORD 			(6, 0, 0, 19, 15, 12,
							skills.WEAPON_PROFICIENCY_MARTIAL,
							skills.WEAPON_FOCUS_MARTIAL, 10, false),
	LONG_SWORD				(8, 0, 0, 19, 15, 15,
							skills.WEAPON_PROFICIENCY_MARTIAL,
							skills.WEAPON_FOCUS_MARTIAL, 15, false),
	BASTARD_SWORD			(10, 0, 0, 19, 18, 17,
							skills.WEAPON_PROFICIENCY_BASTARD_SWORD,
							skills.WEAPON_FOCUS_BASTARD_SWORD, 35, false),
	MASTERWORK_SHORT_SWORD	(6, 1, 0, 19, 20, 17,
							skills.WEAPON_PROFICIENCY_MARTIAL,
							skills.WEAPON_FOCUS_MARTIAL, 110, false),
	MASTERWORK_LONG_SWORD	(8, 1, 0 ,19, 20, 20,
							skills.WEAPON_PROFICIENCY_MARTIAL,
							skills.WEAPON_FOCUS_MARTIAL, 115, false),
	LONG_SWORD_PLUS_1		(8, 1, 1, 19, 21, 25,
							skills.WEAPON_PROFICIENCY_MARTIAL,
							skills.WEAPON_FOCUS_MARTIAL, 500, true),
	MASTERWORK_BASTARD_SWORD (10, 1, 0, 19, 20, 22,
							skills.WEAPON_PROFICIENCY_BASTARD_SWORD,
							skills.WEAPON_FOCUS_BASTARD_SWORD, 135, false),
	BASTARD_SWORD_PLUS_1 	(10, 1, 1, 19, 21, 27,
							skills.WEAPON_PROFICIENCY_BASTARD_SWORD,
							skills.WEAPON_FOCUS_BASTARD_SWORD, 500, true);
	int damageDie;
	int makeDC;
	int damageBonus;
	int toHitBonus;
	int critRange;
	int hardness;
	Skill.skills proficiency;
	Skill.skills focus;
	double makeCost;
	boolean isMagic;

	Weapon (int dmgDie, int hitBonus, int dmgBonus, int crit, int DC, int hard,
			Skill.skills profType, Skill.skills focusSkill, double makeCost, boolean magic) {
		damageDie = dmgDie;
		makeDC = DC;
		damageBonus = dmgBonus;
		toHitBonus = hitBonus;
		critRange = crit;
		proficiency = profType;
		focus = focusSkill;
		hardness = hard;
		this.makeCost = makeCost;
		isMagic = magic;
	}

	public int getACChange(Agent owner) {
		return 0;
	}
	public double getOneOffHeal() {return 0.0;}

	public double getAvgDmgChange(Agent owner) {
		Character c = (Character) owner;
		boolean debugLevel = c.getDebugLocal();
		c.setDebugLocal(false);

		Weapon currentWeapon = c.getWeapon();
		if (currentWeapon == null) currentWeapon = Weapon.FIST;
		CombatAgent ca = c.getCombatAgent();
		double oldAvgDmg = ca.getAvgDmgPerRound(ca, currentWeapon);
		double newAvgDmg = ca.getAvgDmgPerRound(ca, this);
		
		c.setDebugLocal(debugLevel);
		return newAvgDmg - oldAvgDmg;
	}

	public int getMakeDC() {
		return makeDC;
	}

	public int getCritRange() {
		return critRange;
	}

	public int getDmgBonus() {
		return damageBonus;
	}

	public int getDmgDie() {
		return damageDie;
	}

	public int getHitBonus() {
		return toHitBonus;
	}
	public int getHitBonus(Agent owner) {
		Character c = (Character) owner;
		Weapon currentWeapon = c.getWeapon();
		if (currentWeapon == null) currentWeapon = Weapon.FIST;
		return toHitBonus - currentWeapon.toHitBonus;
	}
	public Skill.skills getProficiencyGroup() {
		return proficiency;
	}
	public Skill.skills getFocusSkill() {
		return focus;
	}

	public int getHardness() {
		return hardness;
	}
	public boolean isMagic() {
		return isMagic;
	}
	public Recipe getRecipe() {

		int gold = 0;
		double metal = 0.0, wood = 0.0;
		ArrayList<Artefact> otherIngredients = new ArrayList<Artefact>();
		switch (this) {
		case FIST:
			return null;
		case LIGHT_MACE:
			wood = 1.0;
			break;
		case HEAVY_MACE:
			wood = 1.0;
			metal = 0.5;
			break;
		case SHORT_SWORD:
			metal = 0.75;
			break;
		case LONG_SWORD:
			metal = 1.25;
			break;
		case BASTARD_SWORD:
			metal = 3.0;
			break;
		case MASTERWORK_SHORT_SWORD:
			metal = 1.5;
			break;
		case MASTERWORK_LONG_SWORD:
			metal = 2.5;
			break;
		case LONG_SWORD_PLUS_1:
			otherIngredients.add(MASTERWORK_LONG_SWORD);
			for (int n=1; n<5; n++) otherIngredients.add(Component.GRICK_BEAK);
			break;
		case MASTERWORK_BASTARD_SWORD:
			metal = 6.0;
			break;
		case BASTARD_SWORD_PLUS_1:
			otherIngredients.add(MASTERWORK_BASTARD_SWORD);
			for (int n=1; n<5; n++) otherIngredients.add(Component.GRICK_BEAK);
			break;
		}
		Recipe retValue = new Recipe(this, gold);
		retValue.addIngredient(Resource.WOOD, wood);
		retValue.addIngredient(Resource.METAL, metal);
		for (Artefact ingredient : otherIngredients)
			retValue.addIngredient(ingredient, 1);

		return retValue;
	}

	public double costToMake(Agent a) {
		return DNDArtefactUtilities.costToMake(this, a);
	}

	public long getTimeToMake(Agent a) {
		return DNDArtefactUtilities.timeToMake(this, a);
	}

	public double getBaseCost() {
		return makeCost;
	}

	@Override
	public double getMagicAttack() {
		if (isMagic) return damageBonus;
		return 0;
	}
	@Override
	public boolean isA(Artefact item) {
		return (item.equals(this));
	}

	@Override
	public void changeOwnership(Agent newOwner) {
	}

	@Override
	public boolean isInheritable() {
		return false;
	}
}
