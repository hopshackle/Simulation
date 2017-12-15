package hopshackle.simulation;

import java.awt.event.*;
import java.util.*;
import java.util.logging.Logger;

public class AgentRecorder implements Runnable {

	private AgentDataExtractor dataExtractor;
	private World world;
	private long sampleFrequency;
	private Object key;
	boolean done = false;
	public static String newline = System.getProperty("line.separator");
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private int period;

	public AgentRecorder(World world, long sampleFreq, AgentDataExtractor extractor) {
		dataExtractor = extractor;	
		sampleFrequency = sampleFreq;
		this.world = world;

		key = new Object();
		period = 0;

		extractor.initialise();

		world.addListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				synchronized (key) {
					done = true;
					key.notify();
				}
			}
		});
	}

	public void run() {
		TimerTask newTask = new TimerTask() {
			public void run() {
				synchronized (key) {
					key.notify();
				}
			}
		};

		world.setScheduledTask(newTask, sampleFrequency, sampleFrequency);

		try {
			do synchronized (key) {
				try {
					key.wait();
				} catch (InterruptedException e) {}

	//			logger.info("Starting Agent Recorder: " + dataExtractor.toString());
				dataExtractor.initialiseLoopVariables();
				List<Agent> agentArray = world.getAgentsIncludingChildLocations();
				for (Agent a : agentArray) 
					dataExtractor.extractDataFrom(a);

				period++;
				dataExtractor.dataExtractFinishedForPeriod(period);

			} while (!done);
			
			dataExtractor.closeDown();
			
		} catch (Exception e) {
			logger.info("Agent Recorder " + dataExtractor.toString() + " has crashed: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		}
	}
}
