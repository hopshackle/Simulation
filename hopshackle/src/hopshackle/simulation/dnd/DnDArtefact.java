package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

public interface DnDArtefact extends Artefact {
	
	public double getMagicAttack();
	public int getHitBonus(Agent owner);
	public double getOneOffHeal();
	public int getACChange(Agent owner);
	public double getAvgDmgChange(Agent owner);

}
