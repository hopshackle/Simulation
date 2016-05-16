package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.*;

public class BasicUtilities {

	public static List<Agent> partnersAsList(BasicAgent a1, BasicAgent a2) {
		List<Agent> retValue = new ArrayList<Agent>();
		retValue.add(a1);
		retValue.add(a2);
		return retValue;
	}
}
