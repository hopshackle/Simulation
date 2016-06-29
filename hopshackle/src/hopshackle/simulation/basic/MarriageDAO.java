package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.util.List;

public class MarriageDAO implements DAO<Marriage> {

	@Override
	public String getTableCreationSQL(String tableSuffix) {

		return  "CREATE TABLE IF NOT EXISTS AllMarriages_" + tableSuffix +
		" ( id			INT			PRIMARY KEY," +
		" Husband		INT			NOT NULL,"		+
		" Wife			INT			NOT NULL,"		+
		" Start 		BIGINT 		NOT NULL, "	+
		" Duration 		INT 		NOT NULL, "	+
		" Children		INT			NOT NULL,"		+
		" ChildrenIds 	VARCHAR(50) NOT NULL,"	+
		" INDEX (husband), " +
		" INDEX (wife)" +
		");";
	}
	
	@Override
	public String getTableDeletionSQL(String tableSuffix) {
		return "DROP TABLE IF EXISTS AllMarriages_" + tableSuffix + ";";
	}

	@Override
	public String getTableUpdateSQL(String lastSuffix) {
		return "INSERT INTO AllMarriages_" + lastSuffix + 
				" (id, husband, wife, start, duration, children, childrenIds) VALUES";
	}

	@Override
	public String getValues(Marriage marriage) {

		BasicAgent husband = marriage.getSeniorPartner();
		BasicAgent wife = marriage.getPartnerOf(husband);
		if (husband.isFemale()) {
			wife = husband; 
			husband = marriage.getPartnerOf(wife);
		}
		
		List<Agent> children = husband.getChildren();
		List<Agent> herProgeny = wife.getChildren();
		children.retainAll(herProgeny);
		StringBuffer childrenIds = Agent.convertToStringVersionOfIDs(children);
		
		return String.format(" (%d, %d, %d, %d, %d, %d, '%s')",
				marriage.getId(),
				husband.getUniqueID(),
				wife.getUniqueID(),
				marriage.getStartDate(),
				marriage.getDuration(),
				children.size(),
				childrenIds.toString()
		);
	}
}
