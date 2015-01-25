package hopshackle.simulation;

import java.awt.event.AWTEventListener;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;
import java.util.logging.Logger;

public abstract class Agent extends Observable {

	protected Location location;
	protected World world;
	protected Decider decider;
	protected static Logger errorLogger = Logger.getLogger("hopshackle.simulation");
	protected Genome genome;
	public static String newline = System.getProperty("line.separator");
	protected int generation;
	protected boolean culled = false;
	protected Location birthLocation, deathLocation;
	protected List<Long> parents;
	protected List<Long> children;
	private Lock agentLock = new ReentrantLock();
	private InheritancePolicy inheritancePolicy = null;

	protected static boolean debug = false;
	protected static boolean fullDebug = false;
	private static AtomicLong idFountain = new AtomicLong(1);
	protected boolean debug_this = false;

	protected long birth = 0;
	protected long death = -1;
	private long uniqueID;
	protected List<Action> actionQueue;
	protected List<Action> executedActions;
	protected List<Artefact> inventory;
	protected List<Artefact> inventoryOnMarket = new ArrayList<Artefact>();
	protected Action actionOverride;
	protected double gold;

	protected List<AWTEventListener> listeners;
	protected static double throttle = SimProperties.getPropertyAsDouble("ActionThrottle", "1.0");
	protected double maxAge = SimProperties.getPropertyAsDouble("MaximumAgentAgeInSeconds", "100");
	protected static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	protected MapKnowledge knowledgeOfLocations;
	protected JourneyPlan currentJourneyPlan;
	private boolean knowsOfUnexploredLocations = true;

	private static HashMap<Long, Agent> cacheOfTheLiving = new HashMap<Long, Agent>();
	private static HashMap<Long, String> cacheOfTheDead = new HashMap<Long, String>();
	private static Queue<Agent> queueForTheFerryman = new LinkedList<Agent>();
	private static int deadAgentsToHoldInLivingCache = 1000;
	protected AgentRetriever<?> agentRetriever;	// to be set up by implementing subclass
	protected EntityLog logger;


	public Agent(World world) {
		if (world != null)
			uniqueID = idFountain.getAndIncrement();
		generalAgentInitialisation(world);
		cacheOfTheLiving.put(uniqueID, this);
	}

	public Agent (Location l, Decider d, World world) {
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
		if (world != null)
			setBirth(world.getCurrentTime());
		genome = new Genome();
		actionQueue = new ArrayList<Action>();
		executedActions = new ArrayList<Action>();
		inventory = new ArrayList<Artefact>();
		gold =0;
		parents = new ArrayList<Long>();
		children = new ArrayList<Long>();
		listeners = new ArrayList<AWTEventListener>();
		knowledgeOfLocations = new MapKnowledge(this);
	}

	public Genome getGenome() {
		return genome;
	}
	public void setGenome(Genome g) {
		genome = g;
	}

	public Decider getDecider() {
		return decider;
	}

	public void setDecider(Decider decider) {
		this.decider = decider;
	}

	public Action decide(Decider deciderOverride) {
		maintenance();
		Action retArray = null;
		if (!actionQueue.isEmpty())
			return null;
		if (deciderOverride == null) 
			deciderOverride = this.getDecider();

		if (deciderOverride != null) {
			// first of all we learn from last decision
			dispatchLearningEvent();

			// then we make the next decision
			if (!isDead()) {
				ActionEnum action = deciderOverride.decide(this);
				if (action != null)
					retArray = action.getAction(this);
			}
		}
		return retArray;
	}

	public Action decide() {
		if (getDecider() == null)
			errorLogger.severe("No decider in Agent.decide() for " + this.toString());
		return decide(getDecider());
	}

	protected void dispatchLearningEvent() {
		AgentEvent learningEvent = new AgentEvent(this, AgentEvents.DECISION_STEP_COMPLETE);
		eventDispatch(learningEvent);
	}

	public void die(String reason) {
		log("Dies. " + reason);

		if (inheritancePolicy != null) {
			inheritancePolicy.bequeathEstate(this);
		}

		if (queueForTheFerryman.size() >= deadAgentsToHoldInLivingCache) {
			Agent agentToRemoveFromCache = queueForTheFerryman.poll();
			cacheOfTheLiving.remove(agentToRemoveFromCache.getUniqueID());
			cacheOfTheDead.put(agentToRemoveFromCache.getUniqueID(), world.toString());
		}
		queueForTheFerryman.offer(this);

		purgeActions();
		death = getWorld().getCurrentTime();
		dispatchLearningEvent();
		deathLocation = getLocation();

		AgentEvent deathEvent = new AgentEvent(this, AgentEvents.DEATH);
		eventDispatch(deathEvent);

		// then tidy-up
		listeners.clear();
		setLocation(null);
		inventory.clear();

		log("Closing log file.");
		if (logger != null)
			logger.close();
	}

	public void addAction(Action newAction) {
		if (!isDead() && (getLocation() != null) && newAction != null) {
			if (actionOverride != null && newAction.startTime >= actionOverride.startTime) {
				newAction.delete();
				newAction = actionOverride;
				purgeActions();
			}
			actionQueue.add(newAction);
			getLocation().addAction(newAction);
		}
	}
	public void actionExecuted(Action oldAction) {
		if (oldAction != null) {
			removeAction(oldAction);
			executedActions.add(oldAction);
		} else errorLogger.warning("Null action sent");
	}
	protected void removeAction(Action oldAction) {
		oldAction.setEndTime(getWorld().getCurrentTime());
		actionQueue.remove(oldAction);
	}
	public void purgeActions(){
		while (getNextAction() != null) {
			Action nextAction = getNextAction();
			nextAction.delete();
			removeAction(nextAction);
		}
		if (actionOverride != null) {
			actionOverride.delete();
			actionOverride = null;
		}
	}
	public Action getNextAction() {
		if (actionQueue.isEmpty())
			return null;
		return actionQueue.get(0);
	}
	public List<Action> getActionQueue() {
		return HopshackleUtilities.cloneList(actionQueue);
	}
	public void setActionOverride(Action overrideAction) {
		// indicate that the agent has decided to override their previously chosen set of decisions
		Action nextAction = getNextAction();
		if (nextAction != null && nextAction.startTime >= overrideAction.startTime)
			purgeActions();

		if (!actionQueue.isEmpty()) {
			// still have at least one action to execute before the override takes effect
			actionOverride = overrideAction;
		} else {
			addAction(overrideAction);
		}
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
		if(!debug && !debug_this) return;
		if (logger == null) {
			logger = new EntityLog(toString(), world);
			logger.setEntityBirth(birth);
		}
		logger.log(s);
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
		return (int)(maxAge * 1000.0 * throttle);
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

	public List<Action> getExecutedActions() {
		return executedActions;
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
		}
	}
	private void addChild(Agent child) {
		Long childID = child.getUniqueID();
		if (!children.contains(childID)) {
			children.add(childID);
		}
	}

	public abstract double getScore();
	public abstract double getMaxScore();
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

	public void addListener(AWTEventListener el) {
		if (!listeners.contains(el))
			listeners.add(el);
	}
	public void removeListener(AWTEventListener el) {
		listeners.remove(el);
	}
	private void eventDispatch(AgentEvent ae) {
		for (AWTEventListener el : listeners) {
			el.eventDispatched(ae);
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
	public void setInheritancePolicy(InheritancePolicy ip) {
		inheritancePolicy = ip;
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
}
