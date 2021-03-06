package hopshackle.simulation;

public enum DummyAction implements ActionEnum {

	DUMMY;

	@Override
	public Action<?> getAction(Agent a) {
		return new Action<Agent>(DUMMY, a, false) {

		};
	}

	@Override
	public boolean isChooseable(Agent a) {
		return true;
	}

	@Override
	public Enum<?> getEnum() {
		return DUMMY;
	}

	@Override 
	public String toString() {
		return "DUMMY";
	}

};

