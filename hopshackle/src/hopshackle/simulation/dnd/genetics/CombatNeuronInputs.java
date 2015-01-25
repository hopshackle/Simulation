package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.ArrayList;

public enum CombatNeuronInputs implements GeneticVariable {

	WOUND_US,
	WOUND_PARTY,
	CR_DIFF,
	TARGETTED,
	ODDS,
	CLR_PERCENTAGE,
	MAGIC_LEFT,
	CANNOT_ATTACK,
	NUMBER_US,
	AC,
	DMG,
	INTERNAL_FIGHT;

	public String getDescriptor() {
		return "CBT1";
	}

	public double getValue(DnDAgent a) {
		Character c = (Character)a;
		CombatAgent opp = c.getCurrentTarget();
		if (opp == null) 
			opp = c.getCombatAgent();
		return getValue(a, (DnDAgent)opp.getAgent());
	}

	@Override
	public double getValue(Object o1, Object o2) {
		if (!(o1 instanceof DnDAgent)) return 0.00;
		if (!(o2 instanceof DnDAgent)) return 0.00;
		DnDAgent a1 = (DnDAgent) o1;
		DnDAgent a2 = (DnDAgent) o2;
		Party p1 = null;
		Character c1 = (Character) a1; 
		CombatAgent ca1 = c1.getCombatAgent();
		p1 = c1.getParty();
		Character c2 = (Character) a2;
		CombatAgent ca2 = null;
		if (c2 !=null)
			ca2 = c2.getCombatAgent();
		switch (this) {
		case WOUND_US:
			return c1.getWound();
		case WOUND_PARTY:
			if (p1 == null)
				return c1.getWound();
			return p1.getWound();
		case CR_DIFF:
			Square s = (Square)c1.getLocation();
			if (s == null) return 0.0;
			double CR = s.getX() + s.getY();
			double retValue =  CR - c1.getLevel();
			if (p1 != null) retValue =  CR - p1.getLevel();
			return retValue / 5.0;
		case TARGETTED:
			if (ca1.isTargetted()) return 1.0;
			return 0.0;
		case ODDS:
			double odds = 0.0;
			if (ca1.getCurrentFight() == null) return odds;
			int numberUs = ca1.getCurrentFight().getSideSize(ca1);
			int numberThem;
			if (ca2 == null) {
				numberThem = ca1.getCurrentFight().getOtherSideSize(ca1);
			} else 
				numberThem = ca2.getCurrentFight().getSideSize(ca2);

			if (numberUs > numberThem) {
				// positive number returned
				if (numberThem == 0) {return 1.0;}
				odds = ((double) numberUs) / ((double) numberThem) - 1.0;
			} else {
				// else return negative number
				if (numberUs == 0) {return -1.0;}
				odds = - ((double) numberThem) / ((double) numberUs) + 1.0;
			}
			if (odds > 10.0) odds = 10.0;
			if (odds < -10.0) odds = -10.0;
			return odds / 10.0;
		case MAGIC_LEFT:
			switch (c1.getChrClass()) {
			case CLERIC:
				if (c1.getMaxMp() == 0) return 0.0;
				return (double)c1.getMp() / (double)c1.getMaxMp();
			case FIGHTER:
				if (p1 == null || p1.getMagic() < 0.0) return 0.0;
				return p1.getMagic();
			default:
				return 0.0;
			}
		case CLR_PERCENTAGE:
			ArrayList<Character> members = null;
			if (p1==null) {
				members = new ArrayList<Character>();
				members.add(c1);
			} else {
				members = p1.getMembers();
			}
			double alive = 0.0;
			double clerics = 0.0;
			for (Character m : members) {
				if (!m.isDead()) alive++;
				if (m.getChrClass()==CharacterClass.CLERIC) clerics++;
			}
			return (clerics/alive);
			/*		case MAGIC_ATTACK:
			if (c1.getCombatAgent().getCurrentCM().isMagicAttack())
				return 1.0;
			return 0.0;

			 */
		case CANNOT_ATTACK:
			boolean magicAttack = c1.getCombatAgent().getCurrentCM().isMagicAttack();
			boolean hasDR = false;
			CombatAgent enemy = c1.getCombatAgent().getCurrentTarget();
			if (enemy != null) {
				ArrayList<CombatCondition> ccList = enemy.getCombatConditions();
				for (CombatCondition cc : ccList)
					if (cc instanceof DamageReduction) hasDR = true;
			}
			if (hasDR && !magicAttack) return 1.0;
			return 0.0; //default

		case NUMBER_US:
			return GeneticEnum.NUMBER_US.getValue(a1, a2);

		case AC:
			return GeneticEnum.AC.getValue(a1, a2);

		case DMG:
			return GeneticEnum.DMG.getValue(a1, a2);

		case INTERNAL_FIGHT:
			Fight currentFight = ca1.getCurrentFight();
			if (currentFight != null)
				return currentFight.isInternal() ? 1.0 : 0.0;
			return 0.0;
		}

		return 0.0;
	}

	public double getValue(Object a, double var) {
		return 0;
	}

	public boolean unitaryRange() {
		return true;
	}
}
