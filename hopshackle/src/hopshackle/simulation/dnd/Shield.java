package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

public enum Shield implements DnDArtefact, HasBaseCost {

	LARGE_SHIELD (2, 10, 7),
	SMALL_SHIELD (1, 10, 3),
	TOWER_SHIELD (4, 2, 30);

	private int AC;
	private int maxDex;
	private int DC;
	private double makeCost;
	
	Shield (int ACBonus, int dex, double makeCost) {
		AC = ACBonus;
		maxDex = dex;
		DC = 10 + AC;
		this.makeCost = makeCost;
	}

	public int getACChange(Agent owner) {
		if (!(owner instanceof Character)) return 0;
		Character c = (Character) owner;
		int currentArmourAC = 0;
		if (c.getShield() != null) currentArmourAC += c.getShield().AC;
		int dexMod = c.getDexterity().getMod();
		int currentDexMod = dexMod;
		if (c.getShield() != null) currentDexMod = Math.min(dexMod, c.getShield().maxDex);
		int newDexMod = Math.min(dexMod, maxDex);
		return (AC + newDexMod - currentArmourAC - currentDexMod);
	}

	public int getACBonus() {
		return AC;
	}
	public int getMaxDexBonus() {
		return maxDex;
	}
	public double getAvgDmgChange(Agent owner) {
		return 0;
	}
	public double getOneOffHeal() {return 0.0;}
	

	public int getMakeDC() {
		return DC;
	}

	public int getHitBonus(Agent owner) {
		return 0;
	}
	
	public Recipe getRecipe() {
		double gold = 0;
		double metal = 0.0, wood = 0.0;
		switch (this) {
		case SMALL_SHIELD:
			wood = 0.5;
			break;
		case LARGE_SHIELD:
			wood = 1.0;
			break;
		case TOWER_SHIELD:
			metal = 5.0;
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
