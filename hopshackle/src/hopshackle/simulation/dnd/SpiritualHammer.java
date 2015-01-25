package hopshackle.simulation.dnd;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.actions.Attack;
import hopshackle.simulation.dnd.genetics.CombatActionsI;

import java.util.ArrayList;

public class SpiritualHammer extends DurationCombatSpell implements CombatAgent, CombatCondition {

	private Character caster;
	CombatModifier currentCM;
	Decider combatDecider;
	ArrayList<CombatCondition> combatConditions;
	int attacksMade;
	
	public SpiritualHammer() {
		level = 2;
		name = "Spiritual Hammer";
	}
	
	@Override
	public void implementEffect(Character caster, DnDAgent tgt) {
		// We add the spell as a Combat Agent to the fight that the
		// caster is currently engaged in
		// we then set up the CombatDecider 
		final SpiritualHammer spiritualHammer = this;
		this.caster = caster;
		
		combatConditions = new ArrayList<CombatCondition>();
		combatConditions.add(this);
		duration = (int)caster.getLevel()+1; // it vanishes after the attack on the last round
		attacksMade = 0;
		
		super.implementEffect(caster, tgt);
		contextFight.addCombatant(this, caster.getCombatAgent());
		
		combatDecider = new BaseDecider(null, null){
			@Override
			public ActionEnum decide(Agent a1, Agent a2) {
				// Spiritual Hammer always attacks
				// preferred opponent is the one the caster is fighting
				CombatAgent opponent = spiritualHammer.getController().getCurrentTarget();
				if (opponent == null) {
					opponent = contextFight.getOpponent(spiritualHammer, true);
				}
				final CombatAgent finalOpponent = opponent;
				attacksMade++;
				return new ActionEnum() {
				
					public boolean isChooseable(Agent a) {return false;}
					public String getChromosomeDesc() {return null;}
					public Action getAction(Agent a1, Agent a2) {
						return getAction(a1);
					}
				
					public Action getAction(Agent a) {
						return new Attack(spiritualHammer, finalOpponent, false, false);
					}
					
					@Override
					public Enum<CombatActionsI> getEnum() {
						return CombatActionsI.ATTACK;
					}
				
				};

			}
			@Override
			public double valueOption(ActionEnum option, Agent decidingAgent, Agent contextAgent) {
				return 0;
			}
		};

		// and that should be it
	}

	public ActionType actionToUse() {
		return null;
	}

	public void addCondition(CombatCondition cc) {
		// There should be no Combat conditions that are relevant to 
		// a Spiritual Hammer
	}

	public void applyDamage(CombatAgent attacker, Weapon w, int damage,
			boolean toStun) {
		// A spiritual hammer is invulnerable to attacks - it can just be dispelled
	}

	
	public void attack(CombatAgent enemy, boolean toStun) {
		attack(enemy, toStun, Dice.roll(1, 20), Dice.roll(1, 20));
	}
	public int attack(CombatAgent enemy, boolean toStun, int hitRoll, int critRoll) {
		int retValue = 0;
		boolean critical = false;
		boolean hit = false;

		if (attacksMade > caster.getLevel()) return 0;

		int critRange = 20;

		currentCM = new CombatModifier(this, enemy, this.getCurrentFight());
		// this is then used in the other methods to get modifiers

		log("Rolls a " + hitRoll + " to hit");

		if ((hitRoll >= critRange) && critRoll >= getHitChance(enemy)) critical = true;
		if ((hitRoll == 20) || hitRoll >= getHitChance(enemy)) {
			// a palpable hit
			hit = true;
			int d = Dice.roll(1, 8);
			String logMessage = String.format("%s hits %s for %d points of damage", 
					toString(), enemy.toString(), d);
			if (critical) {
				d = d + Dice.roll(1, 8);
				logMessage = String.format("%s crits %s for %d points of damage", 
						toString(), enemy.toString(), d);
			} 
			log(logMessage);
			enemy.log("Is hit by " + toString());
			enemy.applyDamage(this, null, d, false);
			if (caster.getParty() !=null) {
				if (this.getCurrentFight().isInternal()) {
					Reputation.DAMAGE_FELLOW.apply(caster, d);
				} else {
					Reputation.DAMAGE_ENEMY.apply(caster, d);
				}
			}
		}
		currentCM = null;

		if (hit) retValue +=1;
		if (critical) retValue +=2;

		return retValue;
	}

	public Agent getAgent() {
		return caster;
	}

	public double getAvgDmgPerRound(CombatAgent enemy, Weapon w) {
		return 0;
	}

	public ArrayList<CombatCondition> getCombatConditions() {
		return combatConditions;
	}

	public Decider getCombatDecider() {
		return combatDecider;
	}

	public Character getController() {
		return caster;
	}

	public CombatModifier getCurrentCM() {
		return currentCM;
	}

	public Fight getCurrentFight() {
		return contextFight;
	}

	public CombatAgent getCurrentTarget() {
		return contextFight.getOpponent(this, false);
	}

	public double getHitChance(CombatAgent enemy, Weapon w) {
		// roll required on a d20 to hit - returns this as a percentage,
		// i.e. if an 11 is required, 0.50 is returned; if a 1, then 0.55 etc.

		// if w is null, then use the currently held weapon
		// if enemy is null, then use the current target

		int retValue;
		retValue = 10;

		if (enemy == null) enemy = getCurrentTarget();
		// note - if this is null, then within CombatModifier, the dummyTarget will be automatically used

		if (currentCM == null) currentCM = new CombatModifier(this, enemy, contextFight);

		retValue = retValue - currentCM.getTotalAttackBonus() + currentCM.getTotalDefenseBonus();

		if (contextFight != null) log ("To Hit needed after CM is: " + retValue);
		if (retValue > 20) {retValue = 20;}

		// and finally convert to double
		return ((double)(21 - retValue))/20.0;
	}
	private int getHitChance(CombatAgent enemy) {
		return (int)(20 * (1.05 - getHitChance(enemy, null)));
	}

	public int getInitiative(Fight f) {
		return caster.getCombatAgent().getInitiative(f);
	}

	public boolean hasAofO() {
		return false;
	}

	public void initialiseFightStatus(Fight f) {
		// nothing to do - all done in constructor
	}

	public boolean isAttackable(CombatAgent attacker) {
		return false;
	}

	public boolean isDead() {
		return false;
	}

	public boolean isStunned() {
		return false;
	}

	public boolean isTargetted() {
		return false;
	}

	public void leavesFight() {
		// do nothing - outside a fight, we have no existence
	}

	public void log(String message) {
		caster.log(message + " (Spiritual Hammer)");
	}

	public void newRound() {
		// this is where we need to determine if the spell has expired, and we must remove the 
		// Spiritual Hammer from the Fight
		if (attacksMade >= caster.getLevel()) {
			// we're done
			contextFight.removeCombatant(this);
		}
	}

	public void removeCondition(CombatCondition cc) {
	}

	public void setCombatDecider(Decider cd) {
		// cannot override this on a Spiritual Hammer
	}

	public void setCurrentCM(CombatModifier cm) {
		currentCM = null;
	}

	public void setCurrentTarget(CombatAgent target) {
	}

	public void setStunned(boolean stunned) {
		// No such concept
	}

	public void setTargetted(boolean targetted) {
		// not possible
	}

	public void apply(CombatModifier cm) {
		if (cm.getAttacker().equals(this)) {
			int deltaStrToWis = 0;
			deltaStrToWis = caster.getWisdom().getValue()-caster.getStrength().getValue();
			cm.setStrengthBonus(deltaStrToWis);
			// A spiritual hammer user the caster's wisdom modifier To Hit
			// the combatModifier will be using the caster's strength
			cm.setToHit(caster.getBaseHitMod());
			// we need to ignre any effects from Masterwork weapons etc.
			// this is ugly - as it relies on knowning that Combat Conditions are processed
			// within Combat Modifier after the Base Hit and Weapon modifications
			cm.setMagicAttack(true);
		}
	}
	
	public String toString() {
		return "Spiritual Hammer of " + caster.toString();
	}
}

