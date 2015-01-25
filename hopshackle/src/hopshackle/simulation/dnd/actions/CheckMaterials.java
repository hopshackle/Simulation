package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

import java.util.*;

public class CheckMaterials extends Action {

	private Recipe recipe;
	private int number;

	public CheckMaterials(Agent actor, Recipe recipe, int number) {
		super(actor, (int)(Math.random()*100), false);
		this.recipe = recipe;
		this.number = number;
	}

	/*
	 *  Returns the proportion (between 0.0 and 1.0) of the
	 *  available materials currently in inventory to cook
	 *  this particular recipe and volume
	 */
	public double hasStuff() {
		double retValue = 1.0;

		Character c = (Character) actor;
		if (recipe.getGold() > 0 && number > 0)
			retValue = Math.min(retValue, c.getGold()/(recipe.getGold()*number));

		HashMap<Artefact, Double> list = recipe.getIngredients();
		List<Artefact> inventory = c.getInventory();
		for (Artefact ingredient: list.keySet()) {
			double numberNeeded = list.get(ingredient)*number;
			if (numberNeeded > 0) {
				int numberPresent = 0;
				for (Artefact i : inventory) {
					if (i.equals(ingredient))
						numberPresent++;
				}
				retValue = Math.min(retValue, (double)numberPresent / numberNeeded);
			}
		}

		return retValue;
	}
	protected void doStuff() {
		// nothing - actually check this in doNextDecision
	}

	protected void doNextDecision() {
		double stuff = hasStuff();
		int unitsMakeable = (int)(stuff * number);
		if (unitsMakeable > 0) {
			actor.addAction(new MakeItem((Character)actor, recipe.getProduct(), unitsMakeable));
		} else {
			super.doNextDecision();
		}
	}
	
	public String toString() {return "CHECK_MATERIALS";}
}
