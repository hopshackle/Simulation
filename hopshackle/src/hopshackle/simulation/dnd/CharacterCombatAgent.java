package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

import java.util.*;
import java.util.logging.Logger;

public class CharacterCombatAgent implements CombatAgent {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private Character c;
	private Decider combatDecider;
	private Armour armour;
	private Shield shield;
	private Weapon weapon;
	private boolean targetted;
	private Fight currentFight;
	private CombatModifier currentCM; // to avoid recalculating every time
	private CombatAgent currentTarget;
	private ArrayList<CombatCondition> combatConditions;
	private int attacksOfOpportunity;

	public CharacterCombatAgent(Character c) {
		this.c = c;
		combatConditions = new ArrayList<CombatCondition>();
	}

	public boolean hasCondition(CombatCondition c) {
		boolean alreadyHas = false;
		for (CombatCondition current : combatConditions) {
			if (current.toString().equals(c.toString()))
				alreadyHas = true;
		}
		return alreadyHas;
	}
	public synchronized void addCondition(CombatCondition c) {
		// first make sure we don't already have this as a combat condition
		if (!hasCondition(c)) {
			combatConditions.add(c);
			resetCombatModifier();
		}
	}
	private void resetCombatModifier() {
		currentCM = null;
		// we also need to set the CombatModfier to null on anything
		// that is attacking this agent
		if (this.getCurrentFight() != null) {
			for (CombatAgent att : this.getCurrentFight().getAttackersOf(this)) 
				att.setCurrentCM(null);
		}
	}

	public synchronized void removeCondition(CombatCondition c) {
		combatConditions.remove(c);
		resetCombatModifier();
	}

	public synchronized ArrayList<CombatCondition> getCombatConditions() {
		ArrayList<CombatCondition> retValue = new ArrayList<CombatCondition>();
		for (CombatCondition cc : combatConditions)
			retValue.add(cc);
		return retValue;
	}
	public void setCurrentFight(Fight currentFight) {
		// Remove all non-permanent Combat Conditions

		ArrayList<CombatCondition> temp = new ArrayList<CombatCondition>();
		for (CombatCondition cc : combatConditions) 
			if (!cc.isPermanent()) temp.add(cc);

		for (CombatCondition cc : temp) 
			combatConditions.remove(cc);

		this.currentFight = currentFight;
	}
	public Fight getCurrentFight() {
		return currentFight;
	}

	public void attack(CombatAgent enemy, boolean toStun) {
		attack(enemy, toStun, Dice.roll(1, 20), Dice.roll(1, 20));
	}
	public int attack(CombatAgent enemy, boolean toStun, int hitRoll, int critRoll) {
		int retValue = 0;
		boolean critical = false;
		boolean fumble = false;
		boolean breaksWeapon = false;
		boolean hit = false;
		try {
			c.getLock();

			int critRange = 20;
			if (weapon != null) {
				critRange = weapon.getCritRange();
			}

			currentCM = new CombatModifier(this, enemy, currentFight);

			// this is then used in the other methods to get modifiers

			log("Rolls a " + hitRoll + " to hit");

			if (hitRoll == 1 && critRoll < getHitChance(enemy)) fumble = true;

			if (fumble) {
				log("Fumbles");
				int d = getDamage(this) + getDamage(this);
				if (weapon !=null && d > weapon.getHardness()) {
					log("Breaks weapon");
					c.removeItem(weapon);
					breaksWeapon = true;
				}
			}

			if ((hitRoll >= critRange) && critRoll >= getHitChance(enemy)) critical = true;
			if ((hitRoll == 20) || hitRoll >= getHitChance(enemy)) {
				// a palpable hit
				hit = true;
				int d = getDamage(enemy);
				String logMessage = String.format("%s hits %s for %d points of damage", 
						toString(), enemy.toString(), d);
				if (critical) {
					d = d + getDamage(enemy);
					logMessage = String.format("%s crits %s for %d points of damage", 
							toString(), enemy.toString(), d);
				} 
				log(logMessage);
				enemy.log("Is hit by " + toString());
				if (critical) toStun = false;
				enemy.applyDamage(this, weapon, d, toStun);
				if (c.getParty() !=null && !toStun) {
					if (currentFight.isInternal()) {
						Reputation.DAMAGE_FELLOW.apply(c, d);
					} else {
						Reputation.DAMAGE_ENEMY.apply(c, d);
					}
				}
			}
		} finally {
			c.releaseLock();
		}
		if (hit) retValue +=1;
		if (critical) retValue +=2;
		if (fumble)	retValue +=4;
		if (breaksWeapon) retValue +=8;

		return retValue;
	}	

	public void applyDamage(CombatAgent attacker, Weapon w, int damage, boolean toStun) {
		c.addHp(-damage, toStun);
	}


	public double getAvgDmgPerRound(CombatAgent enemy, Weapon w) {

		if (w == null) w = getWeapon();

		boolean changedWeapon = false;
		Weapon oldWeapon = weapon;
		CombatModifier oldCM = currentCM;

		if (!w.equals(oldWeapon)) {
			changedWeapon = true;
			weapon = w;
			currentCM = new CombatModifier(this, enemy, currentFight);
			// have to re-initialise to pick up weapon
		}
		if (currentCM == null || currentCM.getDefender() != enemy)
			currentCM = new CombatModifier(this, enemy, currentFight);

		double hit = getHitChance(enemy, w);
		double crit = getCritChance(enemy);
		double hitDmg = getAvgDmgOnHit();
		double critDmg = getAvgDmgOnCrit();

		if (changedWeapon){
			weapon = oldWeapon;
			currentCM = oldCM;
		}

		return ((hit - crit) * hitDmg) + (crit * critDmg);
	}
	private double getAvgDmgOnHit() {
		// Assuming a hit, calculate average damage
		Weapon currentWeapon = getWeapon();
		double retValue = 0;

		if (currentCM != null)
			retValue += currentCM.getTotalDamageBonus();

		if (retValue > -1) {
			retValue += (currentWeapon.getDmgDie()+1)/2.0;
		} else {
			// in this case we have a non-linear set up
			double barrier = -retValue;
			double weaponDie = currentWeapon.getDmgDie();
			retValue = ((weaponDie - barrier)/weaponDie) * (currentWeapon.getDmgDie()+1-barrier)/2.0;
			// this says that the number on the weapon die that are under or equal to the barrier
			// count as zero, and those above have the subtraction applied
			if (barrier >= weaponDie) retValue = 0.0;
		}

		if (retValue < 0.0) retValue = 0.0;
		return retValue;
	}
	private double getAvgDmgOnCrit() {
		return getAvgDmgOnHit() * 2.0;
	}

	private double getCritChance(CombatAgent enemy) {
		// Chance 
		int critRange = 20;
		Weapon currentWeapon = getWeapon();
		critRange = currentWeapon.getCritRange();
		double baseCritChance = (double)((21 - critRange))/20;
		double hitChance = getHitChance(enemy, currentWeapon);

		if (hitChance < baseCritChance) {baseCritChance = hitChance;}
		// For the cases where crit Range can be met, and still miss

		return baseCritChance * hitChance;
	}

	public double getHitChance(CombatAgent enemy, Weapon w) {
		// roll required on a d20 to hit - returns this as a percentage,
		// i.e. if an 11 is required, 0.50 is returned; if a 1, then 0.55 etc.

		// if w is null, then use the currently held weapon
		// if enemy is null, then use the current target

		int retValue;
		retValue = 10;

		if (w == null) w = getWeapon();

		if (enemy == null) enemy = getCurrentTarget();
		// note - if this is null, then within CombatModifier, the dummyTarget will be automatically used

		boolean changedWeapon = false;
		Weapon oldWeapon = weapon;
		CombatModifier oldCM = currentCM;

		if (!w.equals(oldWeapon)) {
			changedWeapon = true;
			weapon = w;
			currentCM = new CombatModifier(this, enemy, currentFight);
			// have to re-initialise to pick up weapon
		}

		if (currentCM == null) currentCM = new CombatModifier(this, enemy, currentFight);


		try {
			retValue = retValue - currentCM.getTotalAttackBonus() + currentCM.getTotalDefenseBonus();
		} catch (NullPointerException e) {
			e.printStackTrace();
			logger.severe("Error in null currentCM in CharacterCombatAgent");
		}

		if (currentFight != null) log ("To Hit needed after CM is: " + retValue);
		if (retValue > 20) {retValue = 20;}

		if (changedWeapon){
			weapon = oldWeapon;
			currentCM = oldCM;
		}
		// and finally convert to double
		return ((double)(21 - retValue))/20.0;
	}
	private int getHitChance(CombatAgent enemy) {
		return (int)(20 * (1.05 - getHitChance(enemy, weapon)));
	}

	private int getDamage(CombatAgent enemy) {
		int dmgDie = c.getRace().dmgDie;
		if (weapon != null) dmgDie = weapon.getDmgDie();
		int damage = Dice.roll(1,dmgDie);

		log("Base damage of " + damage);

		if (currentCM != null)
			damage += currentCM.getTotalDamageBonus();

		log("Final damage of " + damage);

		if (damage<0) damage = 0;
		return damage;
	}


	public void setStunned(boolean state) {
		c.addHp(-c.getHp(), true);
	}
	public boolean isStunned(){
		return (c.getTempHp() >= c.getHp());
	}


	public Armour getArmour() {
		return armour;
	}
	public Shield getShield() {
		return shield;
	}
	public Weapon getWeapon() {
		Weapon retValue = weapon;
		if (retValue == null) return Weapon.FIST;
		return retValue;
	}


	public void checkInventory() {
		List<Artefact> inventory = c.getInventory();
		if (armour!=null && !inventory.contains(armour)) {
			armour = null;
		}
		if (shield!=null && !inventory.contains(shield)) {
			shield = null;
		}
		if (weapon!=null && !inventory.contains(weapon)) {
			weapon = Weapon.FIST;
		}
		for (Artefact item : inventory) {
			if (item instanceof Armour) {
				Armour a = (Armour) item;
				if (a.getACChange(c) > 0) {
					armour = a;
					log("Equips " + a);
					resetCombatModifier();
				}
			}
			if (item instanceof Shield) {
				Shield s = (Shield) item;
				if (s.getACChange(c) > 0) {
					shield = s;
					log("Equips " + s);
					resetCombatModifier();
				}
			}
			if (item instanceof Weapon) {
				Weapon w = (Weapon) item;
				if (w.getAvgDmgChange(c) > 0) {
					weapon = w;
					log("Equips " + w);
					resetCombatModifier();
				}
			}
		}
	}

	public int getInitiative(Fight f) {
		return Dice.roll(1, 20) + c.getDexterity().getMod();
		// currently the context is not relevant, but it is likely
		// to become so
	}
	public void initialiseFightStatus(Fight f) {
		if (currentFight != null) {
			logger.severe(toString() +" trying to initialise Combat when in Combat");
		}

		c.getLock();

		setCurrentFight(f);
		targetted = false;
		currentTarget = null;
		if (c.getDebugLocal()) f.setDebug(true);
		// to ensure that if ANY of the participants in a fight
		// are in debug, then the whole fight is detailed for all participants
		// default flags for combat status
		newRound();
	}

	public CombatAgent getCurrentTarget() {
		return currentTarget;
	}

	public void setCurrentTarget(CombatAgent currentTarget) {
		this.currentTarget = currentTarget;
	}

	public boolean isTargetted() {
		return targetted;
	}

	public void setTargetted(boolean targetted) {
		this.targetted = targetted;
	}

	public void leavesFight() {
		setCurrentFight(null);
		targetted = false;
		currentTarget = null;

		// and then remove all inactive spells
		ArrayList<Spell> currentSpells = c.getActiveSpells();
		ArrayList<Spell> spellsToRemove = new ArrayList<Spell>();
		for (Spell oldSpell : currentSpells) {
			if (!oldSpell.isActive())
				spellsToRemove.add(oldSpell);
		}
		for (Spell oldSpell : spellsToRemove)
			c.spellExpires(oldSpell);

		c.releaseLock();
	}

	public CombatModifier getCurrentCM() {
		if (currentCM == null) {
			// then set one up
			currentCM = new CombatModifier(this, currentTarget, currentFight);
		}
		return currentCM;
	}

	public void setCurrentCM(CombatModifier currentCM) {
		this.currentCM = currentCM;
	}

	public Decider getCombatDecider() {
		return combatDecider;
	}

	public void setCombatDecider(Decider combatDecider) {
		this.combatDecider = combatDecider;
	}
	public void log(String s) {
		c.log(s);
	}

	public ActionType actionToUse() {
		return null;
	}

	public Agent getAgent() {
		return c;
	}
	public Character getController() {
		return c;
	}

	public boolean hasAofO() {
		return attacksOfOpportunity>0;
	}

	public boolean isAttackable(CombatAgent attacker) {
		return true;
	}

	public boolean isDead() {
		return c.isDead();
	}

	public void newRound() {
		attacksOfOpportunity = 1;
		removeExpiredCombatConditions();
	}

	private void removeExpiredCombatConditions() {
		ArrayList<CombatCondition> temp = this.getCombatConditions();
		ArrayList<CombatCondition> remList = new ArrayList<CombatCondition>();
		if (temp != null) {
			for (CombatCondition cc : temp) {
				if (cc.roundsLeft() < 1)
					remList.add(cc);
			}
			for (CombatCondition cc : remList) {
				this.removeCondition(cc);
				log("Removed " + cc);
			}
		}
	}

	public String toString() {
		return getAgent().toString();
	}
}

