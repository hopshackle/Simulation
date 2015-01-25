package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.genetics.*;

import java.util.ArrayList;

import org.junit.*;
public class GeneMutabilityTest {

	private Gene testAttackGene;
	private Gene testRetreatGene;
	
	@Before
	public void setup() {
		SimProperties.clear();
		ArrayList<GeneticVariable> combatVariables = new ArrayList<GeneticVariable>();
		for (GeneticVariable gv : CombatNeuronInputs.values())
			combatVariables.add(gv);
		testAttackGene = new Gene(CombatActionsI.ATTACK, combatVariables);
		testRetreatGene = new Gene(CombatActionsI.RETREAT, combatVariables);
	}
	
	@Test
	public void defaultValue() {
		assertTrue(testAttackGene.isMutable());
		assertTrue(testRetreatGene.isMutable());
		
		assertTrue(testAttackGene.mutate());
		assertTrue(testRetreatGene.mutate());
	}
	
	@Test
	public void globalSetting() {
		SimProperties.setProperty("GeneticMutation", "false");
		assertTrue(!testAttackGene.isMutable());
		assertTrue(!testRetreatGene.isMutable());
		
		assertFalse(testAttackGene.mutate());
		assertFalse(testRetreatGene.mutate());
	}
	
	@Test
	public void localOverrideToTrue(){
		SimProperties.setProperty("GeneticMutation", "false");
		SimProperties.setProperty("GeneticMutation.ATTACK", "true");
		assertTrue(testAttackGene.isMutable());
		assertTrue(!testRetreatGene.isMutable());
		
		assertTrue(testAttackGene.mutate());
		assertFalse(testRetreatGene.mutate());
	}
	
	@Test
	public void localOverrideToFalse(){
		SimProperties.setProperty("GeneticMutation", "true");
		SimProperties.setProperty("GeneticMutation.ATTACK", "false");
		assertTrue(!testAttackGene.isMutable());
		assertTrue(testRetreatGene.isMutable());
		
		assertFalse(testAttackGene.mutate());
		assertTrue(testRetreatGene.mutate());
	}
}
