package hopshackle.simulation;

public interface ScoringFunction {

	<T extends Agent> double getScore(T a);
	
}
