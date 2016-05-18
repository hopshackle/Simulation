package hopshackle.simulation;

import java.awt.AWTEvent;

public class AgentEvent extends AWTEvent {

	private static final long serialVersionUID = 1L;
	private Agent agent;
	private AgentEvents event;
	
	public AgentEvent(Agent a, AgentEvents e) {
		super(a, e.getID());
		agent = a;
		event = e;
	}
	
	public AgentEvents getEvent() {return event;}
	public Agent getAgent() {return agent;}
}

