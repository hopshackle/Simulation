package hopshackle.simulation;

import java.util.List;

public class BarterOffer extends Offer {

	private ValuationFunction<List<Artefact>> valFunction;
	protected double numberOfItem;
	protected BarterBid bestBid;

	/*
	 * The protocol is:
	 *  - BarterOffer is created, and allows interested parties to call getValue() to determine if their bids will be accepted
	 *  - A new BarterBid is submitted to BarterOffer via submitBid()
	 *  - If the best bid so far, then this returns true, and will also call accepted() on the bid. Otherwise it returns false, and calls rejected() on the bid.
	 *  - If a bid is later superceded, then BarterOffer will call resolve(false) to indicate that the bid ultimately failed
	 *  - For the final successful bid, BarterOffer will distribute the items bid for, and then call resolve(true) on the BarterBid
	 */
	public BarterOffer(Agent seller, Artefact item, double number, double reservePrice, ValuationFunction<List<Artefact>> valuationFunction) {
		super(seller, item, reservePrice);
		for (int i = 1; i < number; i++) {// first one is removed by super
			seller.removeItem(item);
			seller.addItemToThoseOnMarket(item);
		}
		numberOfItem = number;
		valFunction = valuationFunction;
		bestBid = null;
	}

	public boolean submitBid(BarterBid bid) {
		if (checkBidValidity(bid)) {
			bid.accepted(valueBid(bid.getBarterItems()));
			if (bestBid != null)
				bestBid.resolve(false);
			bestBid = bid;
			return true;
		} else {
			bid.rejected();
			return false;
		}
	}

	private boolean checkBidValidity(BarterBid bid) {
		double valueOfBid = valueBid(bid.getBarterItems());
		double currentBestBid = getBestBid();
		double reservePrice = getReservePrice();
		if (valueOfBid < reservePrice)
			return false;
		if (valueOfBid <= currentBestBid)
			return false;
		return true;
	}

	public void resolve() {
		Agent seller = getSeller();
		Artefact item = getItem();
		if (seller != null) {
			seller.removeItemFromThoseOnMarket(item);
			if (bestBid != null) {
				List<Artefact> priceReceived = bestBid.getBarterItems();
				String message = String.format("Sells %s for %.2f", item.toString(), bestBid.getAmount());
				if (numberOfItem > 1) 
					message = String.format("Sells %d %s for %.2f", (int)numberOfItem, item.toString(), bestBid.getAmount());
				seller.log(message);
				for (Artefact a : priceReceived) 
					seller.addItem(a);
			}
		}
		if (bestBid != null && !bestBid.getBuyer().isDead()) {
			Agent buyer = bestBid.getBuyer();
			for (int i = 0; i < numberOfItem; i++) {
				buyer.addItem(item);
			}
			String message = String.format("Buys %s for %.2f", item.toString(), getBestBid());
			if (numberOfItem > 1) 
				message = String.format("Buys %d %s for %.2f", (int)numberOfItem, item.toString(), getBestBid());
			buyer.log(message);
			bestBid.resolve(true);
		}
		bestBid = null;
	}

	@Override
	public void withdraw() {
		Agent seller = getSeller();
		Artefact item = getItem();
		for (int i = 0; i < numberOfItem; i++) {
			if (seller !=null)  {
				seller.addItem(item);
				seller.removeItemFromThoseOnMarket(item);
			}
		}
	}

	public double valueBid(List<Artefact> bid) {
		if (valFunction != null)
			return valFunction.getValue(bid);
		return bid.size();
	}

	public double getBestBid() {
		if (bestBid != null)
			return bestBid.getAmount();
		return 0.0;
	}

	@Override
	public String toString() {
		return String.format("%d %s", numberOfItem, getItem().toString());
	}

	public double getNumber() {
		return numberOfItem;
	}

	public Agent getCurrentWinner() {
		if (bestBid == null)
			return null;
		return bestBid.getBuyer();
	}
}
