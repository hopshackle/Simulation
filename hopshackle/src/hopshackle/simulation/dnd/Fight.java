package hopshackle.simulation.dnd;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.actions.Attack;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class Fight extends Encounter {

	/* 
	 *  The Fight object is responsible for conducting a Fight between two sides.
	 *  Both sides must be DNDAgents - i.e. either an individual Character, or a Party.
	 *  
	 *  Fight is responsible for:
	 *   - determining initiative of each of the participants
	 *   - running any initial surprise round, allowing for flat-footedness (TODO:)
	 *   - keeping track of exactly who is fighting who
	 *   - and who is hors de Combat  (in a later version it will be responsible for 
	 *   keeping track of people's location in some way - or delegating that activity)
	 *   - For each combatant in initiative order, calling a CombatDecider
	 *   to determine what they should do.
	 *   - Then executing the run() action on the action, passing itself as 
	 *   a context. The run action will update all states etc that are needed.
	 *   - Once the fight has completed (200 rounds, or one side completely 
	 *   out of action), terminating. Whatever called the Fight is responsible for
	 *   dealing with any unrelated repercussions (XP etc.)
	 */
	private ArrayList<CombatAgent> side1;
	private ArrayList<CombatAgent> side2;
	private ArrayList<CombatAgent> allCombatants, initiativeOrder;
	private ArrayList<CombatAgent> combatantTargets;
	private int[] combatantOpponentCount;
	private Hashtable<CombatAgent, Integer> initArray ;
	private boolean debug, isInternal;
	private File logFile;
	private FileWriter logWriter;
	private long start;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");
	private int currentRound;
	private boolean resolved = false;
	private FightTracker fightTracker;

	/* We have a stylised set up, with each combatant able to attack one combatant
	 * from the other side. Up to four combatants may attack one target.
	 */
	public Fight(DnDAgent c1, DnDAgent c2) {
		super(c1, c2);
		start = System.currentTimeMillis();
		side1 = new ArrayList<CombatAgent>();
		side2 = new ArrayList<CombatAgent>();
		for (Character c : c1.getMembers()) 
			if (!c.isDead())
				side1.add(c.getCombatAgent());
		for (Character c : c2.getMembers())
			if (!c.isDead())
				side2.add(c.getCombatAgent());

		allCombatants = new ArrayList<CombatAgent>();
		initiativeOrder = new ArrayList<CombatAgent>();
		allCombatants.addAll(side1);
		allCombatants.addAll(side2);

		int total = allCombatants.size();
		combatantTargets = new ArrayList<CombatAgent>(total);
		combatantOpponentCount = new int[total];
		for (int n=0; n<total; n++) combatantTargets.add(null);

		// determine if this is Internal
		isInternal = false;
		if (c1 instanceof Character && c2 instanceof Character) {
			Party p1 = ((Character)c1).getParty();
			Party p2 = ((Character)c2).getParty();
			if (p1 != null && p2 !=null && p1 == p2)
				isInternal = true;
		}

		// firstly sort combatants in Initiative order
		initArray = new Hashtable<CombatAgent, Integer>();
		// In addition, at the start of any Fight, all combatants will be hors de combat
		// waiting to make their first decision
		// (This can be different for ambushes etc. that may be implemented in the future)
		for (CombatAgent c : allCombatants) 
			c.initialiseFightStatus(this);	

		fightTracker = new FightTracker(this);	// tracks starting and end states of fight for use by various interested objects

		// this also means the Combatant know what Fight it's in, and also
		// who the current opponent is. This is needed for when they start making decisions
		// based on their Genome.
		for (CombatAgent c : allCombatants){
			initiativeOrder.add(c);
			int init = c.getInitiative(this);
			initArray.put(c, init);
			if (debug) log(c.toString() + " rolls initiative of "+init);
		}
		Collections.sort(initiativeOrder, new Comparator<CombatAgent>() {
			public int compare(CombatAgent c1, CombatAgent c2) {
				int retValue = 0;
				retValue =  initArray.get(c2) - initArray.get(c1);
				return retValue;
			}

		});

		currentRound = 0;

	}

	public void resolve() {
		// check entry conditions are valid
		if (!(side1.size()!=0 && side2.size()!=0)) {
			logger.severe("Fight.resolve() invoked without two sides");
			if (side1.size() > 0) 
				for (CombatAgent ca : side1)
					logger.info("Side1: " + ca.toString());
			if (side2.size() > 0) 
				for (CombatAgent ca : side2)
					logger.info("Side2: " + ca.toString());
			return;
		}


		do {
			oneRound();
		} while (side1.size()>0 && side2.size()>0 && currentRound<200);	

		resolved = true;
		fightTracker.resolveFight();

		for (CombatAgent c : allCombatants) {
			c.leavesFight();
			if (c instanceof Agent)
				c.getCombatDecider().setTeacher(null);
		}

		// and tidy up
		if (logWriter != null) {
			try {
				logWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				logger.severe(e.toString());
			}
		}

	}

	public void oneRound() {

		// Now iterate over allCombatants 
		// each one picks a member of the other side, and hit them
		// until all of one side are dead

		/* Check if you have an opponent.
		 * If so - continue.
		 * 
		 * else we need to select a new one.
		 * 
		 * First of all see if anyone has you as an opponent. If so, fight them.
		 * 
		 * If not, then pick a new target randomly.
		 */
		ArrayList<CombatAgent> combatantCycle = new ArrayList<CombatAgent>();
		for (CombatAgent attacker : initiativeOrder) {
			combatantCycle.add(attacker);
		}
		// We take a copy for the main iteration. This is because new combatants can be added during the round.
		// These will only take an action in the following round

		for (CombatAgent attacker : combatantCycle) {

			// First we have a new round. This does housekeeping, such as removing expired
			// combat conditions, and re-setting attacks of opportunity
			attacker.newRound();

			if (attacker.isDead() || attacker.isStunned()) 
				continue;
			// if already dead or stunned, then move on
			// allCombatants always keeps record of all involved
			// it is side1 and side2 that have dead agents removed from then

			// Second, call a CombatDecider to work out what to do

			int attackerIndex = allCombatants.indexOf(attacker);
			CombatAgent defender = combatantTargets.get(attackerIndex);
			Agent defenderController = null;
			if (defender != null) defenderController = defender.getController();
			if (attacker.getCombatDecider() == null) logger.info("Null combat Decider for : " + attacker.toString());
			ActionEnum combatAction = attacker.getCombatDecider().decide(attacker.getController(), defenderController);
			// Not perfect where the CombatAgent is not itself the Character...

			if (debug) {
				log(attacker + " decides on " + combatAction);
				log("Their current target is " + defender);
			}

			// Third, do it. The Action is responsible for updating the state 
			// of the Fight, by calling setTarget(), moveToReserve() and
			// removeCombatant() as needed.
			if (combatAction != null)
				combatAction.getAction(attacker.getController(), defenderController).run();

		}
		currentRound++;
		if (debug) {
			log("End of round " + currentRound);
			log ("Side 1 has " + side1.size() + " combatants left");
			log ("Side 2 has " + side2.size() + " combatants left");
		}

		// and finally we re-sort for initiative order

		Collections.sort(initiativeOrder, new Comparator<CombatAgent>() {
			public int compare(CombatAgent c1, CombatAgent c2) {
				int retValue = 0;
				retValue =  initArray.get(c2) - initArray.get(c1);
				return retValue;
			}

		});
	}

	public boolean getDebug() {
		return debug;
	}
	public void setDebug(boolean flag) {
		debug = flag;
		if (logFile == null && debug == true) {
			String logFileName = "C://Simulations//logs//Fight_" +
			String.format("_%tY%<tm%<td_%<tH%<tM%<tS.log", Calendar.getInstance());
			logFile = new File(logFileName);
			try {
				logWriter = new FileWriter(logFile, true);
			} catch (IOException e) {
				e.printStackTrace();
				logger.severe(e.toString());
			}
		}
	}
	public void log(String s) {
		if(!debug) return;
		try {
			long age = System.currentTimeMillis() - start;
			s = age + ": " + s; 
			logWriter.write(s+newline);
			logger.fine(String.format("%s %s", toString(), s));
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe(e.toString() + " in log function: not severe");
		} 
	}

	public ArrayList<CombatAgent> getAllCombatants() {
		ArrayList<CombatAgent> retValue = new ArrayList<CombatAgent>();
		for (CombatAgent ca : allCombatants) 
			retValue.add(ca);
		return retValue;
	}
	public ArrayList<CombatAgent> getSide(CombatAgent c1) {
		ArrayList<CombatAgent> retValue = new ArrayList<CombatAgent>();
		ArrayList<CombatAgent> temp = side1;
		if (side2.contains(c1)) temp = side2;

		for (CombatAgent ca : temp) 
			retValue.add(ca);
		return retValue;
	}
	public ArrayList<CombatAgent> getOtherSide(CombatAgent c1) {
		ArrayList<CombatAgent> retValue = new ArrayList<CombatAgent>();
		ArrayList<CombatAgent> temp = side2;
		if (side2.contains(c1)) temp = side1;

		for (CombatAgent ca : temp) 
			retValue.add(ca);
		return retValue;
	}

	public int getSideSize(CombatAgent c1) {
		// This return the number of active combatants in 
		// the side containing Character c1
		if (side1.contains(c1)) {
			return side1.size();
		}
		return side2.size();
	}
	public int getOtherSideSize(CombatAgent c1) {
		if (side1.contains(c1)) {
			return side2.size();
		}
		return side1.size();
	}

	public ArrayList<CombatAgent> getAttackersOf(CombatAgent c) {
		// This return the attacker of c (esp useful for A of O)
		ArrayList<CombatAgent> retValue = new ArrayList<CombatAgent>();
		for (int n = 0; n<combatantTargets.size(); n++) {
			if (combatantTargets.get(n) != null && combatantTargets.get(n).equals(c))
				retValue.add(allCombatants.get(n));
		}
		return retValue;
	}

	public CombatAgent getOpponent(CombatAgent c, boolean force) {
		// force indicates whether a new Character should be found if not 
		// currently engaged

		CombatAgent retValue = combatantTargets.get(allCombatants.indexOf(c));

		if (retValue == null && force) {
			// Not currently engaged.
			if (c.isTargetted()) {
				// but in Melee, so pick one of these first
				retValue = chooseValidTarget(getAttackersOf(c), c);
				setTarget(c, retValue);
			} 

			// Now pick any one at random
			if (retValue == null) {
				ArrayList<CombatAgent> temp = side1;
				if (side1.contains(c)) 
					temp = side2;
				retValue = chooseValidTarget(temp, c);
				setTarget(c, retValue);
			}
		}
		return retValue;
	}
	private CombatAgent chooseValidTarget(ArrayList<CombatAgent> list, CombatAgent attacker) {
		ArrayList<CombatAgent> choice = new ArrayList<CombatAgent>();
		// Remove any which already have four opponents or which are non-attackable
		for (CombatAgent opp : list) {
			if (opp.isDead()) continue;
			if (!opp.isAttackable(attacker)) continue;
			int i = allCombatants.indexOf(opp);
			if (combatantOpponentCount[i] < 4) 
				choice.add(opp);
		}
		if (choice.size() == 0) return null;
		int which = Dice.roll(1,choice.size());
		return choice.get(which-1);
	}

	public void moveToReserve(CombatAgent c) {
		// Character is no longer in combat, and needs to be removed
		if (debug) log(c + " withdraws from combat");
		for (CombatAgent opp : getAttackersOf(c)) {
			setTarget(opp, null);
		}
		c.setTargetted(false);
		setTarget(c, null);
	}

	public void setTarget(CombatAgent attacker, CombatAgent defender) {
		// This marks a change of target. We need to do the housekeeping 
		// for both sides
		ArrayList<CombatAgent> temp = new ArrayList<CombatAgent>();
		temp.add(defender);
		if (defender != null && !(defender == chooseValidTarget(temp, attacker)))
			return;
		// ensures that the target is attackable, and we are trying to set
		// the target to a specific one
		if (debug) log(attacker + " changes target to " + defender);
		CombatAgent oldTarget = getOpponent(attacker, false);
		int indexOfOld = allCombatants.indexOf(oldTarget);
		int indexOfAttacker = allCombatants.indexOf(attacker);

		attacker.setCurrentTarget(defender);
		combatantTargets.set(indexOfAttacker, defender);
		if (indexOfOld > -1) {
			combatantOpponentCount[indexOfOld]--;
			if (combatantOpponentCount[indexOfOld] < 1) {
				oldTarget.setTargetted(false);
			}
		}
		if (defender != null) {
			int indexOfDefender = allCombatants.indexOf(defender);
			combatantOpponentCount[indexOfDefender]++;
			defender.setTargetted(true);
		}
	}

	public void removeCombatant(CombatAgent c) {

		if (debug) log(c + " is removed from the fight");

		ArrayList<CombatAgent> attackers = getAttackersOf(c);
		for (CombatAgent attacker : attackers) 
			setTarget(attacker, null);

		setTarget(c, null);

		side1.remove(c);
		side2.remove(c);
	}

	public int getCurrentRound() {
		return currentRound;
	}
	public boolean isInternal() {return isInternal;}

	public void addCombatant(CombatAgent newCombatant, CombatAgent ally) {
		// adds the newCombatant to the fight on the same side as the ally
		// Ally must be an agent already in the fight

		if (!allCombatants.contains(ally))
			return;

		allCombatants.add(newCombatant); 	// the combatant
		initiativeOrder.add(newCombatant);
		combatantTargets.add(null);		  	// initially it has no target
		int total = allCombatants.size();	
		int[] temp = new int[total];		// we also have to allow other combatants to target this combatant
		for (int n=0; n<total-1; n++) temp[n]=combatantOpponentCount[n];
		combatantOpponentCount = temp;

		if (side1.contains(ally)) {
			side1.add(newCombatant);
		} else {
			side2.add(newCombatant);
		}

		newCombatant.initialiseFightStatus(this);
		int init = newCombatant.getInitiative(this);
		initArray.put(newCombatant, init);
		if (debug) log(newCombatant.toString() + " rolls initiative of "+init);
	}

	public boolean isActive() {
		return !resolved;
	}

	public FightTracker getFightTracker() {return fightTracker;}

	public void provokesAO(CombatAgent victim) {
		// This is from the person last attacked, plus any currently attacking
		ArrayList<CombatAgent> attackers = getAttackersOf(victim);
		CombatAgent opponent = getOpponent(victim, false);
		if (opponent != null && !attackers.contains(opponent))
			attackers.add(opponent);
		for (CombatAgent attacker : attackers) {
			attacker.log("Attack of Opportunity on " + victim.toString());
			Attack attackOfOpportunity = new Attack(attacker, victim, false, false);
			attackOfOpportunity.run();
		}
	}

	public int getPossibleAO(CombatAgent ca) {
		CombatAgent target = ca.getCurrentTarget();
		ArrayList<CombatAgent> attackers = getAttackersOf(ca);
		int retValue = attackers.size();
		if (target != null && !(attackers.contains(target))) 
			retValue++;
		return retValue;
	}
}
