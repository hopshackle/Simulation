package hopshackle.simulation.dnd;

import hopshackle.simulation.Dice;

public class CureModerateWounds extends Spell {

	public CureModerateWounds() {
		level = 2;
		name = "Cure Moderate Wounds";
	}
	public void implementEffect(Character caster, DnDAgent target) {
		if (target instanceof Character) {
			Character c = (Character)target;
			int hpHealed = Dice.roll(2,8) + Math.min((int)c.getLevel(), 10);
			c.addHp(hpHealed, false);
			if (caster != target) {
				Reputation.HEAL_FELLOW.apply(caster, hpHealed);
			}
		}
	}
	public boolean isActive() {return false;}
}
