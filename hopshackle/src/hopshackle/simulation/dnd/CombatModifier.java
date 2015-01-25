package hopshackle.simulation.dnd;

import hopshackle.simulation.World;

public class CombatModifier {

	/* This just holds details of combat modifiers
	 * Based on all the different conditions in play
	 */
	private Fight context;
	private CombatAgent attacker;
	private CombatAgent defender;
	private int naturalArmour;
	private int armour;
	private int shield;
	private int deflection;
	private int dodge;
	private int toHit;
	private int damage;
	private int morale;
	private int size;
	private int attackerStrBonus;
	private int attackerStrength;
	private boolean isMagic = false;
	
	public static Character dummyTarget;
	static {
		dummyTarget = new Character(Race.GOBLIN, CharacterClass.WARRIOR, new World());
		dummyTarget.setDexterity(new Attribute(14));
		dummyTarget.setStrength(new Attribute(9));
		// make this concrete to avoid random results that stop unit tests working
		dummyTarget.addXp(2000);
		dummyTarget.addItem(Weapon.LONG_SWORD);
		dummyTarget.addItem(Armour.STUDDED_LEATHER_ARMOUR);
		dummyTarget.addItem(Shield.LARGE_SHIELD);
	}
	
	public CombatModifier(CombatAgent att, CombatAgent def, Fight context) {
		this.context = context;
		this.attacker = att;
		this.defender = def;
		
		if (defender == null || defender.equals(attacker))
			defender = dummyTarget.getCombatAgent();
	
		if (attacker == null)
			attacker = dummyTarget.getCombatAgent();
		
		int maxDexBonus = 20;
		armour = 0;
		shield = 0;
		dodge = 0;
		deflection = 0;
		naturalArmour = 0;
		toHit = 0;
		damage = 0;
		attackerStrength = 11;
		attackerStrBonus = 0;
		
		/* The following modifiers are seen as 'core'.
		 * This means they are not processed via CombatConditions, but within
		 * the main CombatModifier (i.e. here)
		 * If a CombatCondition overrides one of these, then it has to subtract the relevant modifiers
		 * as well as implementing itself (e.g. Weapon Finesse; Flat-Footed)
		 * 		- Base Hit Modifier
		 * 		- Weapon Proficiency
		 * 		- Base strength for To Hit and Damage
		 * 		- Armour, shield and Dex bonuses
		 * 		- Weapon bonuses
		 */
		
		if (defender.getAgent() instanceof Character) {
			Character d = (Character) defender.getAgent();
			Armour a = d.getArmour();
			if (a != null) {
				armour = a.getACBonus();
				maxDexBonus = a.getMaxDexBonus();
			}
			Shield s = d.getShield();
			if (s!=null)
				shield = s.getACBonus();
			dodge = Math.min(d.getDexterity().getMod(), maxDexBonus);
		}

		if (attacker.getAgent() instanceof Character) {
			Character a = (Character) attacker.getAgent();
			attackerStrength = a.getStrength().getValue();
			toHit += a.getBaseHitMod();
			Weapon w = a.getWeapon();
			if (w != null) {
				toHit += w.getHitBonus();
				damage += w.getDmgBonus();
				isMagic = w.isMagic();
				if (!a.isProficient(w)) {
					toHit -= 4;
				} 
			}
		}
		
		processModifiers();
	}

	public int getArmour() {
		return armour;
	}

	public void setArmour(int armour) {
		this.armour = armour;
	}

	public int getDeflection() {
		return deflection;
	}

	public void setDeflection(int deflection) {
		this.deflection = deflection;
	}

	public int getDodge() {
		return dodge;
	}

	public void setDodge(int dodge) {
		this.dodge = dodge;
	}

	public int getNaturalArmour() {
		return naturalArmour;
	}

	public void setNaturalArmour(int naturalArmour) {
		this.naturalArmour = naturalArmour;
	}

	public int getShield() {
		return shield;
	}

	public void setShield(int shield) {
		this.shield = shield;
	}
	
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}

	public CombatAgent getAttacker() {
		return attacker;
	}

	public Fight getContext() {
		return context;
	}

	public CombatAgent getDefender() {
		return defender;
	}
	
	public int getTotalDefenseBonus() {
		if (attacker.getCurrentFight() != null) 
			attacker.log(String.format("Armour: %d, Shield: %d, Dodge: %d, Deflection: %d, NaturalArmour: %d, Size: %d", armour, shield, dodge, deflection, naturalArmour, size));
		return armour+shield+dodge+deflection+naturalArmour+size;
	}
	public int getTotalAttackBonus() {
		int strBonus = 0;
		int actStr = attackerStrength + attackerStrBonus;
		Attribute tempStr = new Attribute(actStr);
		strBonus = tempStr.getMod();
		if (attacker.getCurrentFight() != null) 
			attacker.log(String.format("ToHit: %d, Morale: %d, TotalStr: %d, StrBonus: %d", toHit, morale, strBonus, attackerStrBonus));
		return toHit + morale + strBonus;
	}
	public int getTotalDamageBonus() {
		int strBonus = 0;
		int actStr = attackerStrength + attackerStrBonus;
		Attribute tempStr = new Attribute(actStr);
		strBonus = tempStr.getMod();
		return damage + strBonus;
	}

	public int getDamage() {
		return damage;
	}

	public void setDamage(int damage) {
		this.damage = damage;
	}

	public int getToHit() {
		return toHit;
	}

	public void setToHit(int toHit) {
		this.toHit = toHit;
	}

	public int getMorale() {
		return morale;
	}

	public void setMorale(int morale) {
		this.morale = morale;
	}
	
	private void processModifiers() {
		// the assumption here is that CombatConditions only go on the CombatAgent 
		// i.e. no longer may they be attached to a Party
		for (CombatCondition cc : attacker.getCombatConditions()) {
			cc.apply(this);
			if (attacker.getCurrentFight() != null)
				attacker.log(cc.toString());
		}
		for (CombatCondition cc : defender.getCombatConditions()) {
			cc.apply(this);
			if (attacker.getCurrentFight() != null)
				attacker.log(cc.toString());
		}
	}

	public int getStrengthBonus() {
		return attackerStrBonus;
	}

	public void setStrengthBonus(int i) {
		attackerStrBonus = Math.max(i, attackerStrBonus);
	}
	
	public void setMagicAttack(boolean isMagic) {
		this.isMagic = isMagic;
	}

	public boolean isMagicAttack() {
		return isMagic;
	}
}
