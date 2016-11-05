package hopshackle.simulation;

import hopshackle.simulation.ExperienceRecord.ERState;

import java.util.*;

public class ExperienceRecordCollector<A extends Agent> implements AgentListener {
	
	public interface ERCAllocationPolicy<A> {
		public void apply(A agent);
	}

	private HashMap<A, List<ExperienceRecord<A>>> erListMap = new HashMap<A, List<ExperienceRecord<A>>>();
	private List<AgentListener> listeners = new ArrayList<AgentListener>();
	private ERCAllocationPolicy<A> birthPolicy;
	private ExperienceRecordFactory<A> erFactory;
	protected EventFilter filter;
	
	public ExperienceRecordCollector(ExperienceRecordFactory<A> factory) {
		this(factory, new EventFilter() {
			@Override
			public boolean ignore(AgentEvent event) {
				return false;
			}
		});
	}
	public ExperienceRecordCollector(ExperienceRecordFactory<A> factory, EventFilter filter) {
		erFactory = factory;
		this.filter = filter;
	}

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
				if (er.getState() == ERState.NEXT_ACTION_TAKEN)
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
	
	public void clearAllExperienceRecord() {
		erListMap.clear();
	}
	
	public void removeER(A agent, ExperienceRecord<A> er) {
		if (agentAlreadySeen(agent)) {
			erListMap.get(agent).remove(er);
		}
	}
	
	public void removeAgent(A agent) {
		erListMap.remove(agent);
	}

	protected boolean agentAlreadySeen(A a) {
		if (erListMap.containsKey(a))
			return true;
		return false;
	}
	
	private void addAgentToList(A a) {
		List<ExperienceRecord<A>> tdList = new ArrayList<ExperienceRecord<A>>();
		erListMap.put(a, tdList);
	}
	
	private boolean processNewER(ExperienceRecord<A> newlyRegisteredER, A agent) {
		boolean passOnEvent = false;
		List<ExperienceRecord<A>> tdListForAgent = erListMap.getOrDefault(agent, new ArrayList<ExperienceRecord<A>>());
		for (ExperienceRecord<A> existingER : tdListForAgent) {
	//		System.out.println("\tProcessing: " + existingER.getActionTaken() + " (" + existingER.getActionTaken().getState() + ") for " + newlyRegisteredER.getActionTaken().getActor());
			if (existingER.getState() == ERState.ACTION_COMPLETED && 
					!newlyRegisteredER.getActionTaken().equals(existingER.getActionTaken())) {
				existingER.updateNextActions(newlyRegisteredER);
	//			System.out.println("Linking " + newlyRegisteredER.getActionTaken() + " to " + existingER.getActionTaken());
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
		if (filter.ignore(event)) 
			return;
//		System.out.println("Through Filter: " + event.getAction() + " ; " + event.getEvent());
		A a = (A) event.getAgent();
		Action<A> action = (Action<A>)event.getAction();
		Decider<A> eventDecider = (Decider<A>) event.getDecider();
		ExperienceRecord<A> newER = null;
		boolean passOnEvent = false;
//		if (!a.isDead()) System.out.println("Event received: " + action.actionType + "(" + action.getState() + ") : "+ event.getEvent() + " for " + a);
		switch (event.getEvent()) {
		case BIRTH:
			if (birthPolicy != null) {
				birthPolicy.apply(a);
			}
			break;
		case DEATH:
			processDeathOfAgent(a);
			passOnEvent = true;
			break;
		case DECISION_TAKEN:
			newER = erFactory.generate(a, eventDecider.getCurrentState(a), action, eventDecider.getChooseableOptions(a));
			passOnEvent = processNewER(newER, a);
			break;
		case ACTION_AGREED:
			passOnEvent = createAndProcessNewERIfNeeded(a, action, eventDecider);
			break;
		case ACTION_REJECTED:
		case ACTION_CANCELLED:
		case DECISION_STEP_COMPLETE:
			boolean passOn1 = createAndProcessNewERIfNeeded(a, action, eventDecider);
			passOnEvent = processDecisionForAgent(a, action);
			passOnEvent = passOnEvent || passOn1;
			break;
		}
		
		if (passOnEvent) {
//			System.out.println("Event passed on");
			passOnEventAfterExperienceRecordsUpdated(event);
		}
	} 
	
	private boolean actionPreviouslySeen(A agent, Action<A> action) {
		if (agentAlreadySeen(agent)) {
			List<ExperienceRecord<A>> tdArray = erListMap.get(agent);
			for (ExperienceRecord<A> er : tdArray) {
				Action<A> act = er.getActionTaken();
				if (act.equals(action))
					return true;
			}
		}
		return false;
	}
	
	private boolean createAndProcessNewERIfNeeded(A a, Action<A> action, Decider<A> agentDecider) {
		if (action!= null && !actionPreviouslySeen(a, action) && agentDecider != null) {
			// we need to create a new ER first
			List<ActionEnum<A>> chooseableOptions = new ArrayList<ActionEnum<A>>();
			chooseableOptions.add(action.getType());
			ExperienceRecord<A> newER = erFactory.generate(a, agentDecider.getCurrentState(a), action, chooseableOptions);
			return processNewER(newER, a);
		}
		return false;
	}
	
	protected boolean processDecisionForAgent(A agent, Action<A> action) {
		boolean passOnEvent = false;
//		agent.log("Processing Decision " + action);
		List<ExperienceRecord<A>> tdArray = erListMap.get(agent);
		ExperienceRecord<A> ERForActionReceived = null;
		for (ExperienceRecord<A> td : tdArray) {
			if (td.getActionTaken().equals(action)) {
				ERForActionReceived = td;
				switch (td.getState()) {
				case UNSEEN:
					assert false : "Should not be reachable";
				break;
				case DECISION_TAKEN:
					Decider<A> d = agent.getDecider();
					if (d != null) {
						State<A> newState = d.getCurrentState(agent);
						td.updateWithResults(0.0, newState);
					}
				case ACTION_COMPLETED:
				case NEXT_ACTION_TAKEN:
					// Do nothing
				}
			}
		}
		// We run through all ER again once we have updated the ER specific to the action received
		// This then updates any in an ACTION_COMPLETE state
		for (ExperienceRecord<A> td : tdArray) {
			if (!td.getActionTaken().equals(action)) {
				switch (td.getState()) {
				case ACTION_COMPLETED:
					td.updateNextActions(ERForActionReceived);
					passOnEvent = true;
				case UNSEEN:
				case DECISION_TAKEN:
				case NEXT_ACTION_TAKEN:
					// Do nothing
				}
			}
		}
		return passOnEvent;
	}

	private void processDeathOfAgent(A agent) {
		List<ExperienceRecord<A>> tdArray = erListMap.get(agent);
		for (ExperienceRecord<A> td : tdArray) {
			switch (td.getState()) {
			case DECISION_TAKEN:
				Decider<A> d = agent.getDecider();
				State<A> newState = d.getCurrentState(agent);
				td.updateWithResults(0.0, newState);
			case ACTION_COMPLETED:
				td.updateNextActions(null);
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
	public void setAllocationPolicy(ERCAllocationPolicy<A> newPolicy) {
		birthPolicy = newPolicy;
	}
	
}

