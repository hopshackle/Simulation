package hopshackle.simulation.dnd;

public abstract class DurationCombatSpell extends Spell {
	
	protected int startRound;
	protected Fight contextFight;
	protected DnDAgent target;
	
	public void implementEffect(Character caster, DnDAgent target) {
		this.target = target;
		contextFight = caster.getCurrentFight();
		if (contextFight == null) 
			contextFight = ((Character)target).getCurrentFight();
		
		startRound = contextFight.getCurrentRound();
	}
	public boolean isPermanent() {
		return false;
	}
	public int roundsLeft() {
		if (!contextFight.isActive()) return 0;
		return duration - (contextFight.getCurrentRound() - startRound);
	}
	public boolean isActive() {
		return (contextFight.isActive() && (roundsLeft() > 0));
		// i.e. fight must still be going on - and the spell must not have expired
	}
	public DnDAgent getTarget() {
		return target;
	}
}
