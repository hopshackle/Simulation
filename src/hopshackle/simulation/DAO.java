package hopshackle.simulation;

public interface DAO<T> {

	String getTableCreationSQL(String tableSuffix);

	String getTableUpdateSQL(String tableSuffix);
	
	String getValues(T agent);
	
	String getTableDeletionSQL(String tableSuffix);
}
