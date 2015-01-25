package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

public enum Armour implements DnDArtefact, HasBaseCost {
	
	LEATHER_ARMOUR (2, 6, 10),
	STUDDED_LEATHER_ARMOUR (3, 5, 25),
	CHAIN_SHIRT (4, 4, 100),
	CHAIN_MAIL (5, 2, 150),
	SPLINT_MAIL (6, 0, 200),
	BANDED_MAIL (6, 1, 250),
	HALF_PLATE (7, 0, 600),
	FULL_PLATE (8, 1, 1500);
	
	private int ACBonus;
	private int maxDex;
	private int DC;
	private double makeCost;

	Armour (int AC, int Dex, double makeCost) {
		ACBonus = AC;
		maxDex = Dex;
		DC = 10 + AC;
		this.makeCost = makeCost;
	}
	
	public int getACChange(Agent owner) {
		if (!(owner instanceof Character)) return 0;
		Character c = (Character) owner;
		int currentArmourAC = 0;
		if (c.getArmour() != null) currentArmourAC += c.getArmour().ACBonus;
		int dexMod = c.getDexterity().getMod();
		int currentDexMod = dexMod;
		if (c.getArmour() != null) currentDexMod = Math.min(dexMod, c.getArmour().maxDex);
		int newDexMod = Math.min(dexMod, maxDex);
		return (ACBonus + newDexMod - currentArmourAC - currentDexMod);
	}
	public int getACBonus() {
		return ACBonus;
	}
	public int getMaxDexBonus() {
		return maxDex;
	}

	public double getAvgDmgChange(Agent owner) {
		return 0;
	}

	public int getMakeDC() {
		return DC;
	}

	public int getHitBonus(Agent owner) {
		return 0;
	}
	public double getOneOffHeal() {
		return 0;
	}

	public Recipe getRecipe() {
		int gold = 0;
		double metal = 0.0, wood = 0.0;
		switch (this) {
		case LEATHER_ARMOUR:
			gold = 3;
			break;
		case STUDDED_LEATHER_ARMOUR:
			metal = 0.5;
			gold = 4;
			break;
		case CHAIN_SHIRT:
			metal = 5.0;
			break;
		case CHAIN_MAIL:
			metal = 8.0;
			break;
		case SPLINT_MAIL:
			metal = 10.0;
			wood = 2.0;
			break;
		case BANDED_MAIL:
			metal = 10.0;
			break;
		case HALF_PLATE:
			metal = 15.0;
			break;
		case FULL_PLATE:
			metal = 40.0;
			break;
		}
		Recipe retValue = new Recipe(this, gold);
		retValue.addIngredient(Resource.WOOD, wood);
		retValue.addIngredient(Resource.METAL, metal);
		return retValue;
	}

	public double costToMake(Agent a) {
		return DNDArtefactUtilities.costToMake(this, a);
	}

	public long getTimeToMake(Agent a) {
		return DNDArtefactUtilities.timeToMake(this, a);
	}

	public double getBaseCost() {
		return makeCost;
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
