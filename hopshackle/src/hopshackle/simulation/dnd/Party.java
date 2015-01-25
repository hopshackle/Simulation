package hopshackle.simulation.dnd;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.actions.*;

import java.util.ArrayList;

public class Party extends DnDAgent {

	private Character leader;
	private ArrayList<Character> members;
	private ArrayList<Character> bannedList;

	private ArrayList<Character> exmembers;
	private int reputation;
	private boolean setDead = false;

	public Party (Character l) {
		super(l.getWorld());
		if (Math.random() < 0.0005) this.setDebugLocal(true);
		members = new ArrayList<Character>();
		exmembers = new ArrayList<Character>();
		bannedList = new ArrayList<Character>();
		setLocation(l.getLocation());
		addMember(l);
		addAction(new Rest(this, false)); // always rest as first action on party formation
		reputation = 0;
	}

	public synchronized void addMember(Character c) {
		if (c.getParty() != null) return;
		c.setParty(this);
		if (!members.contains(c)) members.add(c);
		if (leader==null) setLeader(c);
		log(c.toString() + " joins Party " + toString());
		c.purgeActions();
	}
	public synchronized void removeMember(Agent c) {
		if (!exmembers.contains(c)) {
			exmembers.add((Character)c);
		}
	}

	public void chooseLeader() {
		for (Character candidate : members) {
			if (!candidate.isDead()) {
				setLeader(candidate);
				break;
			}
		}
		if (leader == null && members.size() > 0) {
			// no-one left alive. So keep going until we hit maintenance and get cleaned up
			leader = members.get(0);
		}
	}
	public void setLeader(Agent c) {
		setDead = true; // a bit of a hack for state explorers
		dispatchLearningEvent();
		setDead = false;
		// we need to learn based on old leader. We then reset for new leader.

		leader = null;
		if (c != null) {
			leader = (Character) c;
			setGenome(c.getGenome());
			log(c.toString() + " selected as Leader");
			c.log("Selected as leader of " + toString());		
		} 
	}
	
	@Override
	public Decider getDecider() {
		if (leader != null)
			return leader.getDecider();
		return null;
	}

	public synchronized ArrayList<Character> getMembers() {
		ArrayList<Character> retValue = new ArrayList<Character>();
		for (Character ch : members) retValue.add(ch);
		return retValue;
	}

	public Character getLeader() {
		return leader;
	}
	public boolean isDisbanded() {
		if (members.size()< 1) return true;
		return false;
	}
	public boolean isDead() {
		return isDisbanded() || setDead;
	}

	public void surviveEncounter(int CR) {
		addReputation(CR);
		ArrayList<Character> tempArr = new ArrayList<Character>();
		for (Character c: members) {
			if (!c.isDead()) tempArr.add(c);
		}
		int survivors = tempArr.size();
		if (survivors>0) {
			for (Character c : tempArr) {
				int denominator = Math.max(1,(int)c.getLevel() - 2) * survivors;
				c.addXp(CR * 300 / denominator);
			}
			if (!(leader == null) && !getLeader().isDead() && survivors > 1) {
				Reputation.XP_GAIN_WHILE_LEADER.apply(leader, CR * 300);
				Reputation.DEATH_WHILE_LEADER.apply(leader, members.size()-survivors);
			}
		}
	}
	public void rest(int seconds){
		for (Character c : members) {
			if (!c.isDead()) c.rest(seconds);
		}
	}
	public boolean processApplication (Character applicant) {
		if (applicant.getParty() != null) {
			applicant.getParty().removeMember(applicant);
		}
		addMember(applicant);
		return true;
	}

	public Genome getGenome() {
		if (leader != null) 
			return getLeader().getGenome();

		return null;
	}

	public synchronized double getLevel() {
		double total = 0;
		for (Character c : members) {
			total = total + c.getLevel();
		}
		return (total) / ((double)members.size());
	}

	public synchronized double getLevelStdDev() {
		double total = 0;
		double squareSum = 0;
		for (Character c : members) {
			double l = c.getLevel();
			total = total + l;
			squareSum = squareSum + l*l;
		}
		double mean = total / members.size();

		return Math.sqrt( squareSum/members.size() - mean*mean );

	}

	@Override
	public int getSize() {
		return members.size();
	}

	@Override
	public synchronized void setStationary(boolean state) {
		super.setStationary(state);
		for (Character c : members){
			c.setStationary(state);
		}
	}

	@Override
	public synchronized void setLocation(Location l) {
		super.setLocation(l);
		for (Agent c : members){
			if (!c.isDead())
				c.setLocation(l);
		}
	}

	public synchronized void maintenance() {

		ArrayList<Character> tempArr = new ArrayList<Character>();
		for (Character ch : members) {
			if (ch.isDead()) tempArr.add(ch);
		}
		for (Agent ch : tempArr) {
			removeMember(ch);
		}

		for (Character ch : exmembers) {
			finalRemoval(ch);
			// this tidies up, and gives the ex-member their first action in the new world.
		}

		if (members.size()  == 1) {
			// disband party
			log("Party disbanded as only one member left");
			Character finalMember = members.get(0);
			Action leave = new LeaveParty(finalMember, false);
			leave.run();
			finalRemoval(finalMember);
		}

		if (members.size() == 0)  {
			setLeader(null);
			die("Disbanded");
			// to tidy up and ensure the last members set off again
		}

		if (members.size() > 0) {
			if (leader == null) {
				chooseLeader();
			}
		}
		exmembers = new ArrayList<Character>();

		if (Math.random()<0.2)
			addReputation(-(int)getLevel());

		setChanged();
		notifyObservers();
	}

	private synchronized void finalRemoval(Character ch) {
		ch.setParty(null);
		members.remove(ch);
		if (leader != null && leader.equals(ch)) setLeader(null);
		Action action = ch.decide(); 
		ch.addAction(action);
		// to ensure that members who leave always do so at the end of a Party Action
	}

	public String toString() {return String.valueOf(getUniqueID());}
	public String getType() {
		return "PARTY_" + leader.getChrClass();
	}

	public void log(String s) {
		super.log(s);
		try {
			// we deliberately don't use the synchronise method.
			// This may therefore fail - in which case just continue.
			for (Character ch : members) {
				ch.log("Party: " + s);
			}
		} catch (RuntimeException e) {
			errorLogger.severe("Error in Party log: " + e.toString());
		}
	}

	public void banMember(Character c) {
		bannedList.add(c);
	}
	public boolean isBanned(Agent c) {
		return bannedList.contains(c);
	}

	@Override
	public double getWound() {
		double wound = 0.0;
		for (Character c : members) {
			wound += (double)(c.getMaxHp()-c.getHp())/(double)(c.getMaxHp());
		}
		return wound/members.size();
	}
	public synchronized double getMagic() {
		double maxMagic = 0.0;
		double magic = 0.0;
		for (Character c : members) {
			magic += c.getMp();
			maxMagic += c.getMaxMp();
		}
		if (maxMagic < 0.5) 
			return -1.0;
		else 
			return magic/maxMagic;
	}

	public void die(String reason) {
		bannedList.clear();
		super.die(reason);
	}
	public int getReputation() {return reputation;}
	public void addReputation(int points) {reputation+= points;}

	@Override
	public synchronized void addGold(double amount) {
		ArrayList<Character> tempArr = new ArrayList<Character>();
		for (Character c: members) {
			if (!c.isDead()) tempArr.add(c);
		}
		if (tempArr.size()>0) {
			double share = amount / tempArr.size();
			double remainder = amount - (share * tempArr.size()); 
			for (Character c : tempArr) {
				c.addGold(share);
			}
			if (getLeader()!=null && !getLeader().isDead()) {
				leader.addGold(remainder);
				if (tempArr.size() > 1)
					Reputation.GOLD_GAIN_WHILE_LEADER.apply(leader, (int)amount);
			}
		}
	}

	@Override
	public synchronized void addItem(Artefact item) {
		// pick a Party member randomly
		int highestRoll = -100;
		Character choice = null;
		for (Character c : members) {
			if (!c.isDead()){
				int roll = Dice.roll(1, 100);
				if (roll > highestRoll) {
					highestRoll = roll;
					choice = c;
				}
			}
		}
		if (choice !=null) {
			choice.addItem(item);
		}
	}

	public synchronized void addCondition(CombatCondition cc) {
		ArrayList<Character> targetList = this.getMembers();
		for (Character c : targetList)
			if (!c.isDead())
				c.addCondition(cc);
	}

	public double getScore() {
		if (leader != null) 
			return leader.getScore();
		return 0.0;
	}
	public double getMaxScore() {
		return 10000;
	}
}
