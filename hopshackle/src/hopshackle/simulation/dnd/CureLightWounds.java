package hopshackle.simulation.dnd;
import hopshackle.simulation.Dice;

public class CureLightWounds extends Spell {
	
	public CureLightWounds() {
		level = 1;
		name = "Cure Light Wounds";
	}
	public void implementEffect(Character caster, DnDAgent target) {
		if (target instanceof Character) {
			Character c = (Character)target;
			int hpHealed = Dice.roll(1,8) + Math.min((int)c.getLevel(), 5);
			c.addHp(hpHealed, false);
			if (caster != target) {
				Reputation.HEAL_FELLOW.apply(caster, hpHealed);
			}
		}
	}
	public boolean isActive() {return false;}
}
