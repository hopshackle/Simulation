package hopshackle.simulation;

import hopshackle.simulation.ExperienceRecord.State;

import java.util.*;

public class ExperienceRecordCollector<A extends Agent> implements AgentListener {

	private HashMap<A, List<ExperienceRecord<A>>> erListMap = new HashMap<A, List<ExperienceRecord<A>>>();
	private List<AgentListener> listeners;

	public void registerAgent(A a) {
		if (!agentAlreadySeen(a)) {
			addAgentToList(a);
			listenToAgent(a);
		}
	}
	
	public List<ExperienceRecord<A>> getExperienceRecords(A a) {
		if (agentAlreadySeen(a)) {
			List<ExperienceRecord<A>> tdArray = erListMap.get(a);
			return HopshackleUtilities.cloneList(tdArray);
		}
		return new ArrayList<ExperienceRecord<A>>();
	}
	public List<ExperienceRecord<A>> getCompleteExperienceRecords(A a) {
		if (agentAlreadySeen(a)) {
			List<ExperienceRecord<A>> retValue = new ArrayList<ExperienceRecord<A>>();
			List<ExperienceRecord<A>> tdArray = erListMap.get(a);
			for (ExperienceRecord<A> er : tdArray) {
				if (er.getState() == State.NEXT_ACTION_TAKEN)
					retValue.add(er);
			}
			return retValue;
		}
		return new ArrayList<ExperienceRecord<A>>();
	}
	
	public List<A> getAllAgentsWithER() {
		return HopshackleUtilities.cloneList(erListMap.keySet());
	}
	
	public List<ExperienceRecord<A>> getAllExperienceRecords() {
		List<ExperienceRecord<A>> retList = new ArrayList<ExperienceRecord<A>>();
		for (List<ExperienceRecord<A>> l : erListMap.values())
			retList.addAll(l);
		return retList;
	}
	
	public void removeER(A agent, ExperienceRecord<A> er) {
		if (agentAlreadySeen(agent)) {
			erListMap.get(agent).remove(er);
		}
	}
	public void removeAllERForAgent(A agent) {
		erListMap.put(agent, new ArrayList<ExperienceRecord<A>>());
	}


	private boolean agentAlreadySeen(Agent a) {
		if (erListMap.containsKey(a))
			return true;
		return false;
	}
	
	private void addAgentToList(A a) {
		List<ExperienceRecord<A>> tdList = new ArrayList<ExperienceRecord<A>>();
		erListMap.put(a, tdList);
	}

	private boolean processNewER(ExperienceRecord<A> er, A agent) {
		return updateWithExperienceRecord(er, agent);
	}
	
	private boolean updateWithExperienceRecord(ExperienceRecord<A> newlyRegisteredER, A agent) {
		boolean passOnEvent = false;
		List<ExperienceRecord<A>> tdListForAgent = erListMap.getOrDefault(agent, new ArrayList<ExperienceRecord<A>>());
		for (ExperienceRecord<A> existingER : tdListForAgent) {
			if (existingER.getState() == State.ACTION_COMPLETED && !newlyRegisteredER.getActionTaken().equals(existingER.getActionTaken()) && 
					newlyRegisteredER.getActionTaken().getActor().equals(existingER.getActionTaken().getActor())) {
				// A different action, but with the same deciding agent
				existingER.updateNextActions(newlyRegisteredER.possibleActionsFromStartState, newlyRegisteredER.getStartScore());
				passOnEvent = true;
			}
		}
		tdListForAgent.add(newlyRegisteredER);
		return passOnEvent;
	}
	
	private void listenToAgent(A a) {
		a.addListener(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void processEvent(AgentEvent event) {
		A a = (A) event.getAgent();
		Action<A> action = (Action<A>)event.getAction();
		Decider<A> agentDecider = (Decider<A>) event.getDecider();
		ExperienceRecord<A> newER = null;
		boolean passOnEvent = false;
		switch (event.getEvent()) {
		case DEATH:
			processDeathOfAgent(a);
			passOnEvent = true;
			break;
		case DECISION_TAKEN:
			newER = new ExperienceRecord<A>(a.getScore(), (List<GeneticVariable>) agentDecider.getVariables(), 
					agentDecider.getCurrentState(a, a), action, agentDecider.getChooseableOptions(a, a), agentDecider);
			passOnEvent = processNewER(newER, a);
			break;
		case ACTION_AGREED:
			if (action!= null && !actionPreviouslySeen(a, action) && agentDecider != null) {
				// we need to create a new ER first
				List<ActionEnum<A>> chooseableOptions = new ArrayList<ActionEnum<A>>();
				chooseableOptions.add(action.getType());
				newER = new ExperienceRecord<A>(a.getScore(), (List<GeneticVariable>) agentDecider.getVariables(), 
						agentDecider.getCurrentState(a, a), action, chooseableOptions, agentDecider);
				passOnEvent = processNewER(newER, a);
			}
			break;
		case ACTION_REJECTED:
		case ACTION_CANCELLED:
		case DECISION_STEP_COMPLETE:
			passOnEvent = processDecisionForAgent(a, action);
			break;
		}
		
		if (passOnEvent) passOnEventAfterExperienceRecordsUpdated(event);
	} 
	
	private boolean actionPreviouslySeen(A agent, Action<A> action) {
		List<ExperienceRecord<A>> tdArray = erListMap.get(agent);
		for (ExperienceRecord<A> er : tdArray) {
			Action<A> act = er.getActionTaken();
			if (act.equals(action))
				return true;
		}
		return false;
	}
	
	protected boolean processDecisionForAgent(A agent, Action<A> action) {
		List<ExperienceRecord<A>> tdArray = erListMap.get(agent);
		for (ExperienceRecord<A> td : tdArray) {
			if (td.getActionTaken().equals(action)) {
				switch (td.getState()) {
				case UNSEEN:
					assert false : "Should not be reachable";
					break;
				case DECISION_TAKEN:
					double[] newState = BaseDecider.getState(agent, agent, td.variables);
					td.updateWithResults(0.0, newState);
				case ACTION_COMPLETED:
				case NEXT_ACTION_TAKEN:
					// Do nothing
				}
			} else {
				switch (td.getState()) {
				case ACTION_COMPLETED:
					List<ActionEnum<A>> chooseableOptions = new ArrayList<ActionEnum<A>>();
					chooseableOptions.add(action.getType());
					td.updateNextActions(chooseableOptions, agent.getScore());
					return true;
				case UNSEEN:
				case DECISION_TAKEN:
				case NEXT_ACTION_TAKEN:
					// Do nothing
				}
			}
		}
		return false;
	}

	private void processDeathOfAgent(A agent) {
		List<ExperienceRecord<A>> tdArray = erListMap.get(agent);
		for (ExperienceRecord<A> td : tdArray) {
			switch (td.getState()) {
			case DECISION_TAKEN:
				double[] newState = BaseDecider.getState(agent, agent, td.variables);
				td.updateWithResults(0.0, newState);
			case ACTION_COMPLETED:
				td.updateNextActions(new ArrayList<ActionEnum<A>>(), agent.getScore());
				td.setIsFinal();
			case NEXT_ACTION_TAKEN:
			case UNSEEN:
				// Do nothing
			}
		}
	}
	
	public void addListener(AgentListener el) {
		if (!listeners.contains(el))
			listeners.add(el);
	}
	public void removeListener(AgentListener el) {
		listeners.remove(el);
	}
	private void passOnEventAfterExperienceRecordsUpdated(AgentEvent ae) {
		for (AgentListener el : listeners) {
			el.processEvent(ae);
		}
	}
	
}

