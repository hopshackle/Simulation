package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.List;

public class BasicAgentDAO implements DAO<BasicAgent> {

	@Override
	public String getTableCreationSQL(String tableSuffix) {

		return  "CREATE TABLE IF NOT EXISTS AllAgents_" + tableSuffix +
		" ( id 		INT			PRIMARY KEY,"		+
		" name 		VARCHAR(50)		NOT NULL, "	+
		" surname	VARCHAR(50)		NOT NULL, " +
		" age		INT			NOT NULL,"		+
		" birth		BIGINT		NOT NULL,"		+
		" Generation INT		NOT NULL,"		+
		" Gender VARCHAR(1)		NOT NULL,"		+
		" Father	INT			NOT NULL,"		+
		" Mother	INT			NOT NULL,"		+
		" BirthLocation VARCHAR(50) NOT NULL, "	+
		" DeathLocation VARCHAR(50) NOT NULL, "	+
		" NetMovement	INT			NOT NULL,"		+
		" TotalMovement	INT			NOT NULL,"		+
		" Children	INT			NOT NULL,"		+
		" ChildrenIds VARCHAR(200) NOT NULL,"	+
		" ParentGenome	VARCHAR(50)	NOT NULL,"	+
		" Food		INT			NOT NULL,"	+
		" Wood		INT			NOT NULL,"	+
		" Huts		INT			NOT NULL"	+
		");";
	}
	
	@Override
	public String getTableDeletionSQL(String tableSuffix) {
		return "DROP TABLE IF EXISTS AllAgents_" + tableSuffix + ";";
	}

	@Override
	public String getTableUpdateSQL(String lastSuffix) {
		return "INSERT INTO AllAgents_" + lastSuffix + 
				" (id,  name, surname, age, birth, Generation, Gender, Father, Mother, Children, ChildrenIds, ParentGenome, Food, Wood, Huts, " +
				"BirthLocation, DeathLocation, NetMovement, TotalMovement) VALUES";
	}

	@Override
	public String getValues(BasicAgent agent) {

		Long father, mother;
		father = 0l;
		mother = 0l;
		List<Long> parents = agent.getParents();
		if (parents.size() > 0) {
			father = parents.get(0);
			if (parents.size() > 1)
				mother = parents.get(1);
		}
		List<Agent> children = agent.getChildren();
		StringBuffer childrenIds = Agent.convertToStringVersionOfIDs(children);
		
		int numberOfHuts = agent.getNumberInInventoryOf(BuildingType.HUT);
		int amountOfFood = agent.getNumberInInventoryOf(Resource.FOOD);
		int amountOfWood = agent.getNumberInInventoryOf(Resource.WOOD);
		
		Location birthLocation = agent.getBirthLocation();
		Location deathLocation = agent.getLocation();
		int netMovement = 0;
		if (birthLocation != null && deathLocation != null) {
			List<Location> lifeRoute = AStarPathFinder.findRoute(agent, birthLocation, deathLocation, null);
			netMovement = lifeRoute.size();
		}

		return String.format(" (%d, '%s', '%s', %d, %d, %d, '%s', %d, %d, %d, '%s', '%s', %d, %d, %d, '%s', '%s', %d, %d)",
				agent.getUniqueID(),
				agent.toString(),
				agent.getSurname(),
				agent.getAge(),
				agent.getBirth(),
				agent.getGeneration(),
				agent.isMale() ? "M" : "F",
				father,
				mother,
				children.size(),
				childrenIds.toString(),
				agent.getGenome().getName(),
				amountOfFood,
				amountOfWood,
				numberOfHuts,
				birthLocation!=null ? birthLocation.toString() : "NULL",
				deathLocation!=null ? deathLocation.toString() : "NULL",
				netMovement,
				agent.getMovementPointsSpent()
		);
	}
}
