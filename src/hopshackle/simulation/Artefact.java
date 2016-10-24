package hopshackle.simulation;

import java.util.List;

public interface Artefact {

	public int getMakeDC();
	public Recipe getRecipe();
	public double costToMake(Agent a);
	public long getTimeToMake(Agent a);
	public boolean isA(Artefact item);
	public void changeOwnership(Agent newOwner);
	public boolean isInheritable();
}
