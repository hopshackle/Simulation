package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.*;
import java.util.logging.Logger;
/**
 * Sleeps for period, and then wakes up, and generates
 * x new Characters (MIN - currentPop) + 20
 * 
 **/
public class BasicPopulationSpawner implements Runnable{

	private World world;
	private BasicRunWorld parent;
	private Location defaultStartLocation = null;
	private int maxIncrement, minimumWorldPopulation;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private Object key;
	private TimerTask newTask;
	boolean done;

	public BasicPopulationSpawner(BasicRunWorld brw, long freq, int maxIncrement, int minimumWorldPopulation) {
		world = brw.w;
		parent = brw;
		LocationMap worldMap = world.getLocationMap();
		if (worldMap instanceof HexMap) {
			defaultStartLocation = ((HexMap)worldMap).getHexAt(0, 0);
		} else {
			throw new AssertionError("No default start location defined for that map type");
		}

		done = false;
		key = new Object();
		newTask = new TimerTask() {
			public void run() {
				synchronized (key) {
					key.notify();
				}
			}
		};
		world.setScheduledTask(newTask, freq, freq);
		this.maxIncrement = maxIncrement;
		this.minimumWorldPopulation = minimumWorldPopulation;
	}

	public void run() {

		try {
			List<BasicAgent> allAgents = new ArrayList<BasicAgent>();
			List<Agent> tempArr = null;

			do synchronized (key) {

				allAgents.clear();
				tempArr = null;

				try {
					key.wait();
				} catch (InterruptedException e) {
					logger.info("Population Spawner has been interrupted");
				}
	//			logger.info("Population Spawner started");

				tempArr = world.getAgents();
				for (Agent a : tempArr) {
					if (a instanceof BasicAgent) {
						BasicAgent c = (BasicAgent) a;
						allAgents.add(c);
					}
				}

				int agentsToAdd = maxIncrement;
				if (allAgents.size() < minimumWorldPopulation) {

					Long delay = world.getActionProcessor().getDelay();
					if (delay != null && delay > 100) {
						agentsToAdd = (int)(((double)agentsToAdd) * ((10000.0 - ((double)delay))/10000.0));
						logger.info("Throttled number of agents to add: " + agentsToAdd);
					}

					for (int loop = 0; loop<agentsToAdd; loop++) {
						BasicAgent newC = new BasicAgent(world);
						newC.setLocation(defaultStartLocation);
						parent.ercPolicy.apply(newC);
						newC.setGeneration(0);

						Action<BasicAgent> firstAction = BasicActions.REST.getAction(newC);
						firstAction.addToAllPlans();
					}
				}
			} while (!done);

			logger.info("Population Spawner exiting");

		} catch (Exception e) {
			logger.info("Population Spawner has crashed: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		}


	}

	public synchronized void setDone() {
		world.removeScheduledTask(newTask);
		done = true;
	}



}

