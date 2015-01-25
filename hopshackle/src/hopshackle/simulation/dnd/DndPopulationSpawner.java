package hopshackle.simulation.dnd;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.actions.Equip;
import hopshackle.simulation.dnd.genetics.CareerNNDecider;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class DndPopulationSpawner implements Runnable{

	/*	
	Creates TradeVille as a location in the World to put the experts into.
	then sleeps for period, and then wakes up, and generates
	x new Characters (MIN - currentPop) + 20

	Get all agents, and split them into arrays for:
	-	all Fighters
	-	all Experts
	-	all Characters

	Each one from Character.
	Pick a number of current Characters = sampleSize
	Pick the one with the highest XP.
	Use this as a base (for the future when they will have a genome for determining profession).
	Call the chooseClass function. Then set the CharacterClass based on the result.
	Now that we know the Class we pick all instances of this Character.
	Then we pick sampleSize randomly from the relevant array, 
	pick the top two (or top one if the first Character happened to be the right type).
	Cross their genomes – and give to new Character.
	Then set up with initial action.
	For Fighters this is Equip as now, having placed them in 0, 0.
	For Experts put them into TradeVille.
	 */
	private World world;
	private Location defaultStartLocation = null;
	private int maxIncrement;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	private Location tradeVille;
	private Object key;
	private TimerTask newTask;
	boolean done;
	private Genome[] genomesToKeep;
	private Decider[] brainsToKeep;
	private Double[] highBreedingScore;
	private long startTime, worldAge;
	private CareerNNDecider defaultCareerDecider;
	private Decider careerDeciderToKeep;
	private ScoringFunction breedingScore = new ScoreByXP();

	public DndPopulationSpawner(World w, long freq, int maxIncrement) {
		world = w;
		LocationMap worldMap = w.getLocationMap();
		if (worldMap instanceof SquareMap) {
			defaultStartLocation = ((SquareMap)worldMap).getSquareAt(0, 0);
		} else {
			throw new AssertionError("No default start location defined for that map type");
		}
		
		startTime = world.getCurrentTime();
		done = false;
		key = new Object();
		newTask = new TimerTask() {
			public void run() {
				synchronized (key) {
					key.notify();
					//				logger.info("Waking up Population Spawner");
				}
			}
		};
		w.setScheduledTask(newTask, freq, freq);
		tradeVille = new Location(w);
		tradeVille.setName("TradeVille");
		this.maxIncrement = maxIncrement;
		genomesToKeep = new Genome[3];
		brainsToKeep = new Decider[2];
		highBreedingScore = new Double[3];
		for (int n = 0; n<3; n++) {
			highBreedingScore[n] = 0.0;
		}

		genomesToKeep[0] = new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_FTR.txt"));
		genomesToKeep[1] = new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_CLR.txt"));
		genomesToKeep[2] = new Genome(new File(baseDir + "\\Genomes\\Standard\\Starter_EXP.txt"));
		for (int n = 0; n<3; n++)
			genomesToKeep[n].setName("BASE");

		defaultCareerDecider = new CareerNNDecider();

	}

	public void run() {

		try {

			int mktPeriod = (int) SimProperties.getPropertyAsDouble("MarketPeriod", "3000");
			ArrayList<Character> allExperts = new ArrayList<Character>();
			ArrayList<Character> allFighters = new ArrayList<Character>();
			ArrayList<Character> allClerics = new ArrayList<Character>();
			ArrayList<Character> allCharacters = new ArrayList<Character>();
			List<Agent> tempArr = null;

			do synchronized (key) {

				allExperts.clear();
				allFighters.clear();
				allClerics.clear();
				allCharacters.clear();
				tempArr = null;

				try {
					key.wait();
				} catch (InterruptedException e) {
					logger.info("Population Spawner has been interrupted");
				}
				logger.info("Population Spawner started");

				tempArr = world.getAgents();
				for (Agent a : tempArr) {
					if (a instanceof Character) {
						Character c = (Character) a;
						if (c.getRace() == Race.HUMAN)
							allCharacters.add(c);
						if (c.getChrClass() == CharacterClass.EXPERT) 
							allExperts.add(c);
						if (c.getChrClass() == CharacterClass.FIGHTER)
							allFighters.add(c);
						if (c.getChrClass() == CharacterClass.CLERIC)
							allClerics.add(c);
					}
				}

				int humansToAdd;
				humansToAdd = maxIncrement;

				Long delay = world.getActionProcessor().getDelay();
				if (delay != null && delay > 100) {
					humansToAdd = (int)(((double)humansToAdd) * ((10000.0 - ((double)delay))/10000.0));
					logger.info("Throttled number of humans to add: " + humansToAdd);
				}

				double sampleSize = SimProperties.getPropertyAsDouble("SampleSize", "10");
				worldAge = startTime - world.getCurrentTime();

				for (int loop = 0; loop<humansToAdd; loop++) {

					// pick 2 seed Characters
					Character seedChr = AgentExemplar.getExemplar(allCharacters, sampleSize, breedingScore);
					Character seedChr2 = AgentExemplar.getExemplar(allCharacters, sampleSize, breedingScore);

					Genome chrClassBaseGenome = null;
					Decider chrClassBaseCombatDecider = null;
					Decider careerDecider = null;
					if (seedChr != null)
						careerDecider = seedChr.getCareerDecider();
					if (seedChr2 != null) {
						if (careerDecider == null) logger.info("Null CareerDecider for : " + seedChr.toString());
						if (seedChr2.getCareerDecider() == null) logger.info ("Null CareerDecider for : " + seedChr2.toString());
						careerDecider = careerDecider.crossWith(seedChr2.getCareerDecider());
					}

					if (careerDecider == null)
						careerDecider = defaultCareerDecider;
					// and default if no careerDecider found

					Character newC = new Character(Race.HUMAN, careerDecider, null, null, world);

					newC.addGold(20);
					// Set starting cash, as this may be a deciding factor in choice

					ArrayList<Character> classArray = null;
					switch (newC.getChrClass()) {
					case FIGHTER:
						classArray = allFighters;
						chrClassBaseGenome = genomesToKeep[0];
						chrClassBaseCombatDecider = brainsToKeep[0];
						break;
					case CLERIC:
						classArray = allClerics;
						chrClassBaseGenome = genomesToKeep[1];
						chrClassBaseCombatDecider = brainsToKeep[1];
						break;
					case EXPERT:
						classArray = allExperts;
						chrClassBaseGenome = genomesToKeep[2];
						break;
					default:
						logger.warning("Unknown Class in PopulationSpawner");
					}

					Genome newGenome = null;
					if (classArray.size() > 0) {
						seedChr = AgentExemplar.getExemplar(classArray, sampleSize, breedingScore);
						seedChr2 = AgentExemplar.getExemplar(classArray, sampleSize, breedingScore);
						
						if (seedChr == null) {
							logger.info("Null seedChr for " + newC.getChrClass() + ". Array size is " + classArray.size());
							for (Character c : classArray) 
								logger.info(c.toString());
						}
						
						int index = 0;
						switch (seedChr.getChrClass()) {
						case CLERIC:
							index = 1;
							break;
						case EXPERT:
							index = 2;
							break;
						}
						double score = breedingScore.getScore(seedChr) + worldAge/2000.0;
						if (score > highBreedingScore[index]) {
							highBreedingScore[index] = score;
							genomesToKeep[index] = seedChr.getGenome();
							if (index < 2)
								brainsToKeep[index] = seedChr.getCombatDecider();
							if (seedChr.getCareerDecider() != null)
								careerDeciderToKeep = seedChr.getCareerDecider();
							// not actually the one used - but not a major worry
						}

						Genome seedGen = seedChr.getGenome();
						Genome seedGen2 = seedChr2.getGenome();
						newGenome = seedGen.crossWith(seedGen2);
						newGenome.setName(seedGen.getName());

						if (seedChr.getCombatDecider() != null) 
							newC.setCombatDecider(seedChr.getCombatDecider().crossWith(seedChr2.getCombatDecider()));
						newC.addParent(seedChr);

						if (seedChr2 != seedChr) {
							newC.addParent(seedChr2);
						}

					} else { 	// none of this character class available, so pick up defaults
						newGenome = chrClassBaseGenome;
						if (chrClassBaseCombatDecider != null)
							newC.setCombatDecider(chrClassBaseCombatDecider);
					}

					newGenome.mutate();
					newC.setGenome(newGenome);

					Action firstAction = null;
					int generation = 0;
					if (seedChr != null) generation = seedChr.getGeneration();
					newC.setGeneration(generation+1);
					newC.start();
					switch (newC.getChrClass()) {
					case CLERIC:
					case FIGHTER:
						newC.setLocation(defaultStartLocation);
						firstAction = (new Equip(newC, mktPeriod));
						break;
					case EXPERT:
						newC.setLocation(tradeVille);
						firstAction = newC.decide();
						break;
					default:
						logger.warning("Unknown Class in PopulationSpawner 2");
					}
					if (firstAction == null) {
						logger.severe("Null action in Population Spawner:" + newC + ". " + newC.getDecider());
						if (newC.getDecider() instanceof StateDecider)
							logger.severe("State: " + ((StateDecider)newC.getDecider()).getState(newC, null));
					}

					world.getActionProcessor().add(firstAction);

				}
			} while (!done);

			logger.info("Population Spawner exiting");
			// and finally write the records of the Genomes To Keep

			File f;
			CharacterClass chrClass;
			for (int index = 0; index < 3; index++) {
				chrClass = CharacterClass.FIGHTER;
				if (index == 1) chrClass = CharacterClass.CLERIC;
				if (index == 2) chrClass = CharacterClass.EXPERT;
				f = new File(baseDir + "\\Genomes\\" + world.toString() + "_" + chrClass.getAbbrev() + ".txt");
				genomesToKeep[index].recordGenome(f);
			}
			for (int index = 0; index < 2; index++) {
				chrClass = CharacterClass.FIGHTER;
				if (index == 1) chrClass = CharacterClass.CLERIC;
				if (brainsToKeep[index] instanceof NeuralDecider) {
					NeuralDecider brainToSave = (NeuralDecider)brainsToKeep[index];
					brainToSave.saveBrain(world.toString() + "_" + chrClass.getAbbrev(),baseDir + "\\Brains\\");
				}
			}
			if (careerDeciderToKeep instanceof NeuralDecider)
				((NeuralDecider)careerDeciderToKeep).saveBrain(world.toString() + "_CAREER", baseDir + "\\Brains\\");

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

