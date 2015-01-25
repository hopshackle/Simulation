package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

public class Trade extends Action {

	private Recipe recipe = null;
	private int number = 0;

	public Trade(Agent a, long start) {
		super(a, start, true);
	}

	public void addRecipe(Recipe r, int number) {
		recipe = r;
		this.number = number;
	}
	public Recipe getRecipe() {
		return recipe;
	}
	public int getNumber() {return number;}

	protected void doStuff() {
		Character c = (Character) actor;
		c.trade(recipe, number);
	}

	protected void doNextDecision() {
		if (recipe == null) {
			super.doNextDecision();
		} else {
			CheckMaterials check = new CheckMaterials(actor, recipe, number);
			if (check.hasStuff() < 1.0) {	
				// if has stuff already, don't trade
				// when we finish, we want to wait for market cycle to end
				actor.addAction(new WaitForMarket(actor, recipe, number));
			} else {
				actor.addAction(new MakeItem((Character)actor, recipe.getProduct(), number));
			}
		}
	}
	public String toString() {return "TRADE";}

}
