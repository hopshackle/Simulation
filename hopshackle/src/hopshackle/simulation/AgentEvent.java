package hopshackle.simulation;

import java.awt.AWTEvent;

public class AgentEvent extends AWTEvent {

	private static final long serialVersionUID = 1L;
	private Agent agent;
	private AgentEvents event;
	private Action<?> action;
	
	public AgentEvent(Agent a, AgentEvents e) {
		super(a, e.getID());
		agent = a;
		event = e;
		action = agent.getActionPlan().getLastAction();
	}
	
	public AgentEvents getEvent() {return event;}
	public Agent getAgent() {return agent;}
	public Action<?> getAction() {return action;}
}

