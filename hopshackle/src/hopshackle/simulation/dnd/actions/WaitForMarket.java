package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Character;

import java.awt.event.*;

public class WaitForMarket extends Action {

	private Recipe recipe;
	private int number;
	private Market m;
	
	public WaitForMarket(Agent actor, Recipe recipe, int numberToMake) {
		super(actor, 0, false);
		this.recipe = recipe;
		number = numberToMake;
	}

	protected void doStuff() {
		Character c = (Character) actor;
		m = c.getWorld().getMarket();
		m.addListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("MarketCleared")) {
					actor.addAction(new CheckMaterials(actor, recipe, number));
					m.removeListener(this);
				} else {
					// nothing
				}
			}
		});
	}
	
	protected void doNextDecision() {
		// nothing - this is covered from the Listener waiting
		// for the market to clear
	}
	
	public String toString() {return "WAIT_FOR_MARKET";}
}
