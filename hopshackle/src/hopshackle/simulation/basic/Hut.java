package hopshackle.simulation.basic;

import hopshackle.simulation.*;

public class Hut extends Location implements Artefact {

	private Agent owner;
	private BuildingType typeOfBuilding;
	private BasicHex hexLocation;
	private long timeLastOccupied = 0;
	private long HUT_VACANCY_TIMELIMIT = Long.valueOf(SimProperties.getProperty("HutVacancyTimeLimitUntilCollapse", "50000"));
	private long HUT_BECOMES_CLAIMABLE_AFTER = (long) SimProperties.getPropertyAsDouble("HutVacancyTimeLimitUntilClaimable", "20000");

	public Hut(Agent builder) {
		builder.removeItem(Resource.WOOD);
		builder.removeItem(Resource.WOOD);
		builder.removeItem(Resource.WOOD);

		Location currentLocation = builder.getLocation();
		if (currentLocation instanceof BasicHex) {
			hexLocation = (BasicHex)currentLocation;
		} else {
			throw new AssertionError("Hut must be built in a Hex");
		}
		owner = builder;
		typeOfBuilding = BuildingType.HUT;
		Village villageInHex = hexLocation.getVillage();
		if (villageInHex != null)
			this.setParentLocation(villageInHex);
		else
			this.setParentLocation(currentLocation);
		owner.addItem(this);

		if (villageInHex == null && hexLocation.getHuts().size() > 9 ) {
			new Village(hexLocation);
		}
	}

	public void destroy() {
		this.setParentLocation(null);
		if (hexLocation != null) {
			hexLocation.changeMaxCarryingCapacity(1);
			hexLocation = null;
		}
		if (owner != null) {
			owner.removeItem(this);
		}
	}

	public Agent getOwner() {
		return owner;
	}

	public BuildingType getTypeOfBuilding() {
		return typeOfBuilding;
	}

	public BasicHex getLocation() {
		return hexLocation;
	}

	@Override
	public double costToMake(Agent a) {
		return typeOfBuilding.costToMake(a);
	}

	@Override
	public int getMakeDC() {
		return typeOfBuilding.getMakeDC();
	}

	@Override
	public Recipe getRecipe() {
		return typeOfBuilding.getRecipe();
	}

	@Override
	public long getTimeToMake(Agent a) {
		return typeOfBuilding.getTimeToMake(a);
	}

	@Override
	public boolean isA(Artefact item) {
		return item.isA(typeOfBuilding);
	}

	@Override
	public String toString() {
		if (parentLocation instanceof Village) {
			return "Hut in " + parentLocation.toString();
		} else {
			return "Hut at " + hexLocation;
		}
	}

	@Override
	public void maintenance() {
		super.maintenance();
		if (owner != null && owner.isDead())
			owner = null;
		if (isOccupied()) {
			timeLastOccupied = world.getCurrentTime();
		} else {
			if (getTimeLeftVacant() > HUT_VACANCY_TIMELIMIT)
				this.destroy();
		}
	}

	public boolean isOccupied() {
		if (owner == null) return false;
		return owner.getLocation() == hexLocation;
	}


	public long getTimeLeftVacant() {
		if (isOccupied()) {
			return 0;
		}
		return world.getCurrentTime() - timeLastOccupied;
	}

	public boolean isClaimable() {
		if (owner == null)
			return true;
		return (getTimeLeftVacant() - HUT_BECOMES_CLAIMABLE_AFTER > 0);
	}


	/* Change Ownership is called by addItem() and removeItem(). All it is
	 * responsible for is updating any ownership details on the item itself.
	 * (non-Javadoc)
	 * @see hopshackle.simulation.Artefact#changeOwnership(hopshackle.simulation.Agent)
	 */
	@Override
	public void changeOwnership(Agent newOwner) {
		BasicAgent inheritor = (BasicAgent)newOwner;
		owner = inheritor;
	}


	@Override
	public boolean isInheritable() {
		return typeOfBuilding.isInheritable();
	}
}

