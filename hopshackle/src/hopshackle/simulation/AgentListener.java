package hopshackle.simulation;

import java.util.EventListener;

public interface AgentListener extends EventListener {

	public abstract void processEvent(AgentEvent event);
}
