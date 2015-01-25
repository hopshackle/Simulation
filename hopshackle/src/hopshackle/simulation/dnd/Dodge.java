package hopshackle.simulation.dnd;


public class Dodge extends Skill implements CombatCondition {

	public Dodge (Character c) {
		super (c, skills.DODGE);
		c.addCondition(this);
	}

	public void apply(CombatModifier cm) {
		CombatAgent spCombatAgent = skillPossessor.getCombatAgent();
		if (cm.getDefender() == spCombatAgent 
				&& cm.getAttacker() == spCombatAgent.getCurrentTarget()) {
			cm.setDodge(cm.getDodge()+1);
			// Assume that you always dodge the person you are fighting
		}
	}

	public boolean isPermanent() {
		return true;
	}

	public int roundsLeft() {
		return Integer.MAX_VALUE;
	}
}
