package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;

import java.util.List;

public enum CombatActionsI implements ActionEnum {
	ATTACK,
	RETREAT,
	DISENGAGE,
	DEFEND,
	HEAL,
	HEAL_ITEM,
	BUFF,
	SPELL_ATTACK;
	
	private static String chromosomeName = "CBT1";

	public String getChromosomeDesc() {return chromosomeName;}

	public boolean isChooseable(Agent a) {
		boolean retValue = false;
		if (!(a instanceof Character)) 
			return false;

		Character c = (Character)a;

		switch(this) {
		case ATTACK:
		case RETREAT:
		case DISENGAGE:
		case DEFEND:
			retValue = true;
			break;
		case HEAL_ITEM:
			if (c.getWound() > 0.01) {
				List<Artefact> inventory = c.getInventory();
				if (inventory.contains(Potion.CURE_LIGHT_WOUNDS))
					retValue = true;
			}
			break;
		case BUFF:
			if (c.getMp() > 0)
				retValue = true;
			break;
			// There are a number of other checks required
			// to ensure that a useful spell can be cast, but these are
			// in the Buff Action for the sake of simplicity
		case HEAL:
			DnDAgent group = c;
			if (c.getParty() != null) group = c.getParty();
			if (group.getWound() > 0.01) 
				retValue = true;
			if (c.getMp() < 1)
				retValue = false;
			break;
		case SPELL_ATTACK:
			if (c.getLevel()>3 && c.getMp() > 3) 
				retValue = true;
			break;
		}

		return retValue;
	}
	
	public Action getAction(Agent a) {
		Character c = (Character)a;
		CombatAgent ca = c.getCombatAgent();
		boolean recordAction = false;
		if (c.getRace() == Race.HUMAN) recordAction = true;
		return getAction(ca, recordAction);
	}
	
	public Action getAction(CombatAgent ca, boolean recordAction) {
		Action retAction = null;

		switch(this) {
		case ATTACK:
			retAction = new Attack(ca, ca.getCurrentTarget(), false, recordAction);
			break;
		case RETREAT:
			retAction = new Retreat(ca);
			break;
		case DISENGAGE:
			retAction = new Disengage(ca, recordAction);
			break;
		case DEFEND:
			retAction = new Defend(ca, recordAction);
			break;
		case HEAL_ITEM:
			retAction = new HealPotion(ca, recordAction);
			break;
		case BUFF:
			retAction = new Buff(ca);
			break;
		case HEAL:
			retAction = new Heal(ca, recordAction);
			break;
		case SPELL_ATTACK:
			retAction = new SpellAttack(ca);
		}
		return retAction;
	}

	public Action getAction(Agent a1, Agent a2) {
		return getAction(a1);
		// Not implemented
	}

	@Override
	public Enum<CombatActionsI> getEnum() {
		return this;
	}
}
