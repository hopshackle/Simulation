package hopshackle.simulation;

import java.io.File;
import java.util.List;

public class Name {

	protected List<String> nameArray;
	
	public Name (File f) {
		nameArray = HopshackleUtilities.createListFromFile(f);
	}
	
	public String getName() {
		int choice = (int) (Math.random()*nameArray.size());
		return nameArray.get(choice);
	}

}
