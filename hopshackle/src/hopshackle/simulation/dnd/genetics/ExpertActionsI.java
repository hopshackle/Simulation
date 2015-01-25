package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;

import java.util.*;

public enum ExpertActionsI implements ActionEnum {
	TRADE,
	FORAGE,
	MAKE_ITEM;

	private static String chromosomeName = "ED1";

	public String getChromosomeDesc() {return chromosomeName;}

	public boolean isChooseable(Agent a) {
		Character c = (Character) a;

		boolean retValue = false;
		switch(this) {
		case TRADE:
			if (c.getGold() > 5.00 || !c.getInventory().isEmpty()) 
				retValue = true;
			break;
		case FORAGE:
			retValue = true;
			break;
		case MAKE_ITEM:
			Skill s =  c.getSkill(Skill.skills.CRAFT);
			if (s!=null && 
					(c.getGold() > 5.00 || !c.getInventory().isEmpty())) 
				retValue = true;
			break;
		}

		return retValue;
	}
	
	public Action getAction(Agent a1, Agent a2) {
		return getAction(a1);
	}
	public Action getAction(Agent a) {
		Character c = (Character) a;
		Action retAction = null;
		switch(this) {
		case TRADE:
			retAction = new Trade(c, 500);
			break;
		case FORAGE:
			retAction = new Forage(c, 1000);
			break;
		case MAKE_ITEM:
			Stuff itemMade = getItem(c);
			if (itemMade.item != null) {
				Trade t = new Trade(c, 500);
				t.addRecipe(itemMade.item.getRecipe(), itemMade.number);
				retAction = t;
			} else 
				if (c.getGold()>5.0 || !c.getInventory().isEmpty())
					retAction = new Trade(c, 500);
				else 
					retAction = new Forage(c, 1000);
			break;
		}
		return retAction;
	}

	public EnumSet getGeneticVariables() {
		return EnumSet.allOf(ExpertGeneticEnum.class);
	}

	private static Stuff getItem(Character c) {
		/* 
		 * Cycle through all items that can be made
		 * For each one determine a value, and the time to create
		 * determine the most profitable per unit time
		 * and make that one
		 */
		Artefact retValue = null;
		int actualNumberMade = 0;

		Skill s =  c.getSkill(Skill.skills.CRAFT);
		Craft craftSkill = (Craft) s;
		double highestProfit = 0.0;
		for (Artefact i : craftSkill.getMakeableItems()) {

			double costToMake = i.costToMake(c);
			double profit = c.getValue(i) - i.costToMake(c);

			long timeToCreate = 0;
			long timeToTrade = 0;
			double profitPerUnitTime = 0.0;

			// we then need to decide how long it will take to gather the raw materials
			int newNumberMade = 0;
			int numberMade = 0;
			double oldProfit = 0.0;
			double mktPeriod = SimProperties.getPropertyAsDouble("MarketPeriod", "3000");
			do {
				oldProfit = profitPerUnitTime;
				numberMade = newNumberMade;

				if (newNumberMade == 0) {
					newNumberMade = 1;
					numberMade = 1;
				}
				else newNumberMade *=5;

				for (int n=0; n<5 && costToMake * newNumberMade >= c.getGold() && newNumberMade > 0; n++) 
					newNumberMade -= numberMade;

				if (costToMake * newNumberMade >= c.getGold())
					newNumberMade = 0;

				CheckMaterials cm = new CheckMaterials(c, i.getRecipe(), newNumberMade);
				double materials = cm.hasStuff();
				if (materials > 0.999) {
					// if we have the materials, then no need to trade
					timeToTrade = 0;
				} else if (((double)newNumberMade * materials) > (double)numberMade) {
					// if we have the materials for a portion, and this is more than we have already tested for
					// then set number made to this optimal number
					newNumberMade = (int)(materials * (double)newNumberMade);
					timeToTrade = 0;
				} else	{
					// we now iterate over all ingredients, and estimate the number of market cycles to purchase
					// the maximum number is our estimate
					Gene timeGene = c.getGenome().getGene(TradeValuationsExpert.TRADE_TIME, null);
					HashMap<Artefact, Double> ingredients = i.getRecipe().getIngredients();
					double maxCycles = 0;
					for (Artefact in : ingredients.keySet()) {
						if (ingredients.get(in) > 0) {
							double time = newNumberMade * timeGene.getValue(c, in);
							if (time > maxCycles) maxCycles = time;
						}
					}
					maxCycles *= (1-materials);
					// TODO: really this should be more granular at the level of each individual ingredient.
					// But this will work well for single material products (a large number of them)

					timeToTrade = (long) (mktPeriod * Math.max(1, (long)maxCycles));
				}

				timeToCreate = i.getTimeToMake(c) * newNumberMade;
				profitPerUnitTime = newNumberMade * profit / (double)(timeToTrade + timeToCreate + 50);

				if (profitPerUnitTime > highestProfit) {
					highestProfit = profitPerUnitTime;
					retValue = i;
					actualNumberMade = newNumberMade;
				}

			} while ((newNumberMade > numberMade || profitPerUnitTime > oldProfit) && timeToCreate < 10000);
			// stop when we cannot produce any more, or profit starts to decrease for this item

		}
		return new Stuff(actualNumberMade, retValue);
	}

	@Override
	public Enum<ExpertActionsI> getEnum() {
		return this;
	}
	
}


class Stuff {
	int number;
	Artefact item;

	Stuff(int n, Artefact item) {
		number = n;
		this.item = item;
	}
}
