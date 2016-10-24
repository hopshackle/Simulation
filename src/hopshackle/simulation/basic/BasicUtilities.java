package hopshackle.simulation.basic;

import java.util.*;

public class BasicUtilities {

	public static List<BasicAgent> partnersAsList(BasicAgent a1, BasicAgent a2) {
		List<BasicAgent> retValue = new ArrayList<BasicAgent>();
		retValue.add(a1);
		retValue.add(a2);
		return retValue;
	}
}
