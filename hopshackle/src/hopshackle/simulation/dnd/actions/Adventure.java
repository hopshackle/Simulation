package hopshackle.simulation.dnd.actions;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.io.*;
import java.util.ArrayList;

public class Adventure extends Action {

	int CR;
	int treasureCR;
	boolean debug = false;
	static ArrayList<ArrayList<EncounterDetail>> encounterStatic;
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	static {
		// read in random encounter details
		File encounterFile = new File(baseDir + "\\Genomes\\Encounters.txt");
		// now open file, and read each line
		// each line will contain CR of encounter, Number of enemies, Race of enemy, Level of enemy

		// this will be stored in an ArrayList, with one per CR.
		encounterStatic = new ArrayList<ArrayList<EncounterDetail>>(21);
		for (int n = 0; n<21; n++) 
			encounterStatic.add(new ArrayList<EncounterDetail>());

		try
		{
			FileReader fr = new FileReader(encounterFile);
			BufferedReader br = new BufferedReader(fr);
			String nextEncounter = br.readLine();
			while (nextEncounter!=null)
			{
				EncounterDetail ed = new EncounterDetail();

				String temp[] = nextEncounter.split(",");
				ed.CR = Integer.valueOf(temp[0]);
				ed.number = Integer.valueOf(temp[1]);
				ed.race = Race.getRace(temp[2]);
				ed.level = Integer.valueOf(temp[3]);

				ArrayList<EncounterDetail> encounterListForCR = encounterStatic.get(ed.CR);
				encounterListForCR.add(ed);

				nextEncounter = br.readLine();
			}	
			br.close();
		}
		catch (Exception e)
		{
			System.err.println("File input error "+e.toString());
			logger.severe("Error in reading Encounter Static:" + e.toString());
		}
	}

	public Adventure(DnDAgent c, boolean recordAction) {
		super(c, recordAction);
		Square s = (Square) c.getLocation();
		CR = s.getX() + s.getY();
		if (CR == 0) startTime = c.getWorld().getCurrentTime() + 100;
		treasureCR = CR;
	}
	public Adventure(DnDAgent c, long start, int treasureMod, boolean recordAction) {
		super(c, start, recordAction);
		Square s = (Square) c.getLocation();
		CR = s.getX() + s.getY();
		if (CR == 0) startTime = c.getWorld().getCurrentTime() + 100;
		// If in CR = 0 area, then Adventuring is meaningless.
		treasureCR = CR + treasureMod;
	}

	protected void doStuff() {	
		if (actor.isDead()) return;
		boolean atLeastOneAlive = false;
		for (Character c : ((DnDAgent)actor).getMembers()) {
			if (!c.isDead()) atLeastOneAlive = true;
		}
		if (!atLeastOneAlive) return;

		ArrayList<Artefact> treasure = new ArrayList<Artefact>();

		if (CR > 0) {
			Party enemyGroup = null;

			// so first we pick one of the possible encounters randomly
			int diceToRoll = encounterStatic.get(CR).size();
			int roll = Dice.roll(1,diceToRoll)-1;
			EncounterDetail encounter = encounterStatic.get(CR).get(roll);

			// then we iterate over the number of characters, create them, stick them in the party
			// level them up and equip them
			for (int n=0; n< encounter.number; n++) {
				switch (encounter.race) {
				case ORC:
					treasure.add(Component.ORC_HEART);
					break;
				case GRICK:
					treasure.add(Component.GRICK_BEAK);
					break;
				}

				Character monster = new Character(encounter.race, CharacterClass.WARRIOR, actor.getWorld());
				monster.setLocation(actor.getLocation());
				for (int m=1; m<encounter.level; m++) 
					monster.levelUp();
				if (encounter.race == Race.ORC || encounter.race == Race.GOBLIN)
					equip(monster);	// only applies to humanoid foes
				
				if (enemyGroup == null) {
					enemyGroup = new Party(monster);
				} else enemyGroup.addMember(monster);
			}

			Fight f = new Fight((DnDAgent)actor, enemyGroup);
			f.resolve();

			for (Character monster : enemyGroup.getMembers()) {
				enemyGroup.removeMember(monster);
				if (!monster.isDead()) monster.die("Of old age");
			}
			enemyGroup.maintenance();
		}

		if (!actor.isDead()) {
			if (CR>0) {
				((DnDAgent)actor).surviveEncounter(CR);
			}
			if (treasureCR > 0) {
				actor.addGold((int)(Math.pow(treasureCR, 1.25))*25);	
			}
			for (Artefact t : treasure) {
				actor.addItem(t);
			}
		}
	}

	public String toString() {return "ADVENTURE";}

	private void equip(Character monster) {
		monster.addItem(Weapon.LIGHT_MACE);
		if (CR > 2) monster.addItem(Weapon.HEAVY_MACE);
		if (CR > 4) monster.addItem(Weapon.LONG_SWORD);

		switch (CR) {
		case 1:
			monster.addItem(Armour.LEATHER_ARMOUR);
			break;
		case 2:
			monster.addItem(Armour.LEATHER_ARMOUR);
			monster.addItem(Shield.SMALL_SHIELD);
			break;
		case 3: 
			monster.addItem(Armour.STUDDED_LEATHER_ARMOUR);
			monster.addItem(Shield.SMALL_SHIELD);
			break;
		case 4: 
			monster.addItem(Armour.STUDDED_LEATHER_ARMOUR);
			monster.addItem(Shield.LARGE_SHIELD);
			break;
		case 5:
		case 6:
			monster.addItem(Armour.CHAIN_SHIRT);
			monster.addItem(Shield.LARGE_SHIELD);
			break;
		case 7:
			if (monster.getDexterity().getMod()<3) {
				monster.addItem(Armour.CHAIN_MAIL);
			} else {
				monster.addItem(Armour.CHAIN_SHIRT);
			}
			monster.addItem(Shield.LARGE_SHIELD);
			break;
		case 8:
			if (monster.getDexterity().getMod() == 2 
					|| monster.getDexterity().getMod() == 1) {
				monster.addItem(Armour.CHAIN_MAIL);
			} else if (monster.getDexterity().getMod()<1) {
				monster.addItem(Armour.SPLINT_MAIL);
			} else {
				monster.addItem(Armour.CHAIN_SHIRT);
			}
			monster.addItem(Shield.LARGE_SHIELD);
			break;
		case 9:
		case 10:
			if (monster.getDexterity().getMod() == 2) {
				monster.addItem(Armour.CHAIN_MAIL);
			} else if (monster.getDexterity().getMod()<2) {
				monster.addItem(Armour.BANDED_MAIL);
			} else {
				monster.addItem(Armour.CHAIN_SHIRT);
			}
			monster.addItem(Shield.LARGE_SHIELD);
			break;
		case 11:
		case 12:
			if (monster.getDexterity().getMod() == 2) {
				monster.addItem(Armour.CHAIN_MAIL);
			} else if (monster.getDexterity().getMod() == 1) {
				monster.addItem(Armour.BANDED_MAIL);
			} else if (monster.getDexterity().getMod()<1 ) {
				monster.addItem(Armour.HALF_PLATE);
			} else {
				monster.addItem(Armour.CHAIN_SHIRT);
			}
			monster.addItem(Shield.LARGE_SHIELD);
			break;
		default:
			monster.addItem(Armour.FULL_PLATE);
			monster.addItem(Shield.TOWER_SHIELD);
		}
	}
}

class EncounterDetail {
	int CR;
	int number;
	Race race;
	int level;
}


