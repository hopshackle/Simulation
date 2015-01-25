package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

import java.util.*;
import java.util.logging.Logger;

public class FightTracker implements Teacher<Character> {

	private Fight f;
	private ArrayList<CombatAgent> allCombatants, side1, side2;
	private Integer starting1, starting2;
	private Integer[] startingHealth;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");
	private HashMap<Character, List<ExperienceRecord>> teachingEpisodeHash;

	public FightTracker(Fight parent) {
		f = parent;
		/** 
		 * 	We need to set up the starting conditions. For each side we have to record:
		 *  - starting hp and mp
		 *  
		 *  For each combatant we have to store 
		 *  - starting hp and mp
		 *  
		 */
		allCombatants = f.getAllCombatants();
		side1 = f.getSide(allCombatants.get(0));
		side2 = f.getOtherSide(allCombatants.get(0));
		starting1 = 0;
		starting2 = 0;
		startingHealth = new Integer[allCombatants.size()];
		teachingEpisodeHash = new HashMap<Character, List<ExperienceRecord>>();
		for (int n = 0; n<allCombatants.size(); n++) {
			CombatAgent ca = allCombatants.get(n);
			Agent a = ca.getAgent();
			if (a == null || !(a instanceof Character)) continue;
			Character c = (Character) a;
			startingHealth[n] = getHealth(c);
			if (side1.contains(ca)) {
				starting1 += startingHealth[n];
			} else {
				starting2 += startingHealth[n];
			}
			if (c.getCombatDecider() != null) {
				List<ExperienceRecord> te = new ArrayList<ExperienceRecord>();
				teachingEpisodeHash.put(c, te);
				c.getCombatDecider().setTeacher(this);
			}
		}
	}

	public void resolveFight() {
		for (Character ch : teachingEpisodeHash.keySet()) {
			Decider combatDecider = ch.getCombatDecider();
			double[] newState = combatDecider.getCurrentState(ch, ch);
			List<ActionEnum> emptyList = new ArrayList<ActionEnum>();
			List<ExperienceRecord> te = teachingEpisodeHash.get(ch);
			if (te != null && !te.isEmpty())
				for (ExperienceRecord td : te) {
					td.updateWithResults(rewardFunction(ch) * 1000.0, newState, emptyList, true);
					combatDecider.learnFrom(td, 1000.0);
				}
		}
	}

	public double rewardFunction(Character c) {
		double startingHealth = getStartingHealth(c);
		//	double startingHealth = getStartingHealthOfSide(c);
		if (startingHealth < 0.0) return 0.0;
		startingHealth = Math.max(startingHealth, 1.0);
		double currentHealth = getHealth(c);
		//	double currentHealth = getCurrentHealthOfSide(c);

		double retValue = 0;
		if (currentHealth > 0) 
			retValue = 0.5 + currentHealth / (startingHealth * 2.0);
		if (retValue > 1.0) retValue = 1.0;

		return retValue;
	}


	public Integer getHealth(Character c) {
		int retValue = c.getHp() + 4 * c.getMp();
		if (c.isDead()) return 0;
		if (f.isInternal()) {
			if (c.getTempHp() >= c.getHp() || f.getCurrentRound() == 200) 
				return 0;
			retValue = (c.getHp() - c.getTempHp() + 4 * c.getMp());
		}

		List<Artefact> inventory = c.getInventory();
		int clw = 0;
		for (Artefact item : inventory) {
			if (item == Potion.CURE_LIGHT_WOUNDS) {
				retValue += 3;
				clw++;
			}
			if (clw >= c.getLevel() * 2)
				break; // only count Potions of Cure Light up to 2x Level
		}
		return retValue;
	}

	public double getStartingHealth(Character c) {
		CombatAgent ca = c.getCombatAgent();
		int index = allCombatants.lastIndexOf(ca);
		if (index == -1) return -1.0;
		return startingHealth[index];
	}

	/**
	 * Returns the total starting health for the side the character c is in
	 * 
	 * Returns 0.0 if the character is unknown
	 * 
	 * @param c
	 * @return
	 */
	public double getStartingHealthOfSide(Character c) {
		if (c==null) return 0.0;
		CombatAgent ca = c.getCombatAgent();
		if (side1.contains(ca)) {
			return starting1;
		} else {
			if (side2.contains(ca)) {
				return starting2;
			} else {
				return 0.0;
			}
		}
	}
	public double getCurrentHealthOfSide(Character c) {
		if (c==null) return 0.0;
		ArrayList<CombatAgent> side = null;
		if (side1.contains(c.getCombatAgent())) 
			side = side1;
		if (side2.contains(c.getCombatAgent()))
			side = side2;

		if (side == null) return 0.0;

		double retValue = 0;
		for (CombatAgent ca : side) {
			if (ca.getAgent() instanceof Character) 
				retValue += getHealth((Character)ca.getAgent());
		}
		return retValue;
	}

	public double[] getStartingHealth() {
		return new double[] {starting1, starting2};
	}

	@Override
	public boolean registerDecision(Character fighter, ExperienceRecord decision) {
		List<ExperienceRecord> fullList = teachingEpisodeHash.get(fighter);
		if (fullList == null || decision == null)
			return false;
		fullList.add(decision);
		teachingEpisodeHash.put(fighter, fullList);
		return true;
	}

	@Override
	public List<ExperienceRecord> getExperienceRecords(Character fighter) {
		return teachingEpisodeHash.get(fighter);
	}
}


