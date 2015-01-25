package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class ObeySpouse extends Action {

	private BasicAgent ba;
	private static BaseDecider obeySpouseDecider = new HardCodedDecider(BasicActions.OBEY_SPOUSE);
	
	public ObeySpouse(Agent a) {
		super(a, true);
		ba = (BasicAgent)a;
	}
	
	public void doStuff() {
		GiftPolicy giftPolicy = new BasicGiftPolicy();
		giftPolicy.giveItems((BasicAgent)actor);
	}
	
	public void doNextDecision() {
		if (!ba.isDead() && ba.isMarried()) {
			Action nextAction = actor.decide(obeySpouseDecider);
			actor.addAction(nextAction);
		} else {
			super.doNextDecision();
		}
			
	}
	
	public String toString() {
		return "OBEY_SPOUSE";
	}

}
