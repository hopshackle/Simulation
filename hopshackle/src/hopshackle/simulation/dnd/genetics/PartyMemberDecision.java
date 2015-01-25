package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.*;

public class PartyMemberDecision {
	
	private static BaseDecider partyApplicationDecider;
	private static ArrayList<GeneticVariable> variables = new ArrayList<GeneticVariable>(EnumSet.allOf(DnDAgentInteractionGeneticSet.class));
	
	static {
		partyApplicationDecider = new EvolutionDecider(new ArrayList<ActionEnum>(EnumSet.allOf(PartyJoinActionsII.class)),
			 new ArrayList<GeneticVariable>(EnumSet.allOf(DnDAgentInteractionGeneticSet.class)));
		partyApplicationDecider.setName("PartyApplication");
	}
	
	public static double getChanceToApply(DnDAgent group, DnDAgent suitor){
		// routine to see if suitor is interested on group
		if (!(suitor instanceof Character)) return -100.0;
		Agent c = (Agent) suitor;
		if (c.isDead()) return -100.0;
		Genome g = c.getGenome();

		return g.getValue(PartyJoinActions.APPLY_TO_PARTY, suitor, group, variables);
	}

	public static boolean processApplication(DnDAgent group, DnDAgent suitor) {
		if (!(suitor instanceof Character)) return false;
		Agent c = (Agent) suitor;
		if (c.isDead() || group.isDead()) return false;


		ActionEnum result = partyApplicationDecider.decide(group, suitor);
		
		if (result != null && result.equals(PartyJoinActionsII.ACCEPT_APPLICANT)) {
			return true;
		}
		return false;
	}

}
