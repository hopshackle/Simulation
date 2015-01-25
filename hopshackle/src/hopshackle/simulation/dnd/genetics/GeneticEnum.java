package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public enum GeneticEnum implements GeneticVariable{

	LEVEL,
	WOUND,
	WOUND_PARTY,
	CR_DIFF_PARTY,
	AGE,
	NUMBER_US,
	MAGIC_PARTY,
	IS_LEADER,
	AC,
	DMG;

	private static String descriptor = "GEN1";

	public String getDescriptor() {return descriptor;}

	public double getValue(Object a1, Object a2) {
		return getValue(a1, 0.00);
	}

	public double getValue(Object a, double var) {
		if (!(a instanceof DnDAgent)) return 0.00;
		boolean debugLevel;
		
		Square s;
		double CR, retValue;
		Character c = null;
		Party p;
		if (a instanceof Character) {
			c = (Character) a;
		} else if (a instanceof Party) {
			p = (Party)a ;
			c = p.getLeader();
		}
		if (c == null) return -1.0;
		switch (this) {
		case LEVEL:
			return (c.getLevel()-3.0) / 3.0;
		case WOUND:
			retValue = c.getWound();
			return 2.0 * retValue - 1.0;
		case WOUND_PARTY:
			if (c.getParty()!=null) {
				retValue = c.getParty().getWound();
			} else {
				retValue = c.getWound();
			}
			return 2.0 * retValue - 1.0;
	/*	case CR_DIFF:
			s = (Square)c.getLocation();
			if (s == null) return 0.0;
			CR = s.getX() + s.getY();
			retValue = (CR - c.getLevel()) / 5.0;
			return retValue;
			*/
		case CR_DIFF_PARTY:
			s = (Square)c.getLocation();
			if (s == null) return 0.0;
			double levelP = c.getLevel();
			if (c.getParty() != null)
				levelP = c.getParty().getLevel();
			CR = s.getX() + s.getY();
			retValue = (CR - levelP) / 5.0;
			return retValue;
		case AGE:
			retValue = ((double)(c.getMaxAge()-c.getAge()))/((double)c.getMaxAge());
			return 2.0 * retValue - 1.0;
		case NUMBER_US:
			retValue = 1.0;
			if (c.getParty() != null) {
				retValue = c.getParty().getSize();
			}
			retValue = 1.0 - (1.0 / retValue);
			return 2.0 * retValue - 1.0;
	/*	case ENTROPY: 
			retValue = DnDAgentInteractionGeneticSet.entropy((DnDAgent) a);
			return 2.0 * retValue - 1.0;
		case MAGIC:
			retValue = 0.0;
			switch (c.getChrClass()) {
			case CLERIC:
				if (c.getMaxMp() == 0) {
					retValue = 0.0;
				} else {
					retValue = (double)c.getMp() / (double)c.getMaxMp();
				}
				break;
			}
			return 2.0 * retValue - 1.0;
			
		*/
		case MAGIC_PARTY:
			p = c.getParty();
			if (p == null || p.getMagic() < 0.0) {
				retValue = 0.0;
			} else {
				retValue = p.getMagic();
			}
			return 2.0 * retValue - 1.0;
		case IS_LEADER:
			// -1.0 for not in party, or member; 0.0 for leader of party; +1.0 for making decision for Party
			retValue = -1.0;
			if (a instanceof Party) {
				retValue = 1.0;
			} else if (c.getParty() != null && c == c.getParty().getLeader()) {
				retValue = 0.0;
			}
			return retValue;
		case AC:
			debugLevel = c.getDebugLocal();
			c.setDebugLocal(false);
			
			CombatModifier cm = new CombatModifier(CombatModifier.dummyTarget.getCombatAgent(), c.getCombatAgent(), null);
			double ac = 10.0 + cm.getTotalDefenseBonus();
			
			c.setDebugLocal(debugLevel);
			return ((ac - 7.0) / 7.5) - 1.0;
			// to give -1 to +1 range between AC 7-22
		case DMG:
			debugLevel = c.getDebugLocal();
			c.setDebugLocal(false);

			double dmg = c.getCombatAgent().getAvgDmgPerRound(CombatModifier.dummyTarget.getCombatAgent(), null);
			
			c.setDebugLocal(debugLevel);
			return (dmg - 4.0) / 4.0;
			// to give -1 to +1 range between 0 and 8 dmg per round
		}

		return 0.0;
	}

	public boolean unitaryRange() {
		return true;
	}

}
