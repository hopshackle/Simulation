package hopshackle.simulation;

import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.logging.Logger;

public class Offer {

	private double reservePrice;
	private Agent seller;
	private Artefact item;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private final static double MARKET_TAX = SimProperties.getPropertyAsDouble("MarketTax", "0.01");
	
	public Offer(Agent seller, Artefact item, double reservePrice) {
		if (seller == null) logger.warning("Offer with null Seller");
		if (item == null) logger.warning("Offer with null item");
		this.seller = seller;
		this.item = item;
		this.reservePrice = reservePrice;
		if (seller != null)	{
			seller.removeItem(item);
			seller.addItemToThoseOnMarket(item);
		}
	}

	public Artefact getItem() {
		return item;
	}

	public double getReservePrice() {
		return reservePrice;
	}
	public void setReservePrice(double newReserve) {
		reservePrice = newReserve;
	}

	public Agent getSeller() {
		return seller;
	}

	public void withdraw() {
		if (seller !=null)  {
			seller.addItem(item);
			seller.removeItemFromThoseOnMarket(item);
		}
		item = null;
	}

	public void resolve(double actualPrice) {
		if (actualPrice<reservePrice) {
			logger.warning("Offer has been met at below reservePrice");
		}
		if (seller != null) {
			seller.log(String.format("Sells %s for %.2f", item.toString(), actualPrice));
			double tax = MARKET_TAX * actualPrice;
			if (tax < 0.01) tax = 0.01; // minimum transaction charge of 1 copper piece
			seller.addGold(actualPrice-tax);
			if (seller instanceof Character) {
				Character c = (Character) seller;
				if (c.getChrClass() == CharacterClass.EXPERT) {
					c.addXp(Math.round(5.0*(actualPrice-tax)));
				}
			}
		}
	}

	public String toString() {
		return item.toString() + ", Reserve: " + reservePrice;
	}
}
