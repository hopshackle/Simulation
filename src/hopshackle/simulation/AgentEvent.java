package hopshackle.simulation;

import java.util.List;

public class AgentEvent {
	
	public enum Type {
		BIRTH,
		DEATH,
		DECISION_TAKEN,
		ACTION_AGREED,
		ACTION_REJECTED,
		ACTION_EXECUTED,
		ACTION_CANCELLED,
		DECISION_STEP_COMPLETE;
	}

	private Agent agent;
	private Type event;
	private Action action;
	private List<ActionEnum> chooseableOptions;
	private Decider decider;
	
	public AgentEvent(Agent agent, Type eventType) {
		this.agent = agent;
		event = eventType;
		this.decider = agent.getDecider();
	}
	public AgentEvent(Agent agent, Type eventType, Action contextAction) {
		this.agent = agent;
		event = eventType;
		action = contextAction;
		this.decider = agent.getDecider();
	}
	public AgentEvent(Agent agent, Type eventType, Action contextAction, Decider decider, List<ActionEnum> allActions) {
		this.agent = agent;
		event = eventType;
		action = contextAction;
		chooseableOptions = allActions;
		this.decider = decider;
	}
	
	public Type getEvent() {return event;}
	public Agent getAgent() {return agent;}
	public Action getAction() {return action;}
	public List<ActionEnum> getChooseableOptions() {return chooseableOptions;}
	public Decider getDecider() {return decider;}
}

