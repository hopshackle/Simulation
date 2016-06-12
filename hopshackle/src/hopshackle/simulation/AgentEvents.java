package hopshackle.simulation;

public enum AgentEvents {
	DEATH,
	DECISION_STEP_COMPLETE;

	public int getID() {
		int retValue = 0;
		switch (this) {
		case DEATH:
			retValue = 1;
			break;
		case DECISION_STEP_COMPLETE:
			retValue = 2;
			break;
		}
		return retValue;
	}
	
}
