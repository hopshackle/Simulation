package hopshackle.simulation;

public interface AgentDataExtractor {

	public void initialise();
	
	public void initialiseLoopVariables();
	
	public void extractDataFrom(Agent sampledAgent);
	
	public void dataExtractFinishedForPeriod(int period);
	
	public void closeDown();
	
}
