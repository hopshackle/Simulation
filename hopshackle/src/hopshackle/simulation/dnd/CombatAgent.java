package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

import java.util.ArrayList;

public interface CombatAgent {

	/*
	 *  interface to define a CombatAgent
	 *  
	 *   CombatAgent is any entity that can take an active part in a Fight. For example
	 *   a normal Agent, or a Summoned creature, or a spell such as Spiritual Hammer
	 *   
	 *   
	 */
	
	public boolean isAttackable(CombatAgent attacker);
	// returns true if the CombatAgent can be targetted by the attacker
	// this allow Invisibility etc. to be taken into account
	
	public Agent getAgent();
	public Character getController();
	// return the controlling Character  - or null if there is none
	
	public ActionType actionToUse();
	// Returns the actionType for the controlling Character to use this
	// i.e. is it a Free / Move / Standard / Concentration action?
	
	public void leavesFight();
	
	public void initialiseFightStatus(Fight f);
	public Fight getCurrentFight();
	public void newRound();
	
	public boolean isDead();
	
	public boolean hasAofO();
	public int getInitiative(Fight f);
	
	public void setCombatDecider(Decider cd);
	public Decider getCombatDecider();
	
	public boolean isTargetted();
	public void setTargetted(boolean targetted);
	
	public void applyDamage(CombatAgent attacker, Weapon w, int damage, boolean toStun);
		// applies damage to this combat agent as the result of a successful attack
	
	public CombatModifier getCurrentCM();
	public void setCurrentCM(CombatModifier cm);
	
	public CombatAgent getCurrentTarget();
	public void setCurrentTarget(CombatAgent target);
	
	public ArrayList<CombatCondition> getCombatConditions();
	public void addCondition(CombatCondition cc);
	public void removeCondition(CombatCondition cc);
	
	public void attack(CombatAgent target, boolean toStun);
		// if target is null, then default to the currentTarget
	
	public double getAvgDmgPerRound(CombatAgent enemy, Weapon w);
	public double getHitChance(CombatAgent enemy, Weapon w);
	
	public boolean isStunned();
	public void setStunned(boolean stunned);
	
	public void log(String message);
	
}
