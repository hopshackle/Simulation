package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.List;

public enum BasicActions implements ActionEnum<BasicAgent> {

	REST,
	FORAGE,
	BUILD,
	FARM,
	BREED,
	LOOK_FOR_PARTNER,
	MARRY,
	FIND_PLAINS,
	FIND_FOREST,
	FIND_WATER,
	FIND_UNKNOWN,
	FIND_HUT,
	FIND_CIVILISATION;
	
	@Override
	public BasicAction getAction(BasicAgent a) {
		BasicAgent ba = (BasicAgent)a;
		switch (this) {
		case REST:
			return new Rest(a);
		case FORAGE:
			return new Forage(a);
		case BUILD:
			return new BuildHut(a);
		case FARM:
			return new Farm(a);
		case BREED:
			PartnerFinder advertBreed = new PartnerFinder(ba, new BreedingPartnerScoringFunction(ba));
			if (advertBreed.getPartner() != null) {
				return new Breed(ba, advertBreed.getPartner());
			} else {
				return new Rest(a);
			}
		case LOOK_FOR_PARTNER:
			return new LookForPartner(ba);
		case FIND_WATER:
			return new BasicMove(BasicActions.FIND_WATER, a, new TerrainMatcher(TerrainType.OCEAN));
		case FIND_PLAINS:
			return new BasicMove(BasicActions.FIND_PLAINS, a, new WildernessMatcher(a));
		case FIND_FOREST:
			return new BasicMove(BasicActions.FIND_FOREST, a, new TerrainMatcher(TerrainType.FOREST));
		case FIND_UNKNOWN:
			return new BasicMove(BasicActions.FIND_UNKNOWN, a, new UnexploredLocationMatcher(a.getMapKnowledge()));
		case FIND_HUT:
			return new BasicMove(BasicActions.FIND_HUT, a, new HutsOwnedByMatcher(a));
		case FIND_CIVILISATION:
			return new BasicMove(BasicActions.FIND_CIVILISATION, a, new CivilisationMatcher(a));
		default:
			break;
		}
		return null;
	}

	@Override
	public boolean isChooseable(BasicAgent a) {
		BasicHex h = (BasicHex)a.getLocation();
		List<Artefact> inventory = a.getInventory();
		switch (this) {
		case FORAGE:
			if (h.getCarryingCapacity() < 1) 
				return false;
			switch (h.getTerrainType()) {
			case PLAINS:
			case FOREST:
				return true;
			default:
				return false;
			}
		case BUILD:
			if (h.getTerrainType() != TerrainType.PLAINS)
				return false;
			if (a.getNumberInInventoryOf(Resource.WOOD) < 3)
				return false;
			if (h.getMaxCarryingCapacity() > 0) 
				return true;
			return false;
		case FARM:
			if (h.getTerrainType() != TerrainType.PLAINS)
				return false;
			boolean hasAHutInTheHex = false;
			for (Artefact item : inventory) {
				if (item instanceof Hut) {
					Hut hut = (Hut) item;
					if (hut.getLocation().equals(h)) {
						hasAHutInTheHex = true;
					}
				}
			}
			return hasAHutInTheHex;
		case BREED:
			if (!a.isMarried())
				return false;
			if (!FARM.isChooseable(a)) 
				return false;
			if (h.getAgents().size() < 2)
				return false;
			if (!a.ableToBreed())
				return false;
			BasicAgent spouse = a.getPartner();
			if (spouse == null || !spouse.ableToBreed())
				return false;
			if (a.getNumberInInventoryOf(Resource.FOOD) == 0) 
				return false;
			return true;
		case LOOK_FOR_PARTNER:
			if (a.isFemale())
				return false;
			if (a.isMarried() || a.getActionPlan().contains(BasicActions.MARRY)) 
				return false;
			return true;
		case MARRY:
			return false;
			// Only ever created after LOOK_FOR_PARTNER
		case FIND_FOREST:
			if (h.getTerrainType() == TerrainType.FOREST)
				return false;
			if (BasicVariables.FOREST.getProximityToTerrain(a) < 0.01)
				return false;
			return true;
		case FIND_PLAINS:
			if (h.getTerrainType() == TerrainType.PLAINS && h.getHuts().size() == 0)
				return false;
			return true;
		case FIND_WATER:
			if (h.getTerrainType() == TerrainType.OCEAN)
				return false;
			for (Location adjacentLocation : h.getAccessibleLocations()) {
				Hex adjacentHex = (Hex) adjacentLocation;
				if (adjacentHex.getTerrainType() == TerrainType.OCEAN) 
					return false;
			}
			if (BasicVariables.WATER.getProximityToTerrain(a) < 0.01)
				return false;
			return true;
		case FIND_UNKNOWN:
			return a.hasUnexploredLocations();
		case FIND_HUT:
			boolean hasHut = false;
			for (Artefact item : inventory) {
				if (item instanceof Hut) {
					Hut hut = (Hut)item;
					hasHut = true;
					if (hut.getLocation().equals(a.getLocation()))
						return false;
				}
			}
			return hasHut;
		case FIND_CIVILISATION:
			List<Location> potentialVillages = h.getChildLocations();
			for (Location l : potentialVillages) {
				if (l instanceof Village) 
					return false;
			}
			return true;
		case REST:
			return true;
		}
		return false;
	}

	@Override
	public Enum<BasicActions> getEnum() {
		return this;
	}

}
