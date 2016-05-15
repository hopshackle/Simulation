package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.List;

public enum BasicActions implements ActionEnum {

	REST,
	FORAGE,
	BUILD,
	FARM,
	BREED,
	MARRY,
	FIND_PLAINS,
	FIND_FOREST,
	FIND_WATER,
	FIND_UNKNOWN,
	FIND_HUT,
	FIND_CIVILISATION;

	@Override
	public Action getAction(Agent a) {
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
			ba = (BasicAgent) a;
			PartnerFinder advertBreed = new PartnerFinder(ba, new BreedingPartnerScoringFunction(ba));
			if (advertBreed.getPartner() != null) {
				return new Breed(ba, advertBreed.getPartner());
			} else {
				return new Rest(a);
			}
		case MARRY:
			ba = (BasicAgent) a;
			PartnerFinder advertMarry = new PartnerFinder(ba, new MarriagePartnerScoringFunction(ba));
			if (advertMarry.getPartner() != null) {
				return new Marry(ba, advertMarry.getPartner());
			} else {
				return new Rest(a);
			}
		case FIND_WATER:
			return new BasicMove(a, new TerrainMatcher(TerrainType.OCEAN));
		case FIND_PLAINS:
			return new BasicMove(a, new WildernessMatcher(a));
		case FIND_FOREST:
			return new BasicMove(a, new TerrainMatcher(TerrainType.FOREST));
		case FIND_UNKNOWN:
			return new BasicMove(a, new UnexploredLocationMatcher(a.getMapKnowledge()));
		case FIND_HUT:
			return new BasicMove(a, new HutsOwnedByMatcher(a));
		case FIND_CIVILISATION:
			return new BasicMove(a, new CivilisationMatcher(a));
		}
		return null;
	}

	@Override
	public Action getAction(Agent a1, Agent a2) {
		return getAction(a1);
	}

	@Override
	public String getChromosomeDesc() {
		return "BASIC01";
	}

	@Override
	public boolean isChooseable(Agent a) {
		BasicHex h = (BasicHex)a.getLocation();
		BasicAgent ba = (BasicAgent) a;
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
			if (!ba.isMarried())
				return false;
			if (!FARM.isChooseable(a)) 
				return false;
			if (h.getAgents().size() < 2)
				return false;
			if (!ba.ableToBreed())
				return false;
			BasicAgent spouse = ba.getPartner();
			if (spouse == null || !spouse.ableToBreed())
				return false;
			if (a.getNumberInInventoryOf(Resource.FOOD) == 0) 
				return false;
			return true;
		case MARRY:
			if (ba.isFemale())
				return false;
			if (ba.isMarried()) 
				return false;
			return true;
		case FIND_FOREST:
			if (h.getTerrainType() == TerrainType.FOREST)
				return false;
			if (BasicVariables.FOREST.getProximityToTerrain(ba) < 0.01)
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
			if (BasicVariables.WATER.getProximityToTerrain(ba) < 0.01)
				return false;
			return true;
		case FIND_UNKNOWN:
			return ba.hasUnexploredLocations();
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
		}
		return true;
	}

	@Override
	public Enum<BasicActions> getEnum() {
		return this;
	}

}
