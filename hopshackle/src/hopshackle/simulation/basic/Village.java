package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Village extends Location implements Artefact {

	protected static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	protected static Name villageNamer = new Name(new File(baseDir + "\\VillageNames.txt"));
	private static AtomicLong idFountain = new AtomicLong(1);
	private static AgentWriter<Village> villageWriter = new AgentWriter<Village>(new VillageDAO());
	private BuildingType typeOfBuilding;
	private BasicAgent currentLeader;
	private List<Long> allLeaders = new ArrayList<Long>();
	private long uniqueID;
	private long founded;

	public Village(BasicHex parent) {
		super(parent);
		uniqueID = idFountain.getAndIncrement();
		name = villageNamer.getName();
		founded = world.getCurrentTime();
		List<Hut> tempChildren = parent.getHuts();
		for (Hut h : tempChildren) {
			h.setParentLocation(this);
		}
		typeOfBuilding = BuildingType.VILLAGE;
		setLeader(getNewLeader());
	}

	private BasicAgent getNewLeader() {
		HashMap<BasicAgent, Integer> hutOwners = new HashMap<BasicAgent, Integer>();
		BasicAgent newLeader = null;
		List<Hut> huts = getHuts();
		for (Hut hut : huts) {
			BasicAgent hutOwner = (BasicAgent)hut.getOwner();
			if (hutOwners.containsKey(hutOwner)) {
				Integer numberOwned = hutOwners.get(hutOwner);
				numberOwned++;
				hutOwners.put(hutOwner, numberOwned);
			} else {
				hutOwners.put(hutOwner, 1);
			}

			int highestAge = 0;
			int mostOwned = 0;
			for (BasicAgent owner : hutOwners.keySet()) {
				if (owner != null) {
					int age = owner.getAge();
					int owned = hutOwners.get(owner);
					if (!owner.isDead() && (owned > mostOwned || (owned == mostOwned && age > highestAge))) {
						newLeader = owner;
						mostOwned = owned;
						highestAge = age;
					}
				}
			}
		}
		return newLeader;
	}

	public void destroy() {
		villageWriter.write(this, world.toString());
		List<Location> tempChildren = HopshackleUtilities.cloneList(childLocations);
		for (Location childLocation : tempChildren) {
			childLocation.setParentLocation(parentLocation);
		}
		this.setParentLocation(null);
		if (currentLeader != null) {
			currentLeader.log("Village of " + toString() + " ceases to exist.");
			currentLeader.removeItem(this);
		}
	}

	public void setLeader(BasicAgent newLeader) {
		if (currentLeader != null) {
			currentLeader.removeItem(this);
		}
		if (newLeader != null) 
			newLeader.addItem(this);
		// add and removeItem code will invoke changeOwnership automatically
	}

	@Override
	public void changeOwnership(Agent newOwner) {
		BasicAgent inheritor = (BasicAgent)newOwner;
		if (inheritor != currentLeader) {
			currentLeader = inheritor;
			if (inheritor != null) {
				allLeaders.add(inheritor.getUniqueID());
				inheritor.log("Becomes leader of " + toString());
				if (inheritor.getNumberInInventoryOf(BuildingType.VILLAGE) == 1) {	// i.e. this one
					inheritor.setSurname("of " + name);
				}
			}
		}
	}

	@Override
	public void maintenance() {
		super.maintenance();
		if (currentLeader == null || currentLeader.isDead()) {
			BasicAgent newLeader = getNewLeader();
			if (newLeader != null) {
				setLeader(newLeader);
			} else {
				this.destroy(); // no-one eligible. Village ceases to exist.
				return;
			}
		}
		if (getHuts().size() < 6) {
			this.destroy();
			return;
		}
		for (Hut hut : getHuts()) {
			if (hut.getOwner() == null || (hut.isClaimable() && !hut.getOwner().equals(currentLeader))) {
				if (hut.getOwner() != null)
					hut.getOwner().removeItem(hut);
				currentLeader.addItem(hut);
				currentLeader.log("Gains empty Hut in village of " + name);
			}
		}
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
		return typeOfBuilding.isA(item);
	}


	public Agent getOwner() {
		return currentLeader;
	}

	@Override
	public boolean isInheritable() {
		return typeOfBuilding.isInheritable();
	}

	public long getId() {
		return uniqueID;
	}
	public long getFoundationDate() {
		return founded;
	}

	public List<Long> getLeaders() {
		return allLeaders;
	}

	public List<Hut> getHuts() {
		List<Hut> retValue = new ArrayList<Hut>();
		for (Location l : childLocations) {
			if (l instanceof Hut) {
				retValue.add((Hut)l);
			}
		}
		return retValue;
	}
}
