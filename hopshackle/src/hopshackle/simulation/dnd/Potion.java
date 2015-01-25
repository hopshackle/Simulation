package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

public enum Potion implements DnDArtefact, HasBaseCost {

	CURE_LIGHT_WOUNDS(1);

	private int level;

	Potion(int level) {
		this.level = level;
	}

	public double costToMake(Agent a) {
		return DNDArtefactUtilities.costToMake(this, a);
	}

	public long getTimeToMake(Agent a) {
		return DNDArtefactUtilities.timeToMake(this, a);
	}

	public int getACChange(Agent owner) {
		return 0;
	}
	public double getAvgDmgChange(Agent owner) {
		return 0;
	}
	public int getHitBonus(Agent owner) {
		return 0;
	}
	public double getOneOffHeal() {
		double retValue = 0.0;
		switch (this) {
		case CURE_LIGHT_WOUNDS:
			retValue = 5.5;
		}
		return retValue;
	}

	public boolean drink(Character c) {
		if (c.isDead()) return false;
		boolean retValue = false;
		if (c.removeItem(Potion.CURE_LIGHT_WOUNDS)) {
			c.addHp(Dice.roll(1, 8)+1, false);
			c.log("Drinks Potion of Cure Light Wounds");
			retValue = true;
		}
		return retValue;
	}
	
	public int getMakeDC() {
		return 15 + (level * 2) - 1;
	}
	
	public Recipe getRecipe() {
		Recipe retValue = new Recipe(this, 0.0);
		switch(this) {
		case CURE_LIGHT_WOUNDS:
			retValue.addIngredient(Component.ORC_HEART, 5);
			break;
		}
		
		return retValue;
	}


	public double getBaseCost() {
		return ((level * 2) - 1) * 25;
	}

	@Override
	public double getMagicAttack() {
		return 0;
	}
	@Override
	public boolean isA(Artefact item) {
		return (item.equals(this));
	}

	@Override
	public void changeOwnership(Agent newOwner) {
	}

	@Override
	public boolean isInheritable() {
		return false;
	}
}
