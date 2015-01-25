package hopshackle.simulation.basic;

import hopshackle.simulation.AgentDAO;

public class VillageDAO implements AgentDAO<Village> {

	@Override
	public String getTableCreationSQL(String tableSuffix) {
		return  "CREATE TABLE IF NOT EXISTS Villages_" + tableSuffix +
		" ( id 		INT			PRIMARY KEY,"		+
		" name 		VARCHAR(50)	NOT NULL, "	+
		" founded	BIGINT		NOT NULL,"		+
		" destroyed BIGINT		NOT NULL,"		+
		" location 	VARCHAR(50)	NOT NULL,"		+
		" leaders	INT			NOT NULL,"		+
		" leaderIds VARCHAR(200) NOT NULL"	+
		");";
	}

	@Override
	public String getTableDeletionSQL(String tableSuffix) {
		return "DROP TABLE IF EXISTS Villages_" + tableSuffix + ";";
	}

	@Override
	public String getTableUpdateSQL(String tableSuffix) {
		return "INSERT INTO Villages_" + tableSuffix + 
		" (id,  name, founded, destroyed, location, leaders, leaderIds) VALUES"; 
	}

	@Override
	public String getValuesForAgent(Village village) {
		return String.format(" (%d, '%s', %d, %d, '%s', %d, '%s')",
				village.getId(),
				village.toString(),
				village.getFoundationDate(),
				village.getWorld().getCurrentTime(),
				village.getParentLocation().toString(),
				village.getLeaders().size(),
				village.getLeaders().toString()
		);
	}

}
