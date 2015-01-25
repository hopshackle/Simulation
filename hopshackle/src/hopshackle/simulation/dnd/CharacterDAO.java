package hopshackle.simulation.dnd;

import hopshackle.simulation.AgentDAO;

import java.util.List;

public class CharacterDAO implements AgentDAO<Character>{
	
	@Override
	public String getTableCreationSQL(String tableSuffix) {

		String sqlQuery = "CREATE TABLE IF NOT EXISTS AllChr_" + tableSuffix +
		" ( id 		INT			PRIMARY KEY,"		+
		" Name		VARCHAR(50)	NOT NULL,"		+
		" Level		INT			NOT NULL,"		+
		" xp		INT			NOT NULL,"		+
		" age		INT			NOT NULL,"		+
		" race		VARCHAR(20)	NOT NULL,"		+
		" birth		BIGINT		NOT NULL,"		+
		" class		VARCHAR(20) NOT NULL,"		+
		" STR		INT			NOT NULL,"		+
		" DEX		INT			NOT NULL,"		+
		" CON		INT			NOT NULL,"		+
		" INTL		INT			NOT NULL,"		+
		" WIS		INT			NOT NULL,"		+
		" CHA		INT			NOT NULL,"		+
		" Generation INT		NOT NULL,"		+
		" Parent1	INT			NOT NULL,"		+
		" Parent2	INT			NOT NULL,"		+
		" Children	INT			NOT NULL,"		+
		" Reputation INT		NOT NULL,"		+
		" ParentGenome	VARCHAR(50)	NOT NULL,"	+
		" CombatDecider VARCHAR(50) NOT NULL,"	+
		" CareerDecider VARCHAR(50) NOT NULL"	+
		");";
		
		return sqlQuery;
	}
	
	@Override
	public String getTableDeletionSQL(String tableSuffix) {
		return "DROP TABLE IF EXISTS AllChr_" + tableSuffix + ";";
	}

	@Override
	public String getTableUpdateSQL(String lastSuffix) {
		return "INSERT INTO AllChr_" + lastSuffix + 
				" (id, name, level, xp, age, race, birth, class, STR, DEX, CON, INTL, WIS, CHA, Generation, Parent1, Parent2, Children, Reputation, " +
		"ParentGenome, CombatDecider, CareerDecider) VALUES";
	}

	@Override
	public String getValuesForAgent(Character c) {
		Long parent1, parent2;
		parent1 = 0l;
		parent2 = 0l;
		List<Long> parents = c.getParents();
		if (parents.size() > 0) {
			parent1 = parents.get(0);
			if (parents.size() > 1)
				parent2 = parents.get(1);
		}

		return String.format(" (%d, '%s', %d, %d, %d, '%s', %d, '%s', %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, '%s', '%s', '%s')",
				c.getUniqueID(),
				c.getName(),
				(int) c.getLevel(),
				c.getXp(),
				c.getAge(),
				c.getRace(),
				c.getBirth(),
				c.getChrClass(),
				c.getStrength().getValue(),
				c.getDexterity().getValue(),
				c.getConstitution().getValue(),
				c.getIntelligence().getValue(),
				c.getWisdom().getValue(),
				c.getCharisma().getValue(),
				c.getGeneration(),
				parent1,
				parent2,
				c.getNumberOfChildren(),
				c.getReputation(),
				c.getGenome().getName(),
				(c.getCombatDecider() != null) ? c.getCombatDecider().toString() : "NULL",
						(c.getCareerDecider() != null) ? c.getCareerDecider().toString() : "NULL");
	}

}
