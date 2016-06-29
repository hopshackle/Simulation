package hopshackle.simulation;

public class AgentEvent {
	
	public enum Type {
		BIRTH,
		DEATH,
		DECISION_TAKEN,
		ACTION_AGREED,
		ACTION_REJECTED,
		ACTION_CANCELLED,
		DECISION_STEP_COMPLETE;
	}

	private Agent agent;
	private Type event;
	private Action action;
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
	public AgentEvent(Agent agent, Type eventType, Action contextAction, Decider decider) {
		this.agent = agent;
		event = eventType;
		action = contextAction;
		this.decider = decider;
	}
	
	public Type getEvent() {return event;}
	public Agent getAgent() {return agent;}
	public Action getAction() {return action;}
	public Decider getDecider() {return decider;}
}

