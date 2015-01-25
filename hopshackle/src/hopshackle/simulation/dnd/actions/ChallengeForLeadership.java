package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public class ChallengeForLeadership extends Action {
	private boolean leaveParty = false;
	
	public ChallengeForLeadership(Agent a) {
		super(a, 999, true);
	}

	@Override
	protected void doStuff() {
		if (!(actor instanceof Character)) return;
		Character c = (Character) actor;
		Agent victor = null;
		if (!c.isDead() && c.getParty() != null && !c.getParty().isDead()) {
			// Check still a member of a valid party
			Party p = c.getParty();
			Character currentLeader = p.getLeader();
			if (currentLeader == null || currentLeader.isDead()) {
				victor = c;
			} else {
				if (currentLeader == c) return;
				// If so, then stage a fight with the current leader
				// This should be non-fatal (i.e. subdual damage)
				// This is done autimatically in Attack
				Reputation.CHALLENGE.apply(c, currentLeader.getReputation()+100);
				Fight f = new Fight(c, currentLeader);
				f.resolve();
				victor = currentLeader;
				if (c.isDead())
					Reputation.KILL_PARTY_MEMBER.apply(currentLeader);
				if (currentLeader.isDead())
					Reputation.KILL_PARTY_MEMBER.apply(c);
				
				if (currentLeader.isStunned() || currentLeader.isDead())
					victor = c;
			}
			c.addHp(100, true);
			currentLeader.addHp(100, true);
			// then restore any temporary hit points

			// If the challenger loses. He is forced to leave the party.

			if (victor.equals(c)) {
				p.setLeader(c);
				c.log("Wins duel to take party leadership");
				currentLeader.log("Loses duel - no longer party leader");
				c.surviveEncounter((int)currentLeader.getLevel()-1);
			} else {
				// not successful
				c.log("Loses duel - and evicted from Party");
				currentLeader.log("Wins challenge duel - still party leader");
				currentLeader.surviveEncounter((int)c.getLevel()-3);
				p.banMember(c);
				leaveParty = true;
			}
			return;
		}
	}
	protected void doNextDecision() {
		// if lost duel, then next action is to leave party; in this case do not call parent
		if (leaveParty) {
			(new LeaveParty((Character)actor, false)).run();
		} else
			super.doNextDecision();
	}
	public String toString() {return "CHALLENGE";}

}
