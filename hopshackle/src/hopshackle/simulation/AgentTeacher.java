package hopshackle.simulation;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.util.*;

public class AgentTeacher<A extends Agent> implements Teacher<A>, AWTEventListener {

	private HashMap<A, List<ExperienceRecord<A>>> tdArrayHash = new HashMap<A, List<ExperienceRecord<A>>>();
	private HashMap<A, Double> lastScoresOfAgents = new HashMap<A, Double>();

	@Override
	public synchronized boolean registerDecision(A a, ExperienceRecord<A> td) {
		if (!agentAlreadySeen(a)) {
			addAgentToList(a);
			listenToAgent(a);
		}
		addDecisionToHash(a, td);
		return true;
	}

	private boolean agentAlreadySeen(Agent a) {
		if (tdArrayHash.containsKey(a))
			return true;
		return false;
	}

	private void addAgentToList(A a) {
		List<ExperienceRecord<A>> tdList = new ArrayList<ExperienceRecord<A>>();
		tdArrayHash.put(a, tdList);
		lastScoresOfAgents.put(a, a.getScore());
	}

	private void addDecisionToHash(Agent a, ExperienceRecord<A> td) {
		List<ExperienceRecord<A>> tdListForAgent = tdArrayHash.get(a);
		tdListForAgent.add(td);
	}

	private void listenToAgent(A a) {
		a.addListener(this);
	}

	@Override
	public List<ExperienceRecord<A>> getExperienceRecords(A a) {
		if (agentAlreadySeen(a)) {
			List<ExperienceRecord<A>> tdArray = tdArrayHash.get(a);
			return HopshackleUtilities.cloneList(tdArray);
		}
		return new ArrayList<ExperienceRecord<A>>();
	}

	@Override
	public synchronized void eventDispatched(AWTEvent event) {
		if (event instanceof AgentEvent) {
			A a = (A) ((AgentEvent)event).getAgent();
			switch (((AgentEvent) event).getEvent()) {
			case DEATH:
				processDecisionsForAgent(a);
				stopListeningToAgent(a);
				break;
			case DECISION_STEP_COMPLETE:
				processDecisionsForAgent(a);
				break;
			}
		}
	} 

	protected void processDecisionsForAgent(A a) {
		List<ExperienceRecord<A>> tdArray = tdArrayHash.get(a);
		double reward = a.getScore() - lastScoresOfAgents.get(a);
		for (ExperienceRecord<A> td : tdArray) {
			Decider<A> agentDecider = td.getDecider();
			double[] newState = agentDecider.getCurrentState(a, a);
			List<ActionEnum<A>> possibleActions = agentDecider.getChooseableOptions(a, a);
			td.updateWithResults(reward, newState, possibleActions, a.isDead());
			agentDecider.learnFrom(td, a.getMaxScore());
		}
		lastScoresOfAgents.put(a, a.getScore());
		tdArrayHash.put(a, new ArrayList<ExperienceRecord<A>>());
	}

	private void stopListeningToAgent(A a) {
		tdArrayHash.remove(a);
		lastScoresOfAgents.remove(a);
	}
}

