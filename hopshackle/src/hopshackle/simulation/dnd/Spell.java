package hopshackle.simulation.dnd;

import hopshackle.simulation.Dice;


public abstract class Spell {

	protected int level;
	protected int duration;	// in rounds
	protected String name;

	public boolean cast(Character caster, DnDAgent target) {
		if (caster.getMp() < magicPointsNeeded(level)) {
			return false;
		}
		caster.addMp(-magicPointsNeeded(level));
		caster.spellCast(this);
		implementEffect(caster, target);
		caster.log("Casts " + name);
		return true;
	}

	abstract public void implementEffect(Character caster, DnDAgent target);

	abstract public boolean isActive();

	public int getLevel() {
		return level;
	}
	public String getName() {
		return name;
	}
	public boolean isDistracted(Character caster) {
		// We check to see if we are in combat, and the number of attackers
		// Assumption is that 1-2 attackers, a 5' step is made
		// with 3-4 then defensive casting is used 
		// also assume that Clerics have 4 + level ranks in Concentration

		boolean distracted = false;
		if (caster.isInCombat()) {
			Fight f = caster.getCurrentFight();
			CombatAgent ca = caster.getCombatAgent();
			int attackers = f.getPossibleAO(ca);
			if (attackers > 2) {
				// Now we have to make concentration roll
				if (Dice.roll(1,20) <= 15 - caster.getConstitution().getMod() - caster.getLevel() - 4) {
					caster.log("Fails concentration roll");
					distracted = true;
				}
			}
		}
		return distracted;
	}

	protected static int magicPointsNeeded(int spellLevel) {
		int retValue = 0;
		for (int n =1; n <= spellLevel; n++) {
			retValue += n;
		}
		return retValue;
	}

}
