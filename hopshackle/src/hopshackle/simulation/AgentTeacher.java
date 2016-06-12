package hopshackle.simulation;

import hopshackle.simulation.test.refactor.*;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.util.*;

public class AgentTeacher<A extends Agent> implements Teacher<A>, AWTEventListener {

	private HashMap<A, List<ExperienceRecord<A>>> tdArrayHash = new HashMap<A, List<ExperienceRecord<A>>>();

	@Override
	public synchronized boolean registerDecision(A a, ExperienceRecord<A> td) {
		if (!agentAlreadySeen(a)) {
			addAgentToList(a);
			listenToAgent(a);
		}
		updateWithExperienceRecord(a, td);
		return true;
	}

	protected boolean agentAlreadySeen(Agent a) {
		if (tdArrayHash.containsKey(a))
			return true;
		return false;
	}

	private void addAgentToList(A a) {
		List<ExperienceRecord<A>> tdList = new ArrayList<ExperienceRecord<A>>();
		tdArrayHash.put(a, tdList);
	}

	private void updateWithExperienceRecord(Agent a, ExperienceRecord<A> td) {
		List<ExperienceRecord<A>> tdListForAgent = tdArrayHash.get(a);
		List<ExperienceRecord<A>> newTdListForAgent = new ArrayList<ExperienceRecord<A>>();
		for (ExperienceRecord<A> er : tdListForAgent) {
			er.apply(td);
			if (er.getState() != ExperienceRecord.State.NEXT_ACTION_TAKEN) {
				newTdListForAgent.add(er);
			}
		}
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

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void eventDispatched(AWTEvent event) {
		if (event instanceof AgentEvent) {
			AgentEvent ae = (AgentEvent)event;
			A a = (A) ae.getAgent();
			switch (((AgentEvent) event).getEvent()) {
			case DEATH:
				processDeathOfAgent(a);
				stopListeningToAgent(a);
				break;
			case DECISION_STEP_COMPLETE:
				Action<A> action = (Action<A>)ae.getAction();
				if (!actionPreviouslySeen(a, action)) {
					// we need to create a new ER first
					Decider<A> agentDecider = (Decider<A>) a.getDecider();
					List<ActionEnum<A>> chooseableOptions = new ArrayList<ActionEnum<A>>();
					chooseableOptions.add(action.getType());
					ExperienceRecord newER = new ExperienceRecord<A>(a, (List<GeneticVariable>) agentDecider.getVariables(), 
							 agentDecider.getCurrentState(a, a), action, chooseableOptions, agentDecider);
					registerDecision(a, newER);
				}
				processDecisionForAgent(a, action);
				break;
			}
		}
	} 
	
	private boolean actionPreviouslySeen(A agent, Action<A> action) {
		List<ExperienceRecord<A>> tdArray = tdArrayHash.get(agent);
		for (ExperienceRecord<A> er : tdArray) {
			Action<A> act = er.getActionTaken();
			if (act.equals(action))
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected void processDecisionForAgent(A agent, Action<A> action) {
		List<ExperienceRecord<A>> tdArray = tdArrayHash.get(agent);
		for (ExperienceRecord<A> td : tdArray) {
			if (td.getActionTaken().equals(action)) {
				switch (td.getState()) {
				case UNSEEN:
					assert false : "Should not be reachable";
					break;
				case DECISION_TAKEN:
					Decider<A> agentDecider = td.getDecider();
					double[] newState = agentDecider.getCurrentState(agent, agent);
					td.updateWithResults(0.0, newState);
					agentDecider.learnFrom(td, agent.getMaxScore());
				case ACTION_COMPLETED:
				case NEXT_ACTION_TAKEN:
					// Do nothing
				}
			} else {
				switch (td.getState()) {
				case ACTION_COMPLETED:
					List<ActionEnum<A>> chooseableOptions = new ArrayList<ActionEnum<A>>();
					chooseableOptions.add(action.getType());
					td.updateNextActions(chooseableOptions);
				case UNSEEN:
				case DECISION_TAKEN:
				case NEXT_ACTION_TAKEN:
					// Do nothing
				}
			}
		}
	}
	
	private void processDeathOfAgent(A agent) {
		List<ExperienceRecord<A>> tdArray = tdArrayHash.get(agent);
		for (ExperienceRecord<A> td : tdArray) {
				switch (td.getState()) {
				case DECISION_TAKEN:
					Decider<A> agentDecider = td.getDecider();
					double[] newState = agentDecider.getCurrentState(agent, agent);
					td.updateWithResults(0.0, newState);
					agentDecider.learnFrom(td, agent.getMaxScore());
				case ACTION_COMPLETED:
					td.setIsFinal();
				case NEXT_ACTION_TAKEN:
				case UNSEEN:
					// Do nothing
				}
		}
	}

	private void stopListeningToAgent(A a) {
		tdArrayHash.remove(a);
	}

}

