package hopshackle.simulation;

/**
 * Used to find a known Location that meets certain criteria defined in the implementing concrete object
 * 
 * The one method to be implemented is matches(Location loc), which returns true or false
 * @author James
 *
 */
public interface GoalMatcher<T extends Location> {
	
	public boolean matches(T state);
	
	public boolean supercedes(GoalMatcher<T> competitor);

}
