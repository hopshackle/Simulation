package hopshackle.simulation;

import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public class Bid {

	protected double amount;
	private Agent buyer;
	private Artefact item;

	public Bid(Agent buyer, double amount, Artefact item) {
		this.buyer = buyer;
		this.item = item;
		this.amount = amount;
		buyer.addGold(-amount);
	}

	public boolean resolve(boolean filled, double actualPrice) {
		if (filled) {
			if (buyer.isDead()) {
				return false;
			} else {
				buyer.addItem(item);
				buyer.addGold(amount-actualPrice);
				if (buyer instanceof Character) {
					Character c = (Character) buyer;
					if (c.getChrClass() == CharacterClass.EXPERT) {
						c.addXp((int)(5.0*(-actualPrice)));
					}
				}
				buyer.log(String.format("Buys %s for %.2f", item.toString(), actualPrice));
				return true;
			}
		} else {
			if (buyer.isDead()) {
				return false;
			} else {
				buyer.addGold(amount);
				buyer.log("Fails to buy " + item.toString() + ". Money returned.");
				return true;
			}
		}
	}

	public double getAmount() {
		return amount;
	}

	public Agent getBuyer() {
		return buyer;
	}

	public Artefact getItem() {
		return item;
	}

	public String toString() {
		return item + ", " + amount + ", by " +buyer.toString();
	}
}
