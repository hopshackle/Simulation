/**
 * 
 */
package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.PartyMemberDecision;

import java.util.List;

public class JoinParty extends Action {

	private boolean joinsParty = false;

	public JoinParty(DnDAgent c) {
		super(c, 500, true);
		c.setStationary(true);
	}

	protected void doStuff(){
		Character c = (Character) actor;
		if (c.getParty() == null) {
			List<Agent> allAgents = c.getLocation().getAgents();
			boolean blnResult = false;
			double score = 0.0;
			double bestScore = 0.0;
			DnDAgent preferredParty = null;
			for (Agent a : allAgents) {
				DnDAgent d = (DnDAgent) a;
				if (d.isStationary() &&  // exclude Characters who are in a Party
						!d.equals(c) &&
						!d.isDead() &&
						(!((d instanceof Character) && 
								((Character)(d)).getParty() != null))) {
					score = 0.0;
					if (d instanceof Party && ((Party)d).getLeader().getRace() != Race.HUMAN) score = -100;
					if (d instanceof Party && ((Party)d).isBanned(c)) score = -100;
					if (score > -100)
						score = PartyMemberDecision.getChanceToApply(d, (DnDAgent)actor);

					if (score>bestScore) {
						bestScore = score;
						preferredParty = d;
					}
				}
			}

			if (preferredParty != null) {
				blnResult = PartyMemberDecision.processApplication(preferredParty, (DnDAgent)actor);
			}

			if (blnResult) {
				joinsParty = true;
				Party p = null;
				if (preferredParty instanceof Character) {
					p = new Party((Character) preferredParty);
					preferredParty.log("Forms new Party " + p.toString());
					preferredParty.addAction(preferredParty.decide());
					// The party founders actions will be purged when the Party is created
					// so then ened to add a new one
				} else {
					p = (Party) preferredParty;
				}
				if (c.getParty() != null) {
					c.getParty().removeMember(c);
				}
				p.addMember(c);
			} else {
				c.log("Fails to find a party to join");
			}
		}
		c.setStationary(false);
	}

	protected void doNextDecision() {
		if (!joinsParty && Dice.roll(1,6) > 4) {
			Character c = (Character) actor;
			c.log("Encounters wandering monster.");
			c.addAction(new Adventure(c, 0,-2, false));
		} else {
			super.doNextDecision();
		}
	}
	public String toString() {return "JOIN_PARTY";}

}