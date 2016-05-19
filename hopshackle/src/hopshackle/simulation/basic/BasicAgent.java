package hopshackle.simulation.basic;
import hopshackle.simulation.*;

import java.io.File;
import java.util.*;

/**
 * A BasicAgent will wander around the world looking for resources. It starts with a Health of 20 (which is also the maximum).
 * @author James
 *
 */
public class BasicAgent extends Agent implements Persistent {

	private double health;
	private long lastMaintenance;
	private static AgentWriter<BasicAgent> agentWriter = new AgentWriter<BasicAgent>(new BasicAgentDAO());
	private static BasicAgentRetriever masterAgentRetriever = new BasicAgentRetriever();
	private static double debugChance = 1;
	private int movementPointsSpent;
	private Marriage marriage;
	private List<Long> allPartners = new ArrayList<Long>();
	private boolean isMale = true;
	public final static double FULL_HEALTH = 20.0;
	protected static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	protected static Name maleNamer = new Name(new File(baseDir + "\\MaleNames.txt"));
	protected static Name femaleNamer = new Name(new File(baseDir + "\\FemaleNames.txt"));
	private String forename = "";
	private String surname = "";
	private static String MALE_AGE_RANGE;
	private static String FEMALE_AGE_RANGE;
	public static double MINIMUM_HEALTH_FOR_BREEDING;
	public static long MIN_MALE_AGE, MAX_MALE_AGE, MIN_FEMALE_AGE, MAX_FEMALE_AGE;

	static {
		refreshBreedingAges();
	}

	public BasicAgent(World world) {
		super(world);
		health = FULL_HEALTH;
		lastMaintenance = birth;
		if (Math.random() < 0.5) isMale = false;
		if (isMale()) {
			forename = maleNamer.getName();
		} else {
			forename = femaleNamer.getName();
		}
		if (Math.random() < debugChance) 
			setDebugLocal(true);
		setPolicy(new BasicInheritance<BasicAgent>());
		agentRetriever = masterAgentRetriever;
	}

	public BasicAgent(BasicAgent parent1, BasicAgent parent2) {
		this(parent1.getWorld());
		surname = parent1.getSurname();
		if (surname.equals(""))
			surname = parent2.getSurname();
		this.addParent(parent1);
		String gender = "son";
		if (isFemale()) gender = "daughter";
		parent1.log("Has " + gender + " called " + this.toString());
		this.addParent(parent2);
		parent2.log("Has " + gender + " called " + this.toString());
		log("Is born a " + gender + " to " + parent1.toString() + " and " + parent2.toString());
		parent1.addHealth(-10);
		parent2.addHealth(-10);
		if (parent1.getNumberInInventoryOf(Resource.FOOD) > 0) {
			parent1.removeItem(Resource.FOOD);
			this.addItem(Resource.FOOD);
		}
		setLocation(parent1.getLocation());
		setDecider(parent1.getDecider());
		setGenome(parent1.getGenome().crossWith(parent2.getGenome()));
		genome.mutate();
		knowledgeOfLocations.addMapKnowledge(parent1.getMapKnowledge());
		knowledgeOfLocations.addMapKnowledge(parent2.getMapKnowledge());
	}

	public BasicAgent(World world, long uniqueID, long parent1, long parent2, List<Long> childrenList) {
		super(world, uniqueID, parent1, parent2, childrenList);
	}

	@Override
	public double getMaxScore() {
		return 200;
	}

	@Override
	public double getScore() {
		return health + children.size() * 20;
	}

	@Override
	public void maintenance() {
		super.maintenance();
		if (isDead()) return; // may have died of old age
		if (isMarried()) shareFood();
		if (getHealth() < (getMaxHealth() - 3.5) && getInventory().contains(Resource.FOOD)) {
			if (removeItem(Resource.FOOD)) {
				log("Eats surplus Food");
				addHealth(5.0);
			}
		}
		if (world.getCurrentTime() - lastMaintenance > 10000) {
			// need to eat Food
			lastMaintenance = world.getCurrentTime();
			if (getInventory().contains(Resource.FOOD)) {
				removeItem(Resource.FOOD);
				log("Eats Food for subsistence");
			} else {
				addHealth(-5.0);
				log("No Food so starves");
			}
		}
	}
	
	public long getTimeUntilNextMaintenance() {
		return lastMaintenance + 10000 - world.getCurrentTime();
	}

	private void shareFood() {
		int myFood = getNumberInInventoryOf(Resource.FOOD);
		int theirFood = getPartner().getNumberInInventoryOf(Resource.FOOD);
		int myNewFood = (myFood+theirFood+1)/2;
		for (int loop = 0; loop < myFood - myNewFood; loop++) {
			removeItem(Resource.FOOD);
			getPartner().addItem(Resource.FOOD);
		}
	}

	public double getHealth() {
		return health;
	}

	public void addHealth(double changeInHealth) {
		if (isDead()) return;
		health += changeInHealth;
		if (health > getMaxHealth())
			health = getMaxHealth();
		log(String.format("Gains %.0f Health - now on %.0f", changeInHealth, health));
		if (health <= 0.00)
			this.die("Expires from hunger");
	}

	public double getMaxHealth() {
		return FULL_HEALTH;
	}

	@Override
	public void die(String reason) {
		if (isDead()) {
			errorLogger.severe("Dying when already dead. " + toString());
			return;
		}
		agentWriter.write(this, getWorld().toString());
		super.die(reason);
		dissolveMarriage();
	}

	public static AgentWriter<BasicAgent> getAgentWriter() {
		return agentWriter;
	}

	public void recordSpendOfMovementPoints(int mp) {
		movementPointsSpent += mp;
	}
	public int getMovementPointsSpent() {
		return movementPointsSpent;
	}

	public void setMarriage(Marriage newMarriage) {
		if (marriage == null || newMarriage == null) {
			String logMessage;
			if (newMarriage != null) {
				marriage = newMarriage;
				BasicAgent spouse = getPartner();
				logMessage = "Marries " + spouse;
				allPartners.add(spouse.getUniqueID());
			} else {
				logMessage = "Marriage is dissolved"; 
				marriage = null;
			}
			log(logMessage);
		}	
		else 
			throw new AssertionError("Marriage not possible if already married.");
	}
	
	@Override
	public BasicAction decide(Decider decider) {
		if (isMarried() && isFemale()) {
			return null;
		}
		return (BasicAction) super.decide(decider);
	}
	
	public void dissolveMarriage() {
		if (marriage != null)
			marriage.dissolve();
	}

	public boolean isMarried() {
		return (marriage != null);
	}

	public boolean isMale() {
		return isMale;
	}
	public boolean isFemale() {
		return !isMale;
	}
	public void setMale(boolean isMale) {
		this.isMale = isMale;
	}

	public BasicAgent getPartner() {
		if (isMarried())
			return marriage.getPartnerOf(this);
		return null;
	}
	
	public List<BasicAgent> getAllPartners() {
		List<BasicAgent> partners = new ArrayList<BasicAgent>();
		BasicAgentRetriever bar = new BasicAgentRetriever();
		for (Long id : allPartners) {
			BasicAgent s = (BasicAgent) BasicAgent.getAgent(id, bar, world);
			if (s != null)
				partners.add(s);
		}
		bar.closeConnection();
		return partners;
	}

	/**
	 * 
	 * @return
	 * True if the BasicAgent is of breeding age
	 */
	public boolean ableToBreed() {
		long maxAge, minAge;
		maxAge = MAX_MALE_AGE;
		minAge = MIN_MALE_AGE;
		if (isFemale()) {
			maxAge = MAX_FEMALE_AGE;
			minAge = MIN_FEMALE_AGE;
		}
		long age = getAge();
		if (age < minAge || age > maxAge) 
			return false;
		if (getHealth() < MINIMUM_HEALTH_FOR_BREEDING)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer temp = new StringBuffer(forename);
		if (surname != "")
			temp.append(" " + surname);
		temp.append(" [" + getUniqueID() + "]");
		return temp.toString();
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String string) {
		String oldSurname = surname;
		log("Surname changed to " + string);
		surname = string;
		if (isMale()) 
			for (Agent c : getChildren()) {
				BasicAgent child = (BasicAgent)c;
				if (!child.isDead() && child.getSurname().equals(oldSurname))
					child.setSurname(surname);
			}
	}

	public static void refreshBreedingAges() {
		try {
			MALE_AGE_RANGE = SimProperties.getProperty("MaleBreedingAgeRange", "0-200");
			FEMALE_AGE_RANGE = SimProperties.getProperty("FemaleBreedingAgeRange", "0-200");
			MINIMUM_HEALTH_FOR_BREEDING = SimProperties.getPropertyAsDouble("MinimumHealthForBreeding", "11.0");
			MIN_MALE_AGE = Long.valueOf(MALE_AGE_RANGE.split("-")[0])*1000;
			MAX_MALE_AGE = Long.valueOf(MALE_AGE_RANGE.split("-")[1])*1000;
			MIN_FEMALE_AGE = Long.valueOf(FEMALE_AGE_RANGE.split("-")[0])*1000;
			MAX_FEMALE_AGE = Long.valueOf(FEMALE_AGE_RANGE.split("-")[1])*1000;
		} catch (NumberFormatException e) {
			errorLogger.severe("Invalid number format in breeding age ranges. Defaulting to no breeding. " + e.getMessage());
		}
	}
}
