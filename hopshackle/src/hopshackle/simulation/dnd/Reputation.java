package hopshackle.simulation.dnd;

public enum Reputation {
	
	// General principle is that a hit point = 1 Rep
	// And that any combat last four more rounds from point of buff
	// and that to hit chance = 25%
	// and that avg damage per hit = 5hp
	
	CHALLENGE			(-0.5),
	KILL_PARTY_MEMBER	(-50),
	HEAL_FELLOW			(1),
	DAMAGE_FELLOW		(-1),
	DAMAGE_ENEMY		(1),
	BUFF_AC				(1),
	BUFF_ATTACK			(1),
	BUFF_DAMAGE			(1),
	GOLD_GAIN_WHILE_LEADER (0.1),
	XP_GAIN_WHILE_LEADER (0.01),
	DEATH_WHILE_LEADER	(-20);
	
	private double effect;
	
	Reputation(double effect) {
		this.effect = effect;
	}
	
	public void apply(Character c) {
		apply(c, 1);
	}
	public void apply(Character c, int times) {
		c.addSocialRep((int)(effect * times));
	}
}

