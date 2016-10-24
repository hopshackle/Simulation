package hopshackle.simulation;

import java.util.List;

public abstract class GiftPolicy {

	public <T extends Agent> void giveItems(T giver) {
		List<Gift> giftList = getListOfItemsGiven(giver);
		for (Gift gift : giftList) {
			giver.log("Gives " + gift.item.toString() + " to " + gift.recipient.toString());
			gift.recipient.log("Receives " + gift.item.toString() + " from " + giver.toString());
			giver.removeItem(gift.item);
			gift.recipient.addItem(gift.item);
		}
	}

	public abstract <T extends Agent> List<Gift> getListOfItemsGiven(T giver);
	

	public class Gift {
		public Agent recipient;
		public Artefact item;
		
		public Gift(Agent recipient, Artefact item) {
			this.recipient = recipient;
			this.item = item;
		}
	}
}

