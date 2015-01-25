package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

import java.io.File;
import java.util.List;

public final class Name {

	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	private static List<String> nameArray = HopshackleUtilities.createListFromFile(new File(baseDir + "\\DNDNames.txt"));

	public static String getName(Race r, long id) {
	
		if (r == Race.HUMAN) {
			int choice = (int) (Math.random()*nameArray.size());
			return nameArray.get(choice);
		} else
			return (r.toString() + Long.toString(id));
	}
}
