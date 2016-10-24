package hopshackle.simulation;

public interface AgentRetriever<T extends Agent> {

	public T getAgent(long uniqueID, String tableSuffix, World world);
	
	public void closeConnection();

	public void openConnection();
}
