package hopshackle.simulation.basic;

import java.util.*;

import hopshackle.simulation.*;

public class BasicAction extends Action<BasicAgent> {

	public BasicAction(BasicAgent a, long duration, boolean recordAction) {
		this(a, 0l, duration, recordAction);
	}

	public BasicAction(BasicAgent a, long start, long duration, boolean recordAction) {
		super(a, start, duration, recordAction);
	}

	public BasicAction(List<BasicAgent> mandatory, List<BasicAgent> optional, long startOffset, 
			int duration, boolean recordAction) {
		super(mandatory, optional, startOffset, duration, recordAction);
	}
	

}
