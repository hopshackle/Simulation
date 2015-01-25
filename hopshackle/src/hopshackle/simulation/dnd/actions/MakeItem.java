package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.TradeValuationsExpert;

public class MakeItem extends Action {

	private int numberMade;
	private Artefact itemMade;
	
	public MakeItem(Character c, Artefact item, int number) {
		this(c, item, number, item.getTimeToMake(c));
	}
	public MakeItem(Character c, Artefact item, int number, long timeToCreate) {
		super(c, timeToCreate, true);
		itemMade = item;
		numberMade = number;
		c.log("Makes " + number + " " +item);
		// also need to remove all the ingredients
		Recipe r = itemMade.getRecipe();
		for (Artefact i : r.getIngredients().keySet()) {
			int numberNeeded = (int)((r.getIngredients().get(i)*number)+0.99);
			for (int n=0; n<numberNeeded; n++)
				c.removeItem(i);
		}
		c.addGold(-number * r.getGold());
		c.addXp((long)(5.0 * -number * r.getGold()));
	}
	
	protected void doStuff() {
		if (actor.isDead()) return;
		Genome g = actor.getGenome();
		double reservePrice = g.getValue(TradeValuationsExpert.RESERVE, actor, itemMade, null);
		if (reservePrice < 0) reservePrice = 0;
		for (int loop=0; loop<numberMade; loop++) {
			Offer o = new Offer(actor, itemMade, (int)reservePrice);
			Location l = actor.getLocation();
			if (l != null)
				actor.getLocation().getMarket().addItem(o);
		}
	}
	
	public Artefact getProduct() {return itemMade;}
	public int getVolume() {return numberMade;}
	
	public String toString() {return "MAKE_ITEM";}
}
