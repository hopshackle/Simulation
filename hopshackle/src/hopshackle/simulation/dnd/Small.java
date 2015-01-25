package hopshackle.simulation.dnd;

public class Small implements CombatCondition {

	private Character target;
	
	public Small(Character smallPerson) {
		target = smallPerson;
		target.addCondition(this);
	}
	
	public void apply(CombatModifier cm) {
		// size modifier is applied to the AC of the Defender
		if (cm.getDefender().getAgent().equals(target)) {
			cm.setSize(cm.getSize()+1);
		}
		if (cm.getAttacker().getAgent().equals(target)) {
			cm.setSize(cm.getSize()-1);
		}
	}

	public boolean isPermanent() {
		return true;
	}

	public int roundsLeft() {
		return Integer.MAX_VALUE;
	}

}
