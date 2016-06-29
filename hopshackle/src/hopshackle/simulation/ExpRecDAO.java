package hopshackle.simulation;

public class ExpRecDAO implements DAO<ExperienceRecord<?>> {

	@Override
	public String getTableCreationSQL(String tableSuffix) {

		return  "CREATE TABLE IF NOT EXISTS ExperienceRecords_" + tableSuffix +
		" ( id 		INT			PRIMARY KEY	AUTO_INCREMENT,"		+
		" agent 	INT		NOT NULL, "	+
		" startScore FLOAT		NOT NULL, " +
		" endScore		FLOAT			NOT NULL,"		+
		" reward		FLOAT		NOT NULL,"		+
		" startState VARCHAR(200)		NOT NULL,"		+
		" endState VARCHAR(200)		NOT NULL,"		+
		" featureTrace VARCHAR(200)		NOT NULL,"		+
		" actionTaken	VARCHAR(20)			NOT NULL,"		+
		" actionsFromStart VARCHAR(200) NOT NULL, "	+
		" actionsFromEnd VARCHAR(200) NOT NULL, "	+
		" variables	VARCHAR(200)			NOT NULL" +
		");";
	}

	@Override
	public String getTableUpdateSQL(String tableSuffix) {
		return "INSERT INTO ExperienceRecords_" + tableSuffix + 
				" (agent, startScore, endScore, reward, startState, endState, featureTrace, actionTaken, " +
				"actionsFromStart, actionsFromEnd) VALUES";
	}

	@Override
	public String getValues(ExperienceRecord<?> er) {
		
		return String.format(" (%d, %.5f, %.5f, %.5f, '%s', '%s', '%s', '%s', '%s', '%s')",
				er.getAgent().getUniqueID(),
				er.getStartScore(),
				er.getEndScore(),
				er.getReward(),
				HopshackleUtilities.formatArray(er.getStartState(), ",", "%.2f"),
				HopshackleUtilities.formatArray(er.getEndState(), ",", "%.2f"),
				HopshackleUtilities.formatArray(er.getFeatureTrace(), ",", "%.2f"),
				er.getActionTaken().getType(),
				HopshackleUtilities.formatList(er.getPossibleActionsFromStartState(), ",", null),
				HopshackleUtilities.formatList(er.getPossibleActionsFromEndState(), ",", null)
		);
	}

	@Override
	public String getTableDeletionSQL(String tableSuffix) {
		return "DROP TABLE IF EXISTS ExperienceRecords_" + tableSuffix + ";";
	}

}
