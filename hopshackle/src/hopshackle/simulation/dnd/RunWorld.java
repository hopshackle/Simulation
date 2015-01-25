package hopshackle.simulation.dnd;

import hopshackle.GUI.*;
import hopshackle.simulation.*;
import hopshackle.simulation.Action;
import hopshackle.simulation.dnd.actions.Equip;
import hopshackle.simulation.dnd.genetics.*;

import java.awt.Dimension;
import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;

public class RunWorld {

	private World w;
	private Location defaultStartLocation;
	private JFrame frame;
	private String suffix;
	Thread marketThread, expRecordThread, clrRecordThread, ftrRecordThread, populationThread, partyRecordThread;
	private int freq = 5000;
	private int maxInc = 25;
	private long endTime;
	private DndPopulationSpawner populationSpawner;
	private GeneRecorder expRecorder, ftrRecorder, clrRecorder;
	private PartyRecorder partyRecorder;
	private Logger simlog;
	private FileHandler fh;
	private List<NeuralDecider> ftrCombatBrain, clrCombatBrain;
	private List<CareerNNDecider> careerBrain;
	private List<Genome> ftrGenomes, clrGenomes, expGenomes;
	private Location tradeVille;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	public RunWorld(World w1, 	ArrayList<Genome> genomeList, 
			ArrayList<CharacterClass> classList,
			ArrayList<Integer> numberList, boolean showGUI, int runTime) {
		if (genomeList == null) genomeList = new ArrayList<Genome>();
		if (classList == null) classList = new ArrayList<CharacterClass>();
		if (numberList == null) numberList = new ArrayList<Integer>();
		endTime = runTime*60000;

		simlog = Logger.getLogger("hopshackle.simulation");
		try {
			String fileName = "/Simulations/logs/RunWorld_" + w1.toString() + "_" + 
			String.format("%tY%<tm%<td_%<tH%<tM.log", Calendar.getInstance());

			fh = new FileHandler(fileName);
			fh.setFormatter(new SimpleFormatter());
			simlog.addHandler(fh);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		w = w1;
		suffix = w.toString();
		tradeVille = new Location(w);
		tradeVille.setName("TradeVille");

		LocationMap tempMap = w.getLocationMap();
		if (tempMap instanceof SquareMap) {
			defaultStartLocation = ((SquareMap)tempMap).getSquareAt(0, 0);
		} else {
			throw new AssertionError("no default start location defined for this map type");
		}
		
		/*
		 *  First of all we clear state space, unless we are inheriting it from run to run
		 *   Second we load in the correct state space for the next run
		 */

		boolean inheritStateSpace = "true".equals(SimProperties.getProperty("InheritStateSpace", "false"));
		boolean loadStateSpace = "true".equals(SimProperties.getProperty("LoadStateSpaceAtStart", "false"));
		String stateSpaceToLoad = SimProperties.getProperty("StateSpaceToLoad", "");

		if (!inheritStateSpace)
			HopshackleState.clear();

		if (loadStateSpace)
			HopshackleState.loadStates(stateSpaceToLoad);
		if (showGUI)
			createAndShowGUI();

		w.getActionProcessor().start();
		// at this point the main action thread starts polling for Actions

		startSimulation();

		Character death = new Character(w);
		try {
			synchronized (this) {
				this.wait(5000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// we then wait five seconds after initialisation to ensure that 
		// we have kicked off, before setting up the jobs on the queue that will
		// bring the run to an end

		w.setScheduledTask(new StopWorld(), endTime);
		// this stops the population spawner to let population die off

		Action worldDeath = new WorldDeath(death, endTime+1000000);
		// and this actually kills the world, and informs everybody who's interested
		// we have to wait to make sure it doesn't get picked up too quickly
		w.addAction(worldDeath);

	}

	private void createAndShowGUI() {
		//Create and set up the window.
		frame = new JFrame(suffix);

		DnDWorldMap worldMap = new DnDWorldMap(w);
		worldMap.setPreferredSize(new Dimension(500,250));

		JTabbedPane mainTabbedPane = new JTabbedPane();

		AgentGrid agentPanel = new AgentGrid(w, new CharacterTableModel(), null);
		PartyGrid partyPanel = new PartyGrid(w, new PartyTableModel());
		MarketGrid marketPanel = new MarketGrid(new MarketTableModel(w.getMarket()));
		JPanel populationPanel = new PopulationGraph(w, endTime);

		JScrollPane agentScrollPane = new JScrollPane(agentPanel);
		JScrollPane partyScrollPane = new JScrollPane(partyPanel);
		JScrollPane marketScrollPane = new JScrollPane(marketPanel);
		JScrollPane populationScrollPane = new JScrollPane(populationPanel);

		agentScrollPane.setPreferredSize(new Dimension(250,250));
		agentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		mainTabbedPane.add("Characters", agentScrollPane);
		mainTabbedPane.add("Parties", partyScrollPane);
		mainTabbedPane.add("Market", marketScrollPane);
		mainTabbedPane.add("Population", populationScrollPane);

		JSplitPane worldPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		worldPane.add(worldMap);
		worldPane.add(mainTabbedPane);
		frame.getContentPane().add(worldPane);

		//Display the window.
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void startSimulation() {
		marketThread = new Thread(w.getMarket(), "Market Thread"); 
		marketThread.start();

		initiallyPopulate();

		freq = (int) SimProperties.getPropertyAsDouble("FrequencyOfPopulationSpawnerInMilliseconds", "1000");
		maxInc = (int) SimProperties.getPropertyAsDouble("MaxPopulationIncrement", "10");

		populationSpawner = new DndPopulationSpawner(w, freq, maxInc);
		populationThread = new Thread(populationSpawner, "Population Spawner");
		populationThread.start();

		ftrRecorder = new GeneRecorder(new File(baseDir + "\\logs\\Ftr_"+suffix+".txt"), 
				CharacterClass.FIGHTER);
		clrRecorder = new GeneRecorder(new File(baseDir + "\\logs\\Clr_"+suffix+".txt"), 
				CharacterClass.CLERIC);
		expRecorder = new GeneRecorder(new File(baseDir + "\\logs\\Exp_"+suffix+".txt"), 
				CharacterClass.EXPERT);
		partyRecorder = new PartyRecorder(new File(baseDir + "\\logs\\Party_"+suffix+".txt"));


		ftrRecordThread = new Thread(new AgentRecorder(w, 60000, ftrRecorder), "Fighter Recording Thread");
		ftrRecordThread.start();
		clrRecordThread = new Thread(new AgentRecorder(w, 60000, clrRecorder), "Cleric Recording Thread");
		clrRecordThread.start();
		expRecordThread = new Thread(new AgentRecorder(w, 60000, expRecorder), "Expert Recording Thread");
		expRecordThread.start();
		partyRecordThread = new Thread(new AgentRecorder(w, 60000, partyRecorder), "Party Recording Thread");
		partyRecordThread.start();
	}

	private void initiallyPopulate() {		

		final String careerDeciderType = SimProperties.getProperty("CareerDeciderType", "Neural");
		final String combatDeciderType = SimProperties.getProperty("CombatDeciderType", "Neural");

		StateDecider clrDecider = new StateOffPolicyDecider(
				new ArrayList<ActionEnum>(EnumSet.allOf(BasicActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class)));
		clrDecider.setStateType("CLR");
		clrDecider.setPigeonHoles(5);
		StateDecider ftrDecider = new StateOffPolicyDecider(
				new ArrayList<ActionEnum>(EnumSet.allOf(BasicActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(GeneticEnum.class)));	
		ftrDecider.setStateType("FTR");
		ftrDecider.setPigeonHoles(5);
		StateDecider expDecider = new StateOffPolicyDecider(
				new ArrayList<ActionEnum>(EnumSet.allOf(ExpertActionsI.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(ExpertGeneticEnum.class)));
		expDecider.setStateType("EXP");
		expDecider.setPigeonHoles(5);

		CharacterClass.CLERIC.setDefaultDecider(clrDecider);
		CharacterClass.FIGHTER.setDefaultDecider(ftrDecider);
		CharacterClass.EXPERT.setDefaultDecider(expDecider);
		CharacterClass.WARRIOR.setDefaultDecider(new HardCodedDecider(BasicActionsI.STAY));

		clrDecider.setTeacher(new AgentTeacher());
		ftrDecider.setTeacher(new AgentTeacher());
		expDecider.setTeacher(new AgentTeacher());

		CharacterClass.CLERIC.setDefaultTradeDecider(new TradeDecider());
		CharacterClass.FIGHTER.setDefaultTradeDecider(new TradeDecider());
		CharacterClass.EXPERT.setDefaultTradeDecider(new TradeDeciderExp());


		File brainLocation = new File(baseDir + "\\Brains\\Start");
		File genomeLocation = new File(baseDir + "\\Genomes\\Start");


		if (combatDeciderType.equals("Neural")) {
			NeuralDecider clrDefaultNeuralCombatDecider = new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
					new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputs.class)));
			clrDefaultNeuralCombatDecider.setTeacher(new AgentTeacher());
			clrDefaultNeuralCombatDecider.setName("CLR");
			CharacterClass.CLERIC.setDefaultCombatDecider(clrDefaultNeuralCombatDecider);

			NeuralDecider ftrDefaultNeuralCombatDecider = new NeuralDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
					new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputs.class)));
			ftrDefaultNeuralCombatDecider.setTeacher(new AgentTeacher());
			ftrDefaultNeuralCombatDecider.setName("FTR");
			CharacterClass.FIGHTER.setDefaultCombatDecider(ftrDefaultNeuralCombatDecider);

			ftrCombatBrain = LoadBrains.loadNN(brainLocation, LoadBrains.createBrainFilter("FTR"), w);
			for (NeuralDecider nd : ftrCombatBrain) {
				nd.setName("FTR");
				nd.setTeacher(new AgentTeacher());
			}

			clrCombatBrain = LoadBrains.loadNN(brainLocation, LoadBrains.createBrainFilter("CLR"), w);
			for (NeuralDecider nd : clrCombatBrain) {
				nd.setName("CLR");
				nd.setTeacher(new AgentTeacher());
			}
		} else {
			final StateDecider ftrCombatStateDecider = new StateOffPolicyDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
					new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputs.class)));
			final StateDecider clrCombatStateDecider = new StateOffPolicyDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CombatActionsI.class)),
					new ArrayList<GeneticVariable>(EnumSet.allOf(CombatNeuronInputs.class)));
			ftrCombatStateDecider.setStateType("FTRCBT");
			clrCombatStateDecider.setStateType("CLRCBT");
			ftrCombatStateDecider.setPigeonHoles(5);
			clrCombatStateDecider.setPigeonHoles(5);
			ftrCombatStateDecider.setBaseValue(1000);
			clrCombatStateDecider.setBaseValue(1000);

			CharacterClass.FIGHTER.setDefaultCombatDecider(ftrCombatStateDecider);
			CharacterClass.CLERIC.setDefaultCombatDecider(clrCombatStateDecider);
		}

		BaseDecider nonFinalDefaultCareerDecider = null;		
		if (careerDeciderType.equals("Neural")) {
			ArrayList<NeuralDecider> tempArray = LoadBrains.loadNN(brainLocation, LoadBrains.createBrainFilter("CAREER"), w);
			careerBrain = new ArrayList<CareerNNDecider>();
			for (NeuralDecider nd : tempArray) {
				nd.setName("CAREER");
				CareerNNDecider careerNNDecider = CareerNNDecider.CareerNNDeciderFactory(nd);
				careerNNDecider.setTeacher(new CareerTeacher());
				careerBrain.add(careerNNDecider);
			}
			nonFinalDefaultCareerDecider = new CareerNNDecider();
			nonFinalDefaultCareerDecider.setTeacher(new CareerTeacher());
		} else {
			StateDecider careerStateDecider = new StateOffPolicyDecider(new ArrayList<ActionEnum>(EnumSet.allOf(CareerActionsI.class)), 
					new ArrayList<GeneticVariable>(EnumSet.allOf(CareerGeneticEnum.class)));
			careerStateDecider.setBaseValue(10000);
			careerStateDecider.setPigeonHoles(5);
			careerStateDecider.setStateType("CAREER");
			careerStateDecider.setTeacher(new CareerTeacher());
			nonFinalDefaultCareerDecider = careerStateDecider;
		}
		final BaseDecider defaultCareerDecider = nonFinalDefaultCareerDecider;

		ftrGenomes = Genome.loadGenomes(genomeLocation, Genome.createGenomeFilter("FTR"));
		clrGenomes = Genome.loadGenomes(genomeLocation, Genome.createGenomeFilter("CLR"));
		expGenomes = Genome.loadGenomes(genomeLocation, Genome.createGenomeFilter("EXP"));


		class AddPopulation extends TimerTask {
			public void run() {

				int totalPopPerRun = 10;
				// i.e. we use the same one for the whole run of 10
				for (int n = 0; n < totalPopPerRun; n++) {
					Character c = null;
					Genome genomeToUse = null;
					
					BaseDecider careerDecider = defaultCareerDecider;
					if (careerDeciderType.equals("Neural") && careerBrain.size() > 0 ) {
						// rather than hard-code the types to be added, we use an available careerBrain
						careerDecider = careerBrain.get(Dice.roll(1, careerBrain.size())-1);
					} 

					c = new Character(Race.HUMAN, careerDecider, null, null, w);

					// now we need to ensure a correct genome is used
					List<Genome> tempArr = null;
					switch (c.getChrClass()) {
					case EXPERT:
						tempArr = expGenomes;
						break;
					case FIGHTER:
						tempArr = ftrGenomes;
						break;
					case CLERIC:
						tempArr = clrGenomes;
						break;
					}	
					genomeToUse = tempArr.get(Dice.roll(1, tempArr.size())-1);

					if (genomeToUse != null)
						c.setGenome(genomeToUse);
					c.setGeneration(1);
					c.addGold(30);

					Action firstAction = null;
					c.start();
					switch (c.getChrClass()) {
					case EXPERT:
						c.setLocation(tradeVille);
						firstAction = c.decide();
						break;
					case FIGHTER:
						// If no brains loaded, then this will default to the CharacterClass standard
						if (combatDeciderType.equals("Neural")) {
							if (ftrCombatBrain.size() > 0) {
								c.setCombatDecider(ftrCombatBrain.get(Dice.roll(1, ftrCombatBrain.size())-1));
							}  
						}
						// Else Will pick up the characterClass default
						c.setLocation(defaultStartLocation);
						firstAction = (new Equip(c, 4000));
						break;
					case CLERIC:
						if (combatDeciderType.equals("Neural")) { 
							if (clrCombatBrain.size() > 0) {
								c.setCombatDecider(clrCombatBrain.get(Dice.roll(1, clrCombatBrain.size())-1));
							} 
						}
						// else we pick up characterClass defaults
						c.setLocation(defaultStartLocation);
						firstAction = (new Equip(c, 4000));
						break;
					}

					w.getActionProcessor().add(firstAction);
				}
			} 
		}

		// We want to put 600 starting characters in position
		// at the rate of 10 per second for the first minute
		new AddPopulation().run();

		for (int n = 0; n < 59; n++) 
			w.setScheduledTask(new AddPopulation(), 1000 * n);

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

	class WorldDeath extends Action {
		private Logger simLog = Logger.getLogger("hopshackle.simulation");
		WorldDeath(Character d, long st) {
			super(d, st, false);
		}
		public String toString(){
			if (w!=null) return w.toString();
			return "NULL";
		}
		public void doStuff() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			simLog.info("Starting WorldDeath: " + w.toString());
			if (frame != null)
				frame.dispose();

			HopshackleState.recordStates(w.toString());
			simlog.removeHandler(fh);
			w.worldDeath();
			simlog = null;
			fh.close();
			fh = null;
			w = null;
		}
		public void doNextDecision() {}
	}
}
