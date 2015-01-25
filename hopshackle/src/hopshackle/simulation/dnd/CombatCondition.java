package hopshackle.simulation.dnd;

public interface CombatCondition {

	void apply(CombatModifier cm);
	
	boolean isPermanent();
	
	int roundsLeft();
}

