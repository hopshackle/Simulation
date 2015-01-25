package hopshackle.simulation;

public interface AgentDAO<T extends Persistent> {

	String getTableCreationSQL(String tableSuffix);

	String getTableUpdateSQL(String tableSuffix);
	
	String getValuesForAgent(T agent);
	
	String getTableDeletionSQL(String tableSuffix);
}
