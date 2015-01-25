package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

import java.util.*;
import java.util.logging.Logger;

public class TradeDeciderExp extends TradeDecider {
	
	public static ArrayList<GeneticVariable> variables = new ArrayList<GeneticVariable>(EnumSet.allOf(TradeGeneticEnum.class));

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public double getValue(Artefact item, Agent buyer, Genome g) {
		double retValue = 0;
		double functionalValue = 0;
		double tradeValue = 0;

		tradeValue =  g.getValue(TradeValuationsExpert.VALUATION, buyer, item, variables);
		tradeValue *= (0.85 + Math.random()*0.3);
		// this is to apply a random factor, so that over a single market
		// period, an Expert does not consistently create all of a single item
		// (unless the prospective profit is very high relative to other options)

		retValue = Math.max(functionalValue, tradeValue);
		return retValue;
	}

	public double getValueBid (Artefact item, Agent buyer, Genome g) {
		double retValue = 0;
		double functionalValue = 0;
		double tradeValue = 0;

		tradeValue =  g.getValue(TradeValuationsExpert.RESERVE, buyer, item, variables);
		retValue = Math.max(functionalValue, tradeValue);
		return retValue;
	}

	public double getValueOffer (Artefact item, Agent buyer, Genome g) {
		double retValue = 0;
		double functionalValue = 0;
		double tradeValue = 0;

		tradeValue =  g.getValue(TradeValuationsExpert.BASIC, buyer, item, variables);

		retValue = Math.max(functionalValue, tradeValue);
		return retValue;
	}

	public void trade(Character c, Recipe recipe, int number) {
		// this overrides the default standard

		if (c.isDead()) return;
		Location l = c.getLocation();
	//	logger.info("Trader starting");
		Market m = null;
		if (l != null)
			m = l.getMarket();
		if (m != null) {

			// first of all, we preferentially buy the ingredients for the current recipe
			if (recipe != null) {
				HashMap<Artefact, Double> ingredients = recipe.getIngredients();

				for (Artefact i : ingredients.keySet()) {
					// key difference is that the Valuation Gene is used
					double bidValue = 0.0;
					double needed = recipe.getIngredients().get(i) * number;
					
					for (int n=0; (double) n <= needed; n++) {
						bidValue = getValue(i, c, c.getGenome());	// re-do each time given random factor
						if (needed > 0 && bidValue < c.getGold()) {
							Bid newBid = new Bid(c, Math.max(bidValue, 0.0), i);
							m.addBid(newBid);
							if (c.getGold()< 1.0) break;
							// If no money left, then move on
						}
					}
				}
			}
			for (Artefact i : m.getItems()) {
				double bidValue;
				bidValue = getValueBid(i, c, c.getGenome());

				if (bidValue > 0 && bidValue < c.getGold()) {
					Bid newBid = new Bid(c, bidValue, i);
					m.addBid(newBid);
					if (c.getGold()< 1) break;
					// If no money left, then move on
				}
			}
			// Now determine what to sell
			// Experts just sell everything based on basic valuations
			// but not anything which is required for the current recipe
			ArrayList<Artefact> itemsToSell = new ArrayList<Artefact>();
			Hashtable<Artefact, Double> itemsValued = new Hashtable<Artefact, Double>();
			for (Artefact i : c.getInventory())
				if (recipe == null || (recipe != null && !recipe.getIngredients().containsKey(i)))
					itemsToSell.add(i);

			for (Artefact i : itemsToSell) {
				double offerValue;
				if (itemsValued.containsKey(i)) 
					offerValue = itemsValued.get(i);
				else {
					offerValue =  getValueOffer(i, c, c.getGenome());
					if (offerValue < 0) offerValue = 0;
					itemsValued.put(i, offerValue);
				}
				Offer o = new Offer(c, i, offerValue);
				m.addItem(o);
			}
		}
	//	logger.info("Trader Stopped");
	}
}
