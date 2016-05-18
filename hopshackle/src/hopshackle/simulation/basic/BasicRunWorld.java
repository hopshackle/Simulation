package hopshackle.simulation.basic;

import hopshackle.GUI.*;
import hopshackle.simulation.*;
import hopshackle.simulation.Action;

import java.io.IOException;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;

public class BasicRunWorld {

	private World w;
	private Location defaultStartLocation;
	private JFrame frame;
	private String suffix;
	private Logger simlog;
	private FileHandler fh;
	private long endTime;
	private boolean showGUI = true;
	private SimpleAnimationPanel worldMapDisplay;
	private Thread populationThread;
	private BasicPopulationSpawner populationSpawner;
	private AgentRecorder agentRecorder;
	private Thread recordingThread;
	private int freq, maxInc;

	public BasicRunWorld(World w1, boolean showGUI, long runTime) {
		simlog = Logger.getLogger("hopshackle.simulation");
		try {
			String fileName = SimProperties.getProperty("BaseDirectory", "C:/Simulations/logs") + "/RunWorld_" + w1.toString() + "_" + 
					String.format("%tY%<tm%<td_%<tH%<tM.log", Calendar.getInstance());

			fh = new FileHandler(fileName);
			fh.setFormatter(new SimpleFormatter());
			simlog.addHandler(fh);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Starting BasicRunWorld");

		this.showGUI = showGUI;
		endTime = runTime;
		w = w1;
		suffix = w.toString();

		LocationMap tempMap = w.getLocationMap();
		if (tempMap instanceof HexMap) {
			defaultStartLocation = ((HexMap)tempMap).getHexAt(0, 0);
			worldMapDisplay = new WorldHexMap(w);
		} else {
			throw new AssertionError("Must have a World with a LocationMap");
		}

		boolean inheritStateSpace = "true".equals(SimProperties.getProperty("InheritStateSpace", "false"));
		boolean loadStateSpace = "true".equals(SimProperties.getProperty("LoadStateSpaceAtStart", "false"));
		String stateSpaceToLoad = SimProperties.getProperty("StateSpaceToLoad", "");

		if (!inheritStateSpace)
			HopshackleState.clear();

		if (loadStateSpace)
			HopshackleState.loadStates(stateSpaceToLoad);

		if (this.showGUI)
			createAndShowGUI();

		w.getActionProcessor().start();

		startSimulation();

		BasicAgent death = new BasicAgent(w);
		try {
			synchronized (this) {
				this.wait(1000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		simlog.info("End Time is " + endTime);
		w.setScheduledTask(new StopWorld(), endTime);
		// this stops the population spawner to let population die off

		long maxLife = death.getMaxAge();
		simlog.info("Max Life is " + maxLife);
		w.setScheduledTask(new TimerTask() {

			@Override
			public void run() {
				simlog.info("Starting WorldDeath: " + w.toString());
				if (frame != null)
					frame.dispose();

				HopshackleState.recordStates(w.toString());
				simlog.removeHandler(fh);
				w.worldDeath();
				simlog = null;
				if (fh!= null)
					fh.close();
				fh = null;
				w = null;	
			}
		}, endTime + maxLife);
	}

	private void createAndShowGUI() {
		//Create and set up the window.
		frame = new JFrame(suffix);

		JScrollPane mapPane = new JScrollPane(worldMapDisplay);
		frame.getContentPane().add(mapPane);

		//Display the window.
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void startSimulation() {
		initiallyPopulate();
		freq = (int) SimProperties.getPropertyAsDouble("FrequencyOfPopulationSpawnerInMilliseconds", "1000");
		maxInc = (int) SimProperties.getPropertyAsDouble("MaxPopulationIncrement", "10");
		int minPop = (int) SimProperties.getPropertyAsDouble("MinimumWorldPopulation", "100");

		populationSpawner = new BasicPopulationSpawner(w, freq, maxInc, minPop);
		populationThread = new Thread(populationSpawner, "Population Spawner");
		populationThread.start();
		System.out.println("Started PopulationSpawner");

		agentRecorder = new AgentRecorder(w, 60000, new BasicAgentRecorder(w.toString()));
		recordingThread = new Thread(agentRecorder, "BasicAgent recorder");
		recordingThread.start();
	}

	private void initiallyPopulate() {		

		ArrayList<GeneticVariable> variablesToUse = new ArrayList<GeneticVariable>(EnumSet.allOf(BasicVariables.class));
		variablesToUse.remove(BasicVariables.WATER);
	//	variablesToUse.remove(BasicVariables.GENDER);
	//	variablesToUse.remove(BasicVariables.MARRIED_STATUS);

		ArrayList<ActionEnum<BasicAgent>> actionsToUse = new ArrayList<ActionEnum<BasicAgent>>(EnumSet.allOf(BasicActions.class));
		actionsToUse.remove(BasicActions.FIND_WATER);

		//		final StateDecider basicDecider = new StateOffPolicyDecider(
		//				actionsToUse,
		//				variablesToUse);
		//		basicDecider.setStateType("BASIC");
		//		basicDecider.setPigeonHoles(7);

		final GeneralLinearQDecider<BasicAgent> basicDecider = new GeneralLinearQDecider<BasicAgent>(actionsToUse, variablesToUse);

		basicDecider.setTeacher(new AgentTeacher<BasicAgent>());

		class AddPopulation extends TimerTask {
			public void run() {
				int totalPopPerRun = 1;
				for (int n = 0; n < totalPopPerRun; n++) {
					BasicAgent b = null;
					b = new BasicAgent(w);
					b.setLocation(defaultStartLocation);
					b.setDecider(basicDecider);
					b.updatePlan();
				}
			} 
		}
		new AddPopulation().run();

		for (int n = 0; n < 59; n++) 
			w.setScheduledTask(new AddPopulation(), 1000 * n);

		w.setScheduledTask(new TimerTask() {

			@Override
			public void run() {
				double largestWeight = basicDecider.getLargestWeight();
				for (ActionEnum<BasicAgent> ae : basicDecider.getActions()) {
					System.out.println(ae + ":");
					for (GeneticVariable gv1 : basicDecider.getVariables()) {
							double arr = basicDecider.getWeightOf(gv1, ae);
							if (Math.abs(arr) > largestWeight / 100.0)
								System.out.println(String.format("\t%-15s\t\t%.3f", gv1.toString(), arr));
						}
				}
			}
		}, 0, 1000 * 60);

	}

	class StopWorld extends TimerTask {
		private Logger simLog = Logger.getLogger("hopshackle.simulation");
		public StopWorld() {
		}
		public void run() {
			// stage I is to stop spawning new population
			simLog.info("Sending Stop message to Population Spawner");
			populationSpawner.setDone();
		}
	}
}
