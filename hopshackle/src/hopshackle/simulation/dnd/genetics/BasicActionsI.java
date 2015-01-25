package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;

import java.util.List;

public enum BasicActionsI implements ActionEnum {
	ADVENTURE,
	REST,
	MOVE_RANDOM,
	MOVE_LOW_CR,
	MOVE_HIGH_CR,
	JOIN_PARTY,
	STAY,
	LEAVE,
	CHALLENGE;

	private static String chromosomeName = "BD1";

	public String getChromosomeDesc() {return chromosomeName;}

	public boolean isChooseable(Agent a) {
		boolean retValue = false;
		if (!(a instanceof DnDAgent)) return false;
		DnDAgent d = (DnDAgent)a;
		Square s = (Square)d.getLocation();
		int CR = 0;
		if (s != null) {
			CR = s.getX() + s.getY();
		}

		boolean isParty = false;
		boolean inParty = false;
		boolean isLeader = false;
		if (d instanceof Character) {
			Character c = (Character) d;
			if (c.getParty() != null) inParty = true;
			if (inParty && c.getParty().getLeader().equals(c))
				isLeader = true;
		} else if (d instanceof Party) 
			isParty = true;

		switch(this) {
		case ADVENTURE:
		case MOVE_LOW_CR:
			if (inParty) return false;
			if (CR > 0)
				retValue = true;
			break;
		case REST:
		case MOVE_RANDOM:
			if (inParty) return false;
			retValue = true;
			break;
		case MOVE_HIGH_CR:
			if (inParty) return false;
			if (CR < 10)
				retValue = true;
			break;
		case JOIN_PARTY:
			if (inParty) return false;
			if (d instanceof Character) {
				Character c = (Character) d;
				if (c.getParty() == null)
					retValue = true;
			}
			break;
		case STAY:
		case LEAVE:
			if (!inParty || isParty) return false;
			retValue = true;
			break;
		case CHALLENGE:
			if (!inParty || isParty || isLeader) return false;
			retValue = true;
			break;
		}

		return retValue;
	}
	public Action getAction (Agent a, Agent a2) {
		return getAction(a);
	}
	public Action getAction(Agent a) {
		DnDAgent c = (DnDAgent) a;
		Action retAction = null;
		switch(this) {
		case ADVENTURE:
			retAction = new Adventure(c, true);
			break;
		case REST:
			retAction = new Rest(c, true);
			break;
		case MOVE_RANDOM:
			retAction = new Move(c, getMoveLocation(c));
			break;
		case MOVE_HIGH_CR:
			retAction = new Move(c, getMoveLocation(c));
			break;
		case MOVE_LOW_CR:
			retAction = new Move(c, getMoveLocation(c));
			break;
		case JOIN_PARTY:
			retAction = new JoinParty(c);
			break;
		case STAY:
			retAction = new DoNothing(c, 999);
			break;
		case LEAVE:
			retAction = new LeaveParty(c, true);
			break;
		case CHALLENGE:
			retAction = new ChallengeForLeadership(c);
			break;
		}
		return retAction;
	}

	private Location getMoveLocation(Agent ch) {
		Location currentLoc = ch.getLocation();
		List<Location> arrLoc = currentLoc.getAccessibleLocations();
		int n = arrLoc.size();
		if (n == 0) return null;
		int choice =0;
		switch (this) {
		case MOVE_HIGH_CR:
			int highestCR = 0;
			for (int i = 0; i<n; i++) {
				Location l = arrLoc.get(i);
				if (l instanceof Square) {
					Square s = (Square) l;
					int CR = s.getX() + s.getY();
					if (CR > highestCR) {
						highestCR = CR;
						choice = i;
					}
				}
			}
			break;
		case MOVE_LOW_CR:
			int lowestCR = 100;
			for (int i = 0; i<n; i++) {
				Location l = arrLoc.get(i);
				if (l instanceof Square) {
					Square s = (Square) l;
					int CR = s.getX() + s.getY();
					if (CR < lowestCR) {
						lowestCR = CR;
						choice = i;
					}
				}
			}
			break;
		case MOVE_RANDOM:
			choice = (int) (Math.random()*n);
			break;
		}
		return arrLoc.get(choice);
	}
	
	@Override
	public Enum<BasicActionsI> getEnum() {
		return this;
	}
}
