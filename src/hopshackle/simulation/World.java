package hopshackle.simulation;

import java.awt.event.*;
import java.io.File;
import java.util.*;

public class World extends Location {

	public final int MAX_POPULATION;
	private ActionProcessor actionProcessor;
	private Market market;
	private String suffix;
	private WorldCalendar calendar;
	private TimerTask worldMaintenanceTask;
	private ArrayList<ActionListener> listeners;
	private long endOfWorldTime = System.currentTimeMillis();
	private Temperature temperature;
	private RecordActions actionRecorder;
	private DatabaseAccessUtility databaseUtility;
	private LocationMap locationMap;
	private boolean isAlive = true;
	private double lastTempPublished = 999;
	private long actualEndOfWorldTime;
	private Map<String, WorldLogic<?>> logicMap = new HashMap<String, WorldLogic<?>>();

	public World(ActionProcessor ap, String suffix, WorldLogic<?> logic){
		this(ap, suffix, 60000, logic);
		this.setLocationMap(new SquareMap(10, 10));
	}
	
	public World() {
		this(new SimpleWorldLogic<Agent>(null));
	}

	/**
	 * If used for a World, this will throw an AssertionError, as a World must be the top-level in any Location hierarchy.
	 * @param parentLocation
	 */
	public World(Location parentLocation) {
		throw new AssertionError("World cannot have a parent Location");
	}

	/**
	 * A convenience method without full functionality. For use primarily in unit-testing.
	 * 
	 */
	public World(WorldLogic<?> logic) {
		super();
		registerWorldLogic(logic, "AGENT");
		MAX_POPULATION = Integer.valueOf(SimProperties.getProperty("MaxAgentPopulation", "10000"));
		suffix = "dummy";
		this.setName(suffix);
		listeners = new ArrayList<ActionListener>();
		initialiseTemperature(new Temperature(0, 0));
	}

	public World(ActionProcessor ap, String suffix, long end, WorldLogic<?> logic) {
		this(logic);
		this.setName(suffix);
		actionProcessor = ap;
		if (ap != null)
			ap.setWorld(this);
		if (suffix == null) suffix = "001";
		this.suffix = suffix;
		endOfWorldTime = end;
		// set temperature
		initialiseTemperature(null);
	}

	public void initialiseTemperature(Temperature t) {
		if (t == null) {
			double startTemp = SimProperties.getPropertyAsDouble("StartTemperature", "1.0");
			double endTemp = SimProperties.getPropertyAsDouble("EndTemperature", "0.0");
			temperature = new Temperature(startTemp, endTemp);
		} else {
			temperature = t;
		}
	}

	public LocationMap getLocationMap(){
		return locationMap;
	}

	public void setLocationMap(LocationMap newLocationMap) {
		locationMap = newLocationMap;
		locationMap.registerWorld(this);
	}

	@Override
	public synchronized void maintenance() {
		int count = 0;
		while (databaseUtility != null && databaseUtility.isCongested()) {
			try {
				count++;
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (count > 0)
			System.out.println("Waited for " + count +" seconds for DBU to catch up.");

		super.maintenance();

		temperature.setTime((double)getCurrentTime() / endOfWorldTime);
		double newTemp = temperature.getTemperature();
		SimProperties.setProperty("Temperature", String.format("%.3f", newTemp));		
		if (lastTempPublished - newTemp >= 0.01) {
	//		logger.info("Setting Temperature to be " + String.format("%.3f", newTemp));
			lastTempPublished = newTemp;
		}

		int population = agentsInLocation.size();
		if (population > MAX_POPULATION) {
			logger.info("Population = " + population);
			for (int n = population; n>MAX_POPULATION; n--) {
				//pick an Agent at random and terminate them
				int chosen = (int) (Math.random()*n);
				if (agentsInLocation.get(chosen).getGeneration()<100) {
					logger.info("Culled " + agentsInLocation.get(chosen).toString());
					agentsInLocation.get(chosen).markAsCulled();
				}
			}
			logger.info("Culled " + (population - MAX_POPULATION));
		}
	}

	@Override
	public String toString() {
		return suffix;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		suffix = name;
	}

	@SuppressWarnings("rawtypes")
	public boolean addAction(Action a) {
		if (actionProcessor !=null && a !=null) {
			actionProcessor.add(a);
			return true;
		}
		return false;
	}

	@Override
	public Market getMarket() {
		return market;
	}

	public void setMarket(Market m) {
		if (market == null) market = m;
	}

	public void initialiseMarket() {
		String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
		market = new Market(new File(baseDir + "\\logs\\Market_"+suffix+".txt"), this);
	}

	public ActionProcessor getActionProcessor() {
		return actionProcessor;
	}

	public void setActionProcessor(ActionProcessor actionProcessor) {
		this.actionProcessor = actionProcessor;
		if (actionProcessor != null)
			actionProcessor.setWorld(this);
	}

	public Long getCurrentTime() {
		if (calendar != null)
			return calendar.getTime();
		if (isDead())
			return actualEndOfWorldTime;
		return 0l;
	}

	public void setCurrentTime(Long newTime) {
//		System.out.println("Setting time to " + newTime);
		if (calendar != null)
			calendar.setTime(newTime);
	}

	public void setScheduledTask(TimerTask task, long delay, long period) {
		if (calendar != null)
			calendar.setScheduledTask(task, delay, period);
	}
	public void setScheduledTask(TimerTask task, long delay) {
		if (calendar != null)
			calendar.setScheduledTask(task, delay);
	}

	public void removeScheduledTask(TimerTask task) {
		if (calendar != null)
			calendar.removeScheduledTask(task);
	}

	public void addListener(ActionListener newListener) {
		listeners.add(newListener);
	}
	public void removeListener(ActionListener oldListener) {
		if (listeners.contains(oldListener))
			listeners.remove(oldListener);
	}
	public void worldDeath() {
		if (isDead()) return;
		isAlive = false;
		if (calendar != null)
			actualEndOfWorldTime = calendar.getTime();
		setCalendar(null);
		List<Agent> allAgents = new ArrayList<Agent>();
		for (Agent a : agentsInLocation)
			allAgents.add(a);
		for (Agent a : allAgents) 
			a.maintenance();
		for (ActionListener al : listeners) {
			al.actionPerformed(new ActionEvent(this, 1, "Death"));
		}
		listeners.clear();
		actionProcessor = null;
		maintenance(); // will clean up
	}
	public boolean isDead() {
		return !isAlive;
	}

	public void recordAction(Action action) {
		if (actionRecorder != null)
			actionRecorder.recordAction(action);
	}

	public void setCalendar(WorldCalendar calendar, int maintenancePeriod) {
		if (this.calendar != null) {
			worldMaintenanceTask.cancel();
			this.calendar.removeScheduledTask(worldMaintenanceTask);
		}
		this.calendar = calendar;
		worldMaintenanceTask = new TimerTask() {
			@Override
			public void run() {
				maintenance();
			}
		};
		if (this.calendar != null && maintenancePeriod > 0)
			calendar.setScheduledTask(worldMaintenanceTask, maintenancePeriod, maintenancePeriod);
	}

	public void setCalendar(WorldCalendar calendar) {
		setCalendar(calendar, 1000);
	}

	public void setDatabaseAccessUtility(DatabaseAccessUtility databaseUtility) {
		this.databaseUtility = databaseUtility;
		actionRecorder = new RecordActions(this);
	}
	public void updateDatabase(String sql) {
		if (databaseUtility != null) {
			databaseUtility.addUpdate(sql);
		} else {
			logger.severe("Attempt to write to database with null utility. " + sql);
		}
	}

	public String getCurrentDate() {
		if (calendar == null) 
			return "Unknown";
		return calendar.getDate();
	}

	public int getYear() {
		return (int) (getCurrentTime() / 52.0);
	}

	public int getSeason() {
		return (int) (getCurrentTime() % 52) / 13;
	}

	public void setEndOfWorldTime(long worldEnds) {
		endOfWorldTime = worldEnds;
	}
	
	@Override
	public World getWorld() {
		return this;
	}
	public <A extends Agent> void registerWorldLogic(WorldLogic<A> logic, String forAgentType) {
		logicMap.put(forAgentType, logic);
	}
	
	@SuppressWarnings("unchecked")
	public <A extends Agent> WorldLogic<A> getWorldLogic(A agent) {
		String key = agent.getType();
		return (WorldLogic<A>) logicMap.get(key);
	}
}
