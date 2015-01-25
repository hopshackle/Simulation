package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.ArrayList;

import org.junit.Test;
public class ScoreByXPTest {

	@Test
	public void testScoreByXP() {
		Character c = new Character(new World());
		c.addXp(1000);
		
		ScoreByXP t = new ScoreByXP();
		
		assertEquals(t.getScore(c), 1000, 0.001);
	}
	
	@Test
	public void testAgentExemplar() {
		World w = new World();
		ScoringFunction score = new ScoreByXP();
		
		ArrayList<Character> agentList = new ArrayList<Character>();
		Agent sample = AgentExemplar.getExemplar(agentList, 5, score);

		assertTrue(sample == null);
		
		Character c1 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		Character c2 = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
		
		c1.addXp(1000);
		
		agentList.add(c2);
		sample = AgentExemplar.getExemplar(agentList, 5, score);
		
		assertTrue (sample == c2);
		
		agentList.add(c1);
		sample = AgentExemplar.getExemplar(agentList, 10, score);
		
		assertTrue (sample == c1);
	}
}
