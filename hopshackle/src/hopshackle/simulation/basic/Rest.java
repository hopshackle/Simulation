package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.List;

public class Rest extends BasicAction {

	public Rest(BasicAgent a) {
		this(a, 1000);
	}
	public Rest(BasicAgent a, long time) {
		super(BasicActions.REST, a, time, true);
	}

	public void doStuff() {
		GiftPolicy giftPolicy = new BasicGiftPolicy();
		giftPolicy.giveItems((BasicAgent)actor);
		claimAHutIfOneIsCurrentlyAvailable();
	}

	private void claimAHutIfOneIsCurrentlyAvailable() {
		Location here = actor.getLocation();
		if (here == null) return;
		List<Hut> huts = ((BasicHex)here).getHuts();
		for (Hut h : huts) {
			if (h.isClaimable()) {
				Agent oldOwner = h.getOwner();
				if (oldOwner != null) {
					oldOwner.log("Hut at " + here.toString() + " is claimed by " + actor.toString());
					oldOwner.removeItem(h);
				}
				actor.addItem(h);
				actor.log("Claims a vacant hut in " + here.toString());
				break;
			}
		}
	}

	public String toString() {
		return "REST";
	}

}
