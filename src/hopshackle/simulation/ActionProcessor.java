package hopshackle.simulation;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ActionProcessor {

    public static String newline = System.getProperty("line.separator");
    protected static Logger logger = Logger.getLogger("hopshackle.simulation");

    private BlockingQueue<Action> q;
    private Action currentAction;
    private Thread t;
    private ActionThread actionThread;
    private World world;
    private Long delay, delayCount, originalStartTime, lastRecordedTime;
    private Hashtable<String, Long> actionTimes;
    private Hashtable<String, Integer> actionCount;
    private String logFile;
    private boolean debug = false;
    private boolean delayQueue, stepping;
    private Queue<Action> steppingBuffer = new PriorityQueue<>();

    public ActionProcessor(String suffix, Boolean realtime) {
        delayQueue = realtime;
        if (delayQueue) {
            q = new DelayQueue<>();
        } else {
            q = new PriorityBlockingQueue<>();
        }

        actionTimes = new Hashtable<String, Long>();
        actionCount = new Hashtable<String, Integer>();
        lastRecordedTime = 0L;
        delayCount = 0l;
        String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
        logFile = baseDir + "\\logs\\ActionProcessorRecord_" + suffix + ".log";
        actionThread = new ActionThread(this);
        t = new Thread(actionThread, "Action Thread");
        originalStartTime = 0L;
    }

    public void start() {
        t.start();
    }

    public void stop() {
        actionThread.stop();
    }

    public void add(Action a) {
        if (debug)
            logger.info("Adding action " + a + " (" + a.getState()
                    + ") with delay of " + a.getDelay(TimeUnit.MILLISECONDS));
        if (a == null) {
            logger.severe("Null action sent to ActionProcessor");
            return;
        }
        if (stepping) {
            steppingBuffer.add(a);
        } else {
            try {
                if (!q.offer(a, 2, TimeUnit.SECONDS)) {
                    logger.severe("Action dropped as queue is blocked ");
                }
            } catch (InterruptedException e) {
                logger.severe("Action Processor add function: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    /*
     * Useful for unit testing only by allowing actions to be pulled from queue
     * in a controlled fashion (and executed off it)
     */
    public Action getNextUndeletedAction(long wait1, TimeUnit wait2) {
        try {
            int count = 0;
            do {
                count++;
                currentAction = q.poll(wait1, wait2);
                //			System.out.println("AP processing " + currentAction + " : " + currentAction.getState());
            } while (!validAction(currentAction) && count < 10);
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

    public void setTestingMode(boolean state) {
        stepping = state;
    }

    public Action processNextAction() {
        if (stepping) {
            Action nextAction = steppingBuffer.poll();
            // System.out.println(nextAction + " : " + nextAction.getState());
            if (nextAction != null) {
                try {
                    if (!q.offer(nextAction, 2, TimeUnit.SECONDS)) {
                        logger.severe("Action dropped as queue is blocked ");
                    }
                } catch (InterruptedException e) {
                    logger.severe("Action Processor add function: "
                            + e.toString());
                    e.printStackTrace();
                }
                return nextAction;
            }
        }
        return null;
    }

    class ActionThread implements Runnable {
        ActionProcessor ap;
        private volatile boolean done;

        public ActionThread(ActionProcessor parent) {
            ap = parent;
        }

        public void stop() {
            done = true;
        }

        public void run() {
            Long startTime, endTime;
            try {
                int sameActionCount = 0;
                String lastActionName = "";
                do {
                    currentAction = q.poll(10, TimeUnit.SECONDS);
                    // If we ever have to wait for more than 10 seconds,
                    // then exit
                    if (currentAction != null && !done) {
                        if (currentAction.toString().equals(lastActionName)) {
                            sameActionCount++;
                        } else {
                            sameActionCount = 0;
                            lastActionName = currentAction.toString();
                        }
                        if (sameActionCount > 150 && currentAction.getState() == Action.State.CANCELLED) {
                            throw new AssertionError("150+ repetitions of action " + lastActionName);
                        }
      //                  System.out.println("AP processing " + currentAction + " : " + currentAction.getState());
                        synchronized (ap) {
                            if (debug)
                                logger.info("started action: " + currentAction.toString() + " in state of " + currentAction.getState()
                                        + " at " + world.getCurrentTime() + " with delay of " + currentAction.getDelay(TimeUnit.MILLISECONDS));
                            startTime = System.currentTimeMillis();

                            switch (currentAction.getState()) {
                                case PROPOSED:
                                    updateWorldTime(currentAction.getStartTime());
                                    currentAction.cancel(); // Too late
                                case FINISHED:
                                case CANCELLED:
       //                             System.out.println("Running " + currentAction + " in state " + currentAction.getState() + " for " + currentAction.getActor());
                                    currentAction.run();    // for clean up
                                    break;
                                case PLANNED:
                                    updateWorldTime(currentAction.getStartTime());
                                    // We have a slight race condition, in that as a result of moving the world time on
                                    // we may stop the ActionProcessor
                                    if (!done) currentAction.start();
                                    add(currentAction); // This will queue up the actual action execution
                                    break;
                                case EXECUTING:
                                    updateWorldTime(currentAction.getEndTime());
                                    if (!done) currentAction.run();
                                    break;
                            }
                            endTime = System.currentTimeMillis();
                            if (debug) {
                                logger.info("processed action: "
                                        + currentAction.toString()
                                        + " to state of "
                                        + currentAction.getState()
                                        + " at world time "
                                        + world.getCurrentTime());
                                if (currentAction.getState() == Action.State.FINISHED)
                                    recordAction(
                                            currentAction.toString(),
                                            endTime - startTime);
                            }
                            if ((endTime - startTime) > 60000) {
                                logger.warning(currentAction.toString()
                                        + " takes "
                                        + (endTime - startTime) / 1000 + " seconds");
                            }
                            ap.notifyAll();
                        }

                    } else {
                        ap.stop();
                    }

                } while (!done);

                logger.info("Action Processor exiting with time-out at "
                        + world.getCurrentTime());
                world.worldDeath();
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
                    //				System.out.println(String.format("Setting time from %d to %d", world.getCurrentTime(), timeAtCompletion));
                    world.setCurrentTime(timeAtCompletion);
                }
            }
        }
    }

    public Long getDelay() {
        return delay;
    }

    private void recordAction(String s, Long t) {
        if (s == null)
            return;
        Long i = 0l;
        int count = 0;
        if (actionTimes.containsKey(s)) {
            i = actionTimes.get(s);
            count = actionCount.get(s);
        }
        count++;
        i += t;
        actionTimes.put(s, i);
        actionCount.put(s, count);

        if (System.currentTimeMillis() - lastRecordedTime > 60000) {
            int minute = (int) ((System.currentTimeMillis() - originalStartTime) / 60000);
            lastRecordedTime = System.currentTimeMillis();
            try {
                FileWriter logWriter = new FileWriter(logFile, true);
                String logData;

                for (String temp : actionTimes.keySet()) {
                    logData = minute + ", " + temp + ", "
                            + actionTimes.get(temp) + ", "
                            + actionCount.get(temp) + ", "
                            + actionTimes.get(temp) / actionCount.get(temp);
                    logWriter.write(logData + newline);
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
