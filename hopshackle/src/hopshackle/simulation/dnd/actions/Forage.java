package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

public class Forage extends Action {
	
	public Forage(Agent c, int p) {
		super (c, p, true);
		
	}
	
	protected void doStuff() {
		// simply gain 2 wood or 1 metal
		// decide which is more valuable
		Character c = (Character) actor;
		
		double[] value = new double[2];
		value [0] = c.getValue(Resource.WOOD) * 2.0;
		value [1] = c.getValue(Resource.METAL);
		
		if (value[0] > value[1]){
				c.addItem(Resource.WOOD);
				c.addItem(Resource.WOOD);
		} else {
			c.addItem(Resource.METAL);
		}
	}

	public String toString() {return "FORAGE";}
}
