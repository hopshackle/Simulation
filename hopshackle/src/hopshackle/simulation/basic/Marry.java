package hopshackle.simulation.basic;

import hopshackle.simulation.Action;

public class Marry extends Action {
	
	public Marry(BasicAgent partner1, BasicAgent partner2) {
		super(partner1, 1000, true);
		partner2.purgeActions();
		partner2.addAction(new ObeySpouse(partner2));
		new Marriage(partner1, partner2);
	}
	
	public void doStuff() {
		// nothing To Do
	}
	
	public String toString() {
		return "MARRY";
	}
	
}
