package hopshackle.simulation;

import java.util.*;

public class BarterBid extends Bid {

	private List<Artefact> barterItems;
	private List<Artefact> reservedForUseWithItem;
	private BarterOffer barterOffer;

	public BarterBid(Agent buyer, List<Artefact> bid, BarterOffer itemBidFor) {
		this(buyer, bid, itemBidFor, new ArrayList<Artefact>());
	}
	public <T extends Artefact> BarterBid(Agent buyer, List<Artefact> bid, BarterOffer itemBidFor, List<T> reservedItems) {
		super(buyer, 0, itemBidFor.getItem());
		barterOffer = itemBidFor;
		barterItems = HopshackleUtilities.cloneList(bid);
		reservedForUseWithItem = new ArrayList<Artefact>();
		for (T item : reservedItems)
			reservedForUseWithItem.add(item);
	}

	public void accepted(double valueOfBid) {
		for (Artefact a : barterItems)
			this.getBuyer().removeItem(a);
		this.amount = valueOfBid;
		for (Artefact a : reservedForUseWithItem)
			this.getBuyer().removeItem(a);
	}

	public void rejected() {
		barterOffer = null;
		barterItems = null;
		reservedForUseWithItem = null;
	}

	public boolean resolve(boolean filled) {
		Agent buyer = getBuyer();
		for (Artefact reserve : reservedForUseWithItem)
			buyer.addItem(reserve);
		if (!filled) {
			// need to give back the bid items
			if (buyer.getFullDebug())
				buyer.log("Fails to buy " + barterOffer.toString() + ". Bid returned.");
			for (Artefact a : barterItems)
				buyer.addItem(a);
		}

		rejected();
		return true;
	}

	public List<Artefact> getBarterItems() {
		return HopshackleUtilities.cloneList(barterItems);
	}
}
