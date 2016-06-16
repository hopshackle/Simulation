package hopshackle.simulation;

import hopshackle.simulation.ExperienceRecord.State;

import java.util.*;

public class AgentTeacher<A extends Agent> implements Teacher<A>, AgentListener {

	private HashMap<A, List<ExperienceRecord<A>>> tdArrayHash = new HashMap<A, List<ExperienceRecord<A>>>();

	public void registerAgent(A a) {
		if (!agentAlreadySeen(a)) {
			addAgentToList(a);
			listenToAgent(a);
		}
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

	private void processNewER(ExperienceRecord<A> er) {
		updateWithExperienceRecord(er);
		removeCompletedER(er.actor);
	}
	
	private void updateWithExperienceRecord(ExperienceRecord<A> newlyRegisteredER) {
		A a = newlyRegisteredER.actor;
		List<ExperienceRecord<A>> tdListForAgent = tdArrayHash.get(a);
		for (ExperienceRecord<A> existingER : tdListForAgent) {
			if (existingER.getState() == State.ACTION_COMPLETED && !newlyRegisteredER.getActionTaken().equals(existingER.getActionTaken()) && 
					newlyRegisteredER.getActionTaken().getActor().equals(existingER.getActionTaken().getActor())) {
				// A different action, but with the same deciding agent
				existingER.updateNextActions(newlyRegisteredER.possibleActionsFromStartState);
				Decider<A> agentDecider = existingER.getDecider();
				agentDecider.learnFrom(existingER, a.getMaxScore());
			}
		}
		tdListForAgent.add(newlyRegisteredER);
	}
	
	private void removeCompletedER(A agent) {
		List<ExperienceRecord<A>> ERForAgent = tdArrayHash.get(agent);
		List<ExperienceRecord<A>> newERForAgent = new ArrayList<ExperienceRecord<A>>();
		for (ExperienceRecord<A> er : ERForAgent) {
			if (er.getState() != ExperienceRecord.State.NEXT_ACTION_TAKEN)
				newERForAgent.add(er);
		}
		tdArrayHash.put(agent, newERForAgent);
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
	public synchronized void processEvent(AgentEvent event) {
		A a = (A) event.getAgent();
		Action<A> action = (Action<A>)event.getAction();
		Decider<A> agentDecider = (Decider<A>) event.getDecider();
		ExperienceRecord<A> newER = null;
		switch (event.getEvent()) {
		case DEATH:
			processDeathOfAgent(a);
			stopListeningToAgent(a);
			break;
		case DECISION_TAKEN:
			newER = new ExperienceRecord<A>(a, (List<GeneticVariable>) agentDecider.getVariables(), 
					agentDecider.getCurrentState(a, a), action, agentDecider.getChooseableOptions(a, a), agentDecider);
			processNewER(newER);
			break;
		case ACTION_AGREED:
			if (action!= null && !actionPreviouslySeen(a, action) && agentDecider != null) {
				// we need to create a new ER first
				List<ActionEnum<A>> chooseableOptions = new ArrayList<ActionEnum<A>>();
				chooseableOptions.add(action.getType());
				newER = new ExperienceRecord<A>(a, (List<GeneticVariable>) agentDecider.getVariables(), 
						agentDecider.getCurrentState(a, a), action, chooseableOptions, agentDecider);
				processNewER(newER);
			}
			break;
		case ACTION_REJECTED:
		case ACTION_CANCELLED:
			// Cancelling, or Deciding not to proceed with an action is equivalent to completing the action
			// albeit with a different end state
		case DECISION_STEP_COMPLETE:
			processDecisionForAgent(a, action);
			removeCompletedER(a);
			break;
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
				case ACTION_COMPLETED:
				case NEXT_ACTION_TAKEN:
					// Do nothing
				}
			} else {
				switch (td.getState()) {
				case ACTION_COMPLETED:
					Decider<A> agentDecider = td.getDecider();
					List<ActionEnum<A>> chooseableOptions = new ArrayList<ActionEnum<A>>();
					chooseableOptions.add(action.getType());
					td.updateNextActions(chooseableOptions);
					agentDecider.learnFrom(td, agent.getMaxScore());
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
				case ACTION_COMPLETED:
					agentDecider = td.getDecider();
					td.updateNextActions(new ArrayList<ActionEnum<A>>());
					td.setIsFinal();
					agentDecider.learnFrom(td, agent.getMaxScore());
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

