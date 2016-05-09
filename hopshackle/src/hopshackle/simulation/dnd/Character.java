package hopshackle.simulation.dnd;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.genetics.*;

import java.io.File;
import java.util.*;

public class Character extends DnDAgent implements Persistent {

	private Attribute strength, dexterity, constitution, intelligence, wisdom, charisma;
	private String name;
	private Race race;
	private CharacterClass chrClass;
	private int level;
	private int hp;
	private int maxHp;
	private int subdualHp;
	private long xp;
	private int mp;
	private int maxMp;
	private int socialReputation;
	private List<Skill> skills;
	private List<Spell> spells;
	private List<Spell> spellsInAction;

	private Party party = null;
	private Decider partyDecider, careerDecider;
	private TradeDecider tradeDecider;
	private CharacterCombatAgent combatAgent;

	private String partyTestString, worldIdentifierString;

	private boolean initialised = false;
	protected long lastMaintenance;
	private static AgentWriter<Character> characterWriter = new AgentWriter<Character>(new CharacterDAO());

	public Character(World world) {
		//default is a human fighter
		this(Race.HUMAN, CharacterClass.FIGHTER, world);
	}

	public Character(Race r, CharacterClass c, World world) {
		this(r, c, null, null, world);
	}

	public Character(Race r, CharacterClass chrClass, String n, Genome bg, World world) {
		this(r, new CareerDecisionHardCoded(chrClass), n, bg, world);
		start();
	}

	public Character(Race r, Decider careerDecider, String n, Genome bg, World world) {
		super(world);
		partyTestString = "Party: " + getUniqueID() + ": ";
		race = r;
		skills = new ArrayList<Skill>();
		spells = new ArrayList<Spell>(0);
		spellsInAction = new ArrayList<Spell>(0);
		combatAgent = new CharacterCombatAgent(this);

		//Genome
		if (bg == null) {
			genome = new Genome();
		} else genome = Genome.GenomeFactory(bg);

		r.applyRacialModifiers(this);

		if (n == null && race == Race.HUMAN) {
			n = Name.getName(race, 0);
		} else {
			n = "Unnamed";
		}

		if (n.length()>200) {
			n = n.substring(0, 200);
		}

		name = n;
		xp = 0;
		level = 0;
		hp = 0;
		maxHp = 0;
		mp = 0;
		maxMp = 0;
		subdualHp = 0;
		socialReputation = 0;
		this.setCareerDecider(careerDecider);
		chrClass = chooseClass(careerDecider);
		new Skill(this, Skill.skills.WEAPON_PROFICIENCY_SIMPLE);
		// Everybody can use Simple weapons

		lastMaintenance = -1; // this is set correctly once we decide on the location

		String worldIdentifier = "Dummy";
		if (world !=null)
			worldIdentifier = world.toString();
		setLogSuffix(worldIdentifier);	// as default

		if (Math.random() < 0.002 && race.equals(Race.HUMAN))
			setDebugLocal(true);

		log(chrClass.toString());
		log(String.format("STR: %d, DEX: %d, CON: %d, INT: %d, WIS: %d, CHA: %d", 
				getStrength().getValue(),
				getDexterity().getValue(),
				getConstitution().getValue(),
				getIntelligence().getValue(),
				getWisdom().getValue(),
				getCharisma().getValue()));
	}

	/* the start() method is called to indicate that all set up is complete
	 *  for the character - and we are ready to party.
	 *  It is only at this stage that the character will become level 1
	 *  
	 *  It is explicitly not called from the standard constructors
	 */
	public void start() {
		if (!initialised)
			levelUp();
		initialised = true;
	}


	public int getBaseHitMod() {
		return chrClass.baseHit(level);
	}

	public Attribute getCharisma() {
		return charisma;
	}

	public void setCharisma(Attribute charisma) {
		this.charisma = charisma;
	}

	public CharacterClass getChrClass() {
		return chrClass;
	}

	public Attribute getConstitution() {
		return constitution;
	}

	public void setConstitution(Attribute constitution) {
		this.constitution = constitution;
	}

	public Attribute getDexterity() {
		return dexterity;
	}

	public void setDexterity(Attribute dexterity) {
		this.dexterity = dexterity;
	}

	public int getHp() {
		return hp;
	}
	public int getTempHp() {
		return subdualHp;
	}
	public int getMaxHp() {
		return maxHp;
	}
	public int getMp() {
		return mp;
	}
	public int getMaxMp() {
		return maxMp;
	}
	public void setMaxMp(int max) {
		maxMp = max;
	}
	public void setMaxHp(int max) {
		maxHp = max;
		addHp(max-hp, false);
	}

	public void addHp(int change, boolean tempHp) {
		if (tempHp) {
			subdualHp = subdualHp - change;
			if (subdualHp < 0) subdualHp = 0;
			if (change<0) log("Loses "+ (-change) + " temp hp. Now on " + subdualHp);
			if (change>0) log("Gains " + change + " temp hp. Now on " + subdualHp);
			if (subdualHp >= hp) log("is Knocked Out");
		} else {
			int oldhp = hp;
			hp = hp + change;
			if (change > 0) { // real healing also affects temp hp
				subdualHp = subdualHp - change;
				if (subdualHp < 0) subdualHp = 0;
			}
			if (hp > maxHp) hp = maxHp;
			if (oldhp!=hp) {
				if (change<0) log("Loses "+ (-change) + " hp. Now on " + hp);
				if (change>0) log("Gains " + change + " hp. Now on " + hp);
				updateChanged();
			}
			if (hp < 0) {this.die("In combat.");}
		}
	}
	public void addMp(int change) {
		int oldmp = mp;
		mp = mp + change;
		if (mp > maxMp) mp = maxMp;
		if (mp < 0) {mp=0;}
		if (oldmp!=mp) {
			if (change<0) log("Loses "+ (-change) + " mp. Now on " + mp);
			if (change>0) log("Gains " + change + " mp. Now on " + mp);
			updateChanged();
		}
	}

	public void rest(int seconds){
		// Before resting - use up any existing magic points

		//first heal and restore magic points
		for (int n=0; n<seconds; n++) addHp(level, false);
		mp = maxMp;
		subdualHp = 0;

		// Then consider using Healing Potion
		// If wounded, and not in safe area, and at least 6hp damage
		if (location != null && location instanceof Square)  {
			Square s = (Square) location;
			if ( (getMaxHp()-getHp()) > 5 && s.getX() + s.getY() > 0)
				Potion.CURE_LIGHT_WOUNDS.drink(this);
		}
		// then trade
		trade(null, 0);
	}

	public void trade(Recipe recipe, int number) {
		if (tradeDecider != null) {
			tradeDecider.trade(this, recipe, number);
		}
	}
	public Attribute getIntelligence() {
		return intelligence;
	}

	public void setIntelligence(Attribute intelligence) {
		this.intelligence = intelligence;
	}

	public double getLevel() {
		return level;
	}

	public Race getRace() {
		return race;
	}

	public Attribute getStrength() {
		return strength;
	}

	public void setStrength(Attribute strength) {
		this.strength = strength;
	}

	public Attribute getWisdom() {
		return wisdom;
	}

	public void setWisdom(Attribute wisdom) {
		this.wisdom = wisdom;
		// and we should re-check max magic points and clerical spells known as well
		if (chrClass !=null) {
			setMaxMp(chrClass.getMaxMP(this));
			chrClass.setSpells(this);
		}
	}

	public long getXp() {
		return xp;
	}

	public void surviveEncounter(int CR) {
		if (CR > 0) {
			int denominator = Math.max(1, (int)getLevel() - 2);
			addXp(CR * 300 / denominator);
		}
	}
	public void addXp(long change) {
		xp = xp + change;
		log("Gains " + change + " xp, taking total to " + xp);
		//	if (xp < 0) die("Dies of level drain");
		checkLevel();
		updateChanged();
	}

	public int getAC() {
		// TODO: This method is only used by GUI display. So should be removed at some point, and replaced with the same mechanism
		// as used for actual combat, taking into account all the various CombatConditions
		try {
			getLock();

			int retValue = 10;
			int dexBonus = dexterity.getMod();
			Armour armour = combatAgent.getArmour();
			Shield shield = combatAgent.getShield();
			if (armour != null) {
				retValue += armour.getACBonus();
				dexBonus = Math.min(dexBonus, armour.getMaxDexBonus());
			}
			if (shield != null) {
				retValue += shield.getACBonus();
				dexBonus = Math.min(dexBonus, shield.getMaxDexBonus());
			}

			return retValue + dexBonus;

		} finally {
			releaseLock();
		}
	}

	public void levelUp() {
		try {
			getLock();
			long nextBarrier = 0;
			if (level == 0 && chrClass == CharacterClass.CLERIC) 
				resetSpellChoice();
			// because clerical spell choice is determined by Genome - which is not (necessarily) known at the time of career choice
			for (int i=level; i>0; i--) {
				nextBarrier+= i*1000;
			}
			level++;
			if (xp < nextBarrier) xp = nextBarrier;
			int dieRoll = Dice.roll(1, chrClass.getHitDie());
			if (level == 1 && !chrClass.getNPC()) dieRoll = chrClass.getHitDie();
			dieRoll += constitution.getMod();
			if (dieRoll<1) dieRoll=1;
			hp = hp + dieRoll;
			maxHp = maxHp + dieRoll;
			log("Advances to level " + level);
			log("Max hp = " + maxHp + "; hp = " + hp);
			chrClass.levelUp(this);
		} finally {
			releaseLock();
		}
	}

	private void checkLevel() {
		long nextBarrier = 0;
		for (int i=level; i>0; i--) {
			nextBarrier+= i*1000;
		}
		if (xp >= nextBarrier) {
			errorLogger.fine(String.format("%s Reached level %d. XP:%d",toString(), level+1, xp));
			levelUp();
		}
	}

	private CharacterClass chooseClass(Decider careerDecider) {
		if (!race.equals(Race.HUMAN)) 
			return CharacterClass.WARRIOR;

		ActionEnum chosenClass = careerDecider.decide(this);

		CharacterClass retValue = null;
		if (chosenClass == CareerActionsI.EXPERT) {
			retValue = CharacterClass.EXPERT; 
		} else if (chosenClass == CareerActionsI.FIGHTER) {
			retValue = CharacterClass.FIGHTER;
		} else if (chosenClass == CareerActionsI.CLERIC) {
			retValue = CharacterClass.CLERIC;
		} else {
			errorLogger.warning("No Class chosen");
			retValue = CharacterClass.FIGHTER;
		}

		return retValue;
	}

	public void setLogSuffix(String suffix) {
		worldIdentifierString = suffix;
		if (logger != null)
			logger.rename(name + "_" + getUniqueID() + "_" + suffix);

		log(String.format("STR:%d DEX:%d CON:%d INT:%d WIS:%d CHA:%d", 
				strength.getValue(),
				dexterity.getValue(),
				constitution.getValue(),
				intelligence.getValue(),
				wisdom.getValue(),
				charisma.getValue()));

	}

	public void die(String reason) {
		if (isDead()) return;

		try {
			getLock();

			if (party != null && !party.isDead())  {
				party.addGold(getGold());
				addGold(-getGold());
				for(Artefact a : getInventory()) {
					party.addItem(a);
				}
			}

			super.die(reason);

			Character partyLeader = null;
			if (party != null) partyLeader = party.getLeader();
			if (partyLeader == this) {
				party.setLeader(null);
				if (race == Race.HUMAN) {
					party.chooseLeader();
				}
			}

			hp = -10;

			spellsInAction = new ArrayList<Spell>(0);

			updateChanged();

			if (race.equals(Race.HUMAN)) 
				characterWriter.write(this, worldIdentifierString);

		} finally {
			releaseLock();
		}
	}

	public String toString() {
		return String.format("%s [%d], level %d %s %s", name, getUniqueID(), level, race, chrClass);
	}
	public String getType() {
		return chrClass.toString();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		if (name.length()>200) name = name.substring(0, 200);
		this.name = name;
	}

	/* note that this is always called from the Agent.decide() method
	 * (non-Javadoc)
	 * @see hopshackle.simulation.Agent#maintenance()
	 */
	public void maintenance() {
		super.maintenance();

		if (!initialised) {
			errorLogger.severe("Character in maintenance is not yet initialised: " + toString());
			return;
		}
		long newMaintenance = getWorld().getCurrentTime();
		double subsistence =  ((newMaintenance - lastMaintenance) /(1000.0) * level);
		if (subsistence > 0.0) {
			lastMaintenance = newMaintenance;
			addGold(-subsistence);
		}
		if (gold < -getLevel()*5) {
			// assume a credit limit of 5 gp per level
			gold = 0;
			die("Goes bankrupt");
		}
		subdualHp = 0;

		updateChanged();
		return;
	}

	public Decider getPartyDecider() {
		return partyDecider;
	}

	public void setPartyDecider(Decider partyDecider) {
		this.partyDecider = partyDecider;
	}

	public TradeDecider getTradeDecider() {
		return tradeDecider;
	}

	public void setTradeDecider(TradeDecider tradeDecider) {
		this.tradeDecider = tradeDecider;
	}

	public Party getParty() {
		return party;
	}

	public void setParty(Party party) {
		this.party = party;
	}

	public double getLevelStdDev() {
		return 0.0;
	}

	public int getSize() {
		return 1;
	}

	public ArrayList<Character> getMembers() {
		ArrayList<Character> temp = new ArrayList<Character>();
		temp.add(this);
		return temp;
	}

	public void log(String s) {
		try {
			if (!s.startsWith(partyTestString))
				super.log(s);
			if (party != null && !s.startsWith("Party")) {
				party.log(String.valueOf(getUniqueID())+ ": " + s);
			}
			if (getCurrentFight() != null && !s.startsWith("Party"))
				getCurrentFight().log(this.toString() + ": " + s);
		} catch (NullPointerException e) {
			e.printStackTrace();
			errorLogger.severe("Error in character log method: " + e.toString());
		}
	}

	public double getWound() {
		return (double)(getMaxHp()-getHp())/(double)(getMaxHp());
	}

	public int getReputation() {return socialReputation;}
	public void addSocialRep(int points) {socialReputation += points;}

	public double getValue(Artefact item) {
		return tradeDecider.getValue(item, this, genome);
	}
	public void addItem(Artefact item) {
		try {
			getLock();
			super.addItem(item);
			if ((item instanceof Armour) || (item instanceof Weapon) || (item instanceof Shield))
				checkInventory();
		} finally {
			releaseLock();
		}
	}
	public boolean removeItem(Artefact item) {
		boolean retValue = false;
		try {
			getLock();
			retValue = super.removeItem(item);
			if ((item instanceof Armour) || (item instanceof Weapon) || (item instanceof Shield))
				checkInventory();
		} finally {
			releaseLock();
		}
		return retValue;
	}
	private void checkInventory() {
		boolean debugLevel = getDebugLocal();
		setDebugLocal(false);

		Fight f = getCurrentFight();
		boolean fightDebugLevel = false;
		if (f != null) { 
			fightDebugLevel = f.getDebug();
			f.setDebug(false);
		}

		combatAgent.checkInventory();

		setDebugLocal(debugLevel);
		if (f != null)
			f.setDebug(fightDebugLevel);
	}

	public void setDebugLocal(boolean debug){
		super.setDebugLocal(debug);
		Fight f = getCurrentFight();
		if (f != null) {
			f.setDebug(debug);
		}
	}

	public void addSkill(Skill s) {
		for (Skill existingSkill : skills) {
			if (existingSkill.getName().equals(s.getName())) return;
		}
		skills.add(s);
	}
	public Skill getSkill(Skill.skills name) {
		for (Skill s : skills) {
			if (s.getName().equals(name)) return s;
		}
		return null;
	}
	public boolean hasSkill(Skill.skills name) {
		if (name == null) return false;
		for (Skill s : skills) {
			if  (s.getName().equals(name)) return true;
		}
		return false;
	}
	public void addSpell(Spell s) {
		for (Spell existingSpell : spells) {
			if (existingSpell.getName().equals(s.getName())) return;
		}
		spells.add(s);
	}
	public Spell getSpell(String name) {
		for (Spell s : spells) {
			if (s.getName().equals(name)) return s;
		}
		return null;
	}
	public boolean hasSpell(String name) {
		if (name == null) return false;
		for (Spell s : spells) {
			if  (s.getName().equals(name)) return true;
		}
		return false;
	}
	public ArrayList<Spell> getSpellList() {
		ArrayList<Spell> retValue = new ArrayList<Spell>();
		for (Spell s : spells) {
			retValue.add(s);
		}
		return retValue;
	}

	public boolean isProficient(Artefact item) {
		if (!(item instanceof Weapon)) {
			return true;
		}
		Weapon w = (Weapon) item;
		Skill.skills requiredSkill = w.getProficiencyGroup();
		for (Skill s: skills) {
			if (s.getName().equals(requiredSkill)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInCombat() {
		return combatAgent.getCurrentFight() != null;
	}

	public void setLocation(Location l) {
		super.setLocation(l);
		if (lastMaintenance == -1) lastMaintenance = getWorld().getCurrentTime();

		// We can only set start times once we know the world the Character is in
	}
	public Armour getArmour() {return combatAgent.getArmour();}
	public Weapon getWeapon() {return combatAgent.getWeapon();}
	public Shield getShield() {return combatAgent.getShield();}
	public Fight getCurrentFight() {return combatAgent.getCurrentFight();}
	public void setCombatDecider(Decider cd) {combatAgent.setCombatDecider(cd);}

	public void addCondition(CombatCondition cc) {
		// convenience method
		combatAgent.addCondition(cc);
	}
	public ArrayList<CombatCondition> getCombatConditions() {return combatAgent.getCombatConditions();}
	public CombatAgent getCurrentTarget() {return combatAgent.getCurrentTarget();}
	public CombatModifier getCurrentCM() {return combatAgent.getCurrentCM();}
	public boolean isTargetted() {return combatAgent.isTargetted();}
	public boolean isStunned() {return combatAgent.isStunned();}

	public double getScore() {
		return xp;
	}
	public double getMaxScore() {
		return 10000;
	}

	private void updateChanged() {
		/*
		 * we have a small problem in that setChanged() is sychronized
		 *  and we will may aready have a lock by another thread on the Character
		 *  so to be safe we check for this.
		 *  
		 */
		if (tryLock()) {
			try {
				setChanged();
				notifyObservers();

			} finally {
				releaseLock();
			}
		}
	}

	public CombatAgent getCombatAgent() {
		return combatAgent;
	}

	public void setCombatAgent(CharacterCombatAgent combatAgent) {
		this.combatAgent = combatAgent;
	}
	public void spellCast(Spell newSpell) {
		spellsInAction.add(newSpell);
	}
	public boolean spellExpires(Spell oldSpell) {
		if (spellsInAction.contains(oldSpell)) {
			spellsInAction.remove(oldSpell);
			return true;
		}
		return false;
	}
	public ArrayList<Spell> getActiveSpells() {
		ArrayList<Spell> retValue = new ArrayList<Spell>();
		for (Spell s : spellsInAction) 
			retValue.add(s);
		return retValue;
	}

	public Decider getCombatDecider() {
		return this.combatAgent.getCombatDecider();
	}
	public Decider getCareerDecider() {
		return careerDecider;
	}
	public void setCareerDecider(Decider careerDecider) {
		this.careerDecider = careerDecider;
	}


	private void resetSpellChoice() {
		if (chrClass == CharacterClass.CLERIC) {
			// reset spells
			spells.clear();
			CharacterClass.CLERIC.setSpells(this);
		}
	}

	public static void setAgentWriter(AgentWriter<Character> newAgentWriter) {
		characterWriter = newAgentWriter;
	}
}

