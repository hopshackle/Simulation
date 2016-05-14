package hopshackle.simulation;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ActionProcessor {

	public static String newline = System.getProperty("line.separator");
	private BlockingQueue<Action> q;
	private Action currentAction;
	private Thread t;
	private World world;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private Long delay, delayCount, originalStartTime, lastRecordedTime;
	private Hashtable<String, Long> actionTimes;
	private Hashtable<String, Integer> actionCount;
	private String logFile;
	private boolean debug = true;
	private boolean delayQueue, done;

	public ActionProcessor() {
		this("", true);
	}
	public ActionProcessor(String suffix, Boolean realtime) {
		delayQueue = realtime;
		done = false;
		if (delayQueue) {
			q = new DelayQueue<Action>();
		} else {
			q = new PriorityBlockingQueue<Action>();
		}

		actionTimes = new Hashtable<String, Long>();
		actionCount = new Hashtable<String, Integer>();
		lastRecordedTime = 0L;
		delayCount = 0l;
		String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
		logFile = baseDir + "\\logs\\ActionProcessorRecord_" + suffix + ".log";
		t = new Thread(new ActionThread(this), "Action Thread");
		originalStartTime = 0L;
	}

	public void start() {
		t.start();
	}
	public void stop() {
		done = true;
	}

	public void add(Action a) {
		if (debug) logger.info("Adding action " + a);
		if (a == null) {
			logger.severe("Null action sent to ActionProcessor");
			return;
		}
		try {
			if (!q.offer(a, 2, TimeUnit.SECONDS)) {
				logger.severe("Action dropped as queue is blocked ");
			}
		} catch (InterruptedException e) {
			logger.severe("Action Processor add function: " + e.toString());
			e.printStackTrace();
		}
	}

	/*
	 * Useful for unit testing only by allowing actions to be pulled from 
	 * queue in a controlled fashion (and executed off it)
	 */
	public Action getNextUndeletedAction(long wait1, TimeUnit wait2) {
		try {
			do {
				currentAction = q.poll(wait1, wait2);
			} while (!validAction(currentAction));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// If we ever have to wait for more than wait period, then exit

		return currentAction;
	}
	private boolean validAction(Action currentAction2) {
		if (currentAction2 == null)
			return false;
		switch (currentAction2.getState()) {
		case FINISHED:
		case CANCELLED:
			return false;
		case PROPOSED:
		case PLANNED:
		case EXECUTING:
			return true;
		default: 
			return false;
		}
	}
	// Only used in testing?
	public Action getNextUndeletedAction() {
		return getNextUndeletedAction(1, TimeUnit.MILLISECONDS);
	}

	class ActionThread implements Runnable {
		ActionProcessor ap;
		public ActionThread(ActionProcessor parent) {
			ap = parent;
		}
		public void run() {
			Long startTime, endTime;
			try {
				do while (!done) { 
					currentAction = q.poll(10, TimeUnit.SECONDS);
					// If we ever have to wait for more than 10 seconds, then exit
					if (currentAction != null) {

						synchronized (ap) {
							if (debug) logger.info("started action: " + currentAction.toString());
							startTime = System.currentTimeMillis();
							
							switch (currentAction.getState()) {
							case PROPOSED:
								currentAction.cancel();	// To late
							case FINISHED:
							case CANCELLED:
								continue;
							case PLANNED:
								updateWorldTime(currentAction.getStartTime());
								currentAction.start();
								add(currentAction);		// This will queue up the actual action execution
								break;
							case EXECUTING:
								updateWorldTime(currentAction.getEndTime());
								currentAction.run();
								break;
							}
							endTime = System.currentTimeMillis();
							if (debug) {
								logger.info("processed action: " + currentAction.toString() + " to state of " + currentAction.getState() + 
										" at world time " + world.getCurrentTime());
								if (currentAction.getState() == Action.State.FINISHED)
									recordAction(currentAction.toString(), endTime - startTime);
							}
							if ((endTime - startTime) > 1000) {
								logger.warning(currentAction.toString() + " takes " + (endTime-startTime) + " ms");
							}
							ap.notifyAll();
						}
						
					} else {done = true;}

				} while (!done);

				logger.info("Action Processor exiting with time-out");
			} catch (InterruptedException ex) {
				logger.severe("Action Processor: " + ex.toString());
				ex.printStackTrace();
			}
		}
	}
	
	private void updateWorldTime(long timeAtCompletion) {
		long worldStartTime = world.getCurrentTime();
		if (delayQueue) {
			delay = worldStartTime - timeAtCompletion;
			if (delay > 500 && delayCount < delay) {
				logger.warning("Delay is " + delay + " ms");
				// Delay only makes sense in real-time mode
			}
			delayCount = Math.max(delay, delayCount);
			delayCount -= 5;
		} else {
			if (world != null) {
				if (world.getCurrentTime() < timeAtCompletion) {
					world.setCurrentTime(timeAtCompletion);
			//		if (debug) logger.info("Setting world time to " + timeAtCompletion);
				}
			}
		}
	}

	public Long getDelay() {
		return delay;
	}
	private void recordAction(String s, Long t) {
		if (s==null) return;
		Long i = 0l;
		int count = 0;
		if (actionTimes.containsKey(s)) {
			i = actionTimes.get(s);
			count = actionCount.get(s);
		}
		count++;
		i+=t;
		actionTimes.put(s, i);
		actionCount.put(s, count);

		if (System.currentTimeMillis() - lastRecordedTime > 60000) {
			int minute = (int)((System.currentTimeMillis() - originalStartTime)/60000);
			lastRecordedTime = System.currentTimeMillis();
			try {
				FileWriter logWriter = new FileWriter(logFile, true);
				String logData;

				for(String temp : actionTimes.keySet()) {
					logData = minute + ", " + 
					temp + ", " + actionTimes.get(temp) +
					", " + actionCount.get(temp) + 
					", " + actionTimes.get(temp)/actionCount.get(temp);
					logWriter.write(logData+newline);
				}
				logWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				logger.severe(e.toString());
			}

			actionCount.clear();
			actionTimes.clear();
		}
	}
	public World getWorld() {
		return world;
	}
	public void setWorld(World world) {
		this.world = world;
	}
}
