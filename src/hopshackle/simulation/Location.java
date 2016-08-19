package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class Location implements Persistent {

	private static AtomicLong idFountain = new AtomicLong(1);
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	
	protected List<Location> childLocations;
	protected Location parentLocation;
	protected List<Location> accessibleLocations;
	protected List<Agent> agentsInLocation;
	protected String name;
	protected World world;
	private long uniqueID;

	public Location() {
		childLocations = new ArrayList<Location>();
		parentLocation = null;
		accessibleLocations = new ArrayList<Location>();
		agentsInLocation = new ArrayList<Agent>();
		uniqueID = idFountain.getAndIncrement();
	}

	public Location(Location parent) {
		this();
		setParentLocation(parent);
	}

	public Boolean isParent(Location l) {
		if (parentLocation == null) return false;
		return (parentLocation.equals(l));
	}
	public Location getParentLocation() {
		return parentLocation;
	}
	public synchronized void setParentLocation(Location newParentLocation) {
		if (newParentLocation != parentLocation) {
			if (parentLocation != null)
				parentLocation.removeChildLocation(this);
			if (newParentLocation != null)
				newParentLocation.addChildLocation(this);
		} 

		parentLocation = newParentLocation;
		Location superParent = parentLocation;
		while (!(superParent instanceof World) && superParent != null) {
			superParent = superParent.getParentLocation();
		}
		if (superParent != null) {
			world = (World)superParent;
		}
	}

	public boolean hasRouteTo(Location l) {
		return accessibleLocations.contains(l);
	}
	public synchronized boolean addAccessibleLocation(Location l) {
		if (hasRouteTo(l)) return false;
		accessibleLocations.add(l);
		return true;
	}
	public synchronized boolean removeAccessibleLocation(Location l) {
		if (!hasRouteTo(l)) return false;
		accessibleLocations.remove(l);
		return true;
	}

	public List<Location> getAccessibleLocations() {
		return HopshackleUtilities.cloneList(accessibleLocations);
	}

	public boolean isChild(Location l) {
		return childLocations.contains(l);
	}
	protected boolean addChildLocation(Location l) {
		if (isChild(l) || isParent(l)) return false;
		childLocations.add(l);
		return true;
	}
	protected boolean removeChildLocation(Location l) {
		boolean result = childLocations.contains(l);
		if (!result) return false;
		childLocations.remove(l);
		return true;
	}
	public List<Location> getChildLocations() {
		return HopshackleUtilities.cloneList(childLocations);
	}

	public synchronized boolean containsAgent(Agent a) {
		return agentsInLocation.contains(a);
	}
	public synchronized boolean addAgent(Agent a) {
		if (containsAgent(a)) return false;
		agentsInLocation.add(a);
		if (parentLocation != null) 
			parentLocation.addAgent(a);
		return true;
	}
	public synchronized boolean removeAgent(Agent a) {
		if (!containsAgent(a)) return false;
		agentsInLocation.remove(a);
		if (parentLocation != null) 
			parentLocation.removeAgent(a);
		return true;
	}

	public synchronized List<Agent> getAgents() {
		return HopshackleUtilities.cloneList(agentsInLocation);
	}

	public void maintenance() {
		List<Location> tempChildLocations = new ArrayList<Location>();
		for (Location l : childLocations) {
			tempChildLocations.add(l);
		}
		for (Location l : tempChildLocations) {
			l.maintenance();
		}
	}

	public boolean addAction(Action a) {
		if (a == null) return false;
		if (parentLocation != null) {
			parentLocation.addAction(a);
		} else return false;
		return true;
	}
	public Market getMarket() {
		if (parentLocation != null) return parentLocation.getMarket();
		return null;
	}
	public String toString() { return name;}
	public void setName(String name) {this.name = name;}

	public String additionalDescriptiveText() {
		return "";
	}

	@Override
	public World getWorld() {
		return world;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Location> Set<T> getAllChildLocationsOfType(T exemplar) {
		Set<T> retValue = new HashSet<T>();
		for (Location loc : childLocations) {
			retValue.addAll(loc.getAllChildLocationsOfType(exemplar));
			if (loc.getClass().equals(exemplar.getClass()))
				retValue.add((T)loc);
		}
		return retValue;
	}
	
	@Override
	public int hashCode() {
		return (int) uniqueID;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof Location) {
			return ((Location)o).uniqueID == uniqueID;
		} else {
			return false;
		}
	}
	
}
