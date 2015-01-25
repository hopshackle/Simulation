package hopshackle.simulation.dnd;

public class DamageReduction implements CombatCondition {

	private Character holder;
	private int DR;

	public DamageReduction(Character target, int reduction) {
		DR = reduction;
		holder = target;
	}

	@Override
	public void apply(CombatModifier cm) {
		// size modifier is applied to the AC of the Defender
		if (cm.getDefender().getAgent().equals(holder)) {
			if (!cm.isMagicAttack())
				cm.setDamage(cm.getDamage()-DR);
		}
	}

	@Override
	public boolean isPermanent() {
		return true;
	}

	@Override
	public int roundsLeft() {
		return Integer.MAX_VALUE;
	}

}
