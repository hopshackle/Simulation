package hopshackle.simulation;

import hopshackle.simulation.AgentEvent.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public abstract class Agent extends Observable {

	protected static boolean debug = false;
	protected static boolean fullDebug = false;
	private static AtomicLong idFountain = new AtomicLong(1);
	public static String newline = System.getProperty("line.separator");
	protected static Logger errorLogger = Logger.getLogger("hopshackle.simulation");
	protected static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	
	private static HashMap<Long, Agent> cacheOfTheLiving = new HashMap<Long, Agent>();
	private static HashMap<Long, String> cacheOfTheDead = new HashMap<Long, String>();
	private static Queue<Agent> queueForTheFerryman = new LinkedList<Agent>();
	private static int deadAgentsToHoldInLivingCache = 1000;

	protected Location location;
	protected World world;
	protected Decider decider, doNothingDecider;
	protected ActionPlan actionPlan;
	protected Genome genome;
	protected int generation;
	protected boolean culled = false;
	protected Location birthLocation, deathLocation;
	protected List<Long> parents;
	protected List<Long> children;
	private Lock agentLock = new ReentrantLock();
	private Map<String, Policy<?>> policies = new HashMap<String, Policy<?>>(); 
	protected boolean debug_this = false;

	protected long birth = 0;
	protected long death = -1;
	private long uniqueID;

	protected List<Artefact> inventory;
	protected List<Artefact> inventoryOnMarket = new ArrayList<Artefact>();
	protected double gold;
	protected List<AgentListener> listeners;
	protected double maxAge = SimProperties.getPropertyAsDouble("MaximumAgentAgeInSeconds", "100");
	protected MapKnowledge knowledgeOfLocations;
	protected JourneyPlan currentJourneyPlan;
	private boolean knowsOfUnexploredLocations = true;
	protected AgentRetriever<?> agentRetriever;	// to be set up by implementing subclass
	protected EntityLog logger;

	public Agent(World world) {
		if (world != null)
			uniqueID = idFountain.getAndIncrement();
		generalAgentInitialisation(world);
		cacheOfTheLiving.put(uniqueID, this);
	}

	public Agent (Location l, Decider<?> d, World world) {
		this(world);
		setLocation(l);
		setDecider(d);
	}

	public Agent(World world, long uniqueID, long parent1, long parent2, List<Long> childrenList) {
		this.uniqueID = uniqueID;
		generalAgentInitialisation(world);
		if (parent1 > 0l) parents.add(parent1);
		if (parent2 > 0l) parents.add(parent2);
		this.children = childrenList;
	}

	private void generalAgentInitialisation(World world) {
		this.world = world;
		if (world != null) setBirth(world.getCurrentTime());
		genome = new Genome();
		inventory = new ArrayList<Artefact>();
		gold =0;
		actionPlan = new ActionPlan(this);
		parents = new ArrayList<Long>();
		children = new ArrayList<Long>();
		listeners = new ArrayList<AgentListener>();
		knowledgeOfLocations = new MapKnowledge(this);
	}

	public Genome getGenome() {
		return genome;
	}
	public void setGenome(Genome g) {
		genome = g;
	}

	@SuppressWarnings("unchecked")
	public <V extends Agent> Decider<V> getDecider() {
		return decider;
	}

	public void setDecider(Decider<?> decider) {
		this.decider = decider;
	}

	@SuppressWarnings({ "unchecked" })
	public void decide() {
		if (getDecider() == null)
			errorLogger.severe("No decider in Agent.decide() for " + this.toString());
		if (!isDead() && getDecider() != null) {
			Decider decider = getDecider();
			decider.decide(this);
		}
	}

	public void die(String reason) {
		log("Dies. " + reason);

		@SuppressWarnings("unchecked")
		Policy<Agent> inheritancePolicy = (Policy<Agent>) getPolicy("inheritance");
		if (inheritancePolicy != null) {
			inheritancePolicy.apply(this);
		}

		if (queueForTheFerryman.size() >= deadAgentsToHoldInLivingCache) {
			Agent agentToRemoveFromCache = queueForTheFerryman.poll();
			cacheOfTheLiving.remove(agentToRemoveFromCache.getUniqueID());
			cacheOfTheDead.put(agentToRemoveFromCache.getUniqueID(), world.toString());
		}
		queueForTheFerryman.offer(this);

		death = getWorld().getCurrentTime();
		deathLocation = getLocation();
		AgentEvent deathEvent = new AgentEvent(this, Type.DEATH);
		eventDispatch(deathEvent);

		// then tidy-up
		listeners.clear();
		purgeActions(true);
		actionPlan.executedActions.clear();
		setLocation(null);
		inventory.clear();
		clearMapKnowledge();

		if (logger != null)
			logger.close();
	}
	
	public void purgeActions(boolean overrideExecuting){
		actionPlan.purgeActions(overrideExecuting);
	}
	public Action<? extends Agent> getNextAction() {
		return actionPlan.getNextAction();
	}

	public Location getLocation() {
		return location;
	}
	public World getWorld() {
		return world;
	}

	public void setLocation(Location l) {
		if (birthLocation == null)
			birthLocation = l;
		if (location!=null){
			location.removeAgent(this);
		}
		location = l;
		if (location!=null) {
			log("Moves to " + l.toString() + "; " + l.additionalDescriptiveText());
			location.addAgent(this);
			knowledgeOfLocations.addLocation(l);
			for (Location adjacentLocation : l.accessibleLocations) {
				knowledgeOfLocations.addLocation(adjacentLocation);
			}
			knowledgeOfLocations.updateJourneyTrackers();
		}
		if (currentJourneyPlan != null && currentJourneyPlan.getDestination().equals(l)) {
			setJourneyPlan(null);
		}
	}

	public void addKnowledgeOfLocation(Location newLocation) {
		knowledgeOfLocations.addLocation(newLocation);
	}
	public void log(String s) {
		if(!getDebugLocal()) return;
		if (logger == null) {
			logger = new EntityLog(toString(), world);
			logger.setEntityBirth(birth);
		}
		logger.log(s);
		if (isDead()) logger.close();
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = generation;
		log("Generation: " + generation);
	}

	public void markAsCulled() {
		culled = true;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static void setDebugGlobal(boolean debug) {
		Agent.debug = debug;
	}
	public void setDebugLocal(boolean debug) {
		debug_this = debug;
	}
	public boolean getDebugLocal() {
		return debug_this || debug;
	}

	public boolean isDead() {
		if (death < 0){
			return false;
		} else return true;
	}

	public long getBirth() {
		return birth;
	}
	public void setBirth(long birth) {
		this.birth = birth;
	}
	public void setDeath(long death) {
		this.death = death;
	}

	public int getAge() {
		return (int)(getWorld().getCurrentTime() - birth);
	}

	public void addAge(int change) {
		birth = birth - change;
		setChanged();
		notifyObservers();
	}

	/**
	 * Mainly a convenience function for testing.
	 */
	public void setAge(int age) {
		long currentAge = getAge();
		long birth = getBirth();
		setBirth(birth + currentAge - age);
	}

	public int getMaxAge() {
		return (int)(maxAge * 1000.0);
	}

	public long getUniqueID() {
		return uniqueID;
	}
	public String toString() {
		return "Agent_"+String.valueOf(uniqueID);
	}
	public String getType() {
		return "AGENT";
	}

	public synchronized void addItem(Artefact item) {
		inventory.add(item);
		item.changeOwnership(this);
		log("Receives " + item);
	}
	public synchronized boolean removeItem(Artefact item) {
		item.changeOwnership(null);
		return inventory.remove(item);
	}
	public synchronized List<Artefact> getInventory() {
		return HopshackleUtilities.cloneList(inventory);
	}
	@SuppressWarnings("unchecked")
	public <T extends Artefact> List<T> getInventoryOf(T type) {
		List<Artefact> fullInv = getInventory();
		List<T> retValue = new ArrayList<T>();
		for (Artefact i : fullInv) {
			if (i.isA(type)) {
				retValue.add((T) i);
			}
		}
		return retValue;
	}
	@SuppressWarnings("unchecked")
	public <T extends Artefact> List<T> getInventoryOnMarketOf(T type) {
		List<Artefact> fullInv = HopshackleUtilities.cloneList(inventoryOnMarket);
		List<T> retValue = new ArrayList<T>();
		for (Artefact i : fullInv) {
			if (i.isA(type)) {
				retValue.add((T) i);
			}
		}
		return retValue;
	}

	public HashMap<Artefact, Integer> getSummaryInventory() {
		List<Artefact> fullInv = getInventory();
		HashMap<Artefact, Integer> retValue = new HashMap<Artefact, Integer>();
		for (Artefact i : fullInv) {
			if (retValue.containsKey(i)) {
				int newNumber = retValue.get(i)+1;
				retValue.put(i, newNumber);
			} else {
				retValue.put(i, 1);
			}
		}

		return retValue;
	}

	public int getNumberInInventoryOf(Artefact item) {
		List<Artefact> fullInv = getInventory();
		int retValue = 0;
		for (Artefact i : fullInv) {
			if (i.isA(item)) {
				retValue++;
			}
		}
		return retValue;
	}

	public double getGold() {
		return gold;
	}
	public void addGold(double change) {
		gold += change;
		if (change > 0.0) {
			log(String.format("Received %.2f gold. Total now %.2f", change, gold));
		} else if (change < 0.0){
			log(String.format("Loses %.2f gold. Total now %.2f", -change, gold));
		}
	}

	public List<Action<?>> getExecutedActions() {
		return actionPlan.executedActions;
	}

	public int getNumberOfChildren() {
		return children.size();
	}
	public List<Agent> getChildren() {
		List<Agent> retList = new ArrayList<Agent>();
		for (Long child : children) {
			Agent a = Agent.getAgent(child, agentRetriever, world);
			if (a != null)
				retList.add(a);
			else
				errorLogger.severe("Null child " + child + " for " + this.toString());
		}
		return retList;
	}

	public List<Long> getParents() {
		return parents;
	}

	public MapKnowledge getMapKnowledge() {
		return knowledgeOfLocations;
	}
	public void clearMapKnowledge() {
		knowledgeOfLocations = new MapKnowledge(this);
	}

	public void addParent(Agent parent) {
		Long parentID = parent.getUniqueID();
		if (!parents.contains(parentID)) {
			parents.add(parentID);
			parent.addChild(this);
			AgentEvent birthEvent = new AgentEvent(this, Type.BIRTH);
			parent.eventDispatch(birthEvent);
		}
	}
	private void addChild(Agent child) {
		Long childID = child.getUniqueID();
		if (!children.contains(childID)) {
			children.add(childID);
		}
	}

	public double getScore() {
		return 0;
	}
	public double getMaxScore() {
		return 0;
	}
	public void maintenance() {
		if (isDead()) return;

		for (Artefact item : getInventory()) {
			if (item instanceof ArtefactRequiringMaintenance) {
				ArtefactRequiringMaintenance arm = (ArtefactRequiringMaintenance) item;
				arm.artefactMaintenance(this);
			}
		}

		if (getAge() > getMaxAge()) {
			die("Of old age.");
		}
		if (culled && !isDead()) {
			die("Culled");
			errorLogger.info(toString() +" struck by lightning");
		}
		if (world.isDead())
			die("World has ended");
	}

	public void addListener(AgentListener el) {
		if (!listeners.contains(el))
			listeners.add(el);
	}
	public void removeListener(AgentListener el) {
		listeners.remove(el);
	}
	protected void eventDispatch(AgentEvent ae) {
		for (AgentListener el : listeners) {
			el.processEvent(ae);
		}
	}

	public static void clearAndResetCacheBuffer(int i) {
		if (i < 1) i = 1;
		deadAgentsToHoldInLivingCache = i;
		queueForTheFerryman.clear();
		cacheOfTheDead.clear();
		cacheOfTheLiving.clear();
	}

	public static Agent getAgent(long uniqueRef) {
		Agent retValue = cacheOfTheLiving.get(uniqueRef);
		return retValue;
	}
	public static Agent getAgent(long uniqueRef, AgentRetriever<?> agentRetriever, World world) {
		Agent firstAttempt = getAgent(uniqueRef);
		if (firstAttempt != null)
			return firstAttempt;

		Agent secondAttempt = null;
		String worldName = cacheOfTheDead.get(uniqueRef);
		if (worldName != null && worldName != "" && agentRetriever != null && world != null) {
			secondAttempt = agentRetriever.getAgent(uniqueRef, worldName, world);
		}
		return secondAttempt;
	}

	public JourneyPlan getJourneyPlan() {
		return currentJourneyPlan;
	}
	public void setJourneyPlan(JourneyPlan newJourneyPlan) {
		currentJourneyPlan = newJourneyPlan;
	}
	public Location getBirthLocation() {
		return birthLocation;
	}
	public Location getDeathLocation() {
		return deathLocation;
	}
	public boolean hasUnexploredLocations() {
		return knowsOfUnexploredLocations;
	}
	public void setHasUnexploredLocations(boolean state) {
		knowsOfUnexploredLocations = state;
	}

	public static StringBuffer convertToStringVersionOfIDs (List<Agent> agentList) {
		StringBuffer idsAsString = new StringBuffer();
		boolean firstAgent = true;
		for (Agent child : agentList) {
			if (child == null) continue;
			if (!firstAgent) {
				idsAsString.append(",");
			}
			firstAgent = false;
			idsAsString.append(child.getUniqueID());
		}
		return idsAsString;
	}

	public void getLock() {
		agentLock.lock();
	}	
	public void releaseLock() {
		agentLock.unlock();
	}
	public boolean tryLock() {
		return agentLock.tryLock();
	}

	public boolean getFullDebug() {
		return fullDebug;
	}

	public void addItemToThoseOnMarket(Artefact item) {
		inventoryOnMarket.add(item);
	}
	public void removeItemFromThoseOnMarket(Artefact item) {
		inventoryOnMarket.remove(item);
	}
	public Policy<?> getPolicy(String type) {
		return policies.getOrDefault(type, null);
	}
	public void setPolicy(Policy<?> newPolicy) {
		policies.put(newPolicy.type, newPolicy);
	}
	public ActionPlan getActionPlan() {
		return actionPlan;
	}
	@Override
    public int hashCode() {
		return (int) uniqueID;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof Agent) {
			return ((Agent)o).getUniqueID() == getUniqueID();
		} else {
			return false;
		}
	}
	
}
