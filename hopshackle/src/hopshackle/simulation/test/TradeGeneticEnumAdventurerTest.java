package hopshackle.simulation.test;

import static org.junit.Assert.assertEquals;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.genetics.*;

import java.util.*;

import org.junit.*;
public class TradeGeneticEnumAdventurerTest {

	private Character ftr;
	@Before
	public void setUp() throws Exception {
		ftr = new Character(new World());
		ftr.addGold(10.0);
		ftr.setTradeDecider(new TradeDecider());
		Genome g = ftr.getGenome();
		Gene gene = new Gene("AC|0.00|1.0>* TEN|0.5>* VARIABLE", 
				new ArrayList<GeneticVariable>(EnumSet.allOf(TradeGeneticEnumAdventurer.class)), 
				new ArrayList<ActionEnum>(EnumSet.allOf(TradeValuationsAdventurer.class)));
		Chromosome chromosome = new Chromosome(new ArrayList<ActionEnum>(EnumSet.allOf(TradeValuationsAdventurer.class)),
				new ArrayList<GeneticVariable>(EnumSet.allOf(TradeGeneticEnumAdventurer.class)));
		chromosome.addGene(gene);
		g.setChromosome(chromosome);
	}

	@Test
	public void testBasicEnum() {
		// check the numerals and Gold
		assertEquals(TradeGeneticEnumAdventurer.FIVE.getValue(ftr, 0.00), 5.0, 0.001);
		assertEquals(TradeGeneticEnumAdventurer.TEN.getValue(ftr, 0.00), 10.0, 0.001);
		assertEquals(TradeGeneticEnumAdventurer.FIFTY.getValue(ftr, 0.00), 50.0, 0.001);
		assertEquals(TradeGeneticEnumAdventurer.HUNDRED.getValue(ftr, 0.00), 100.0, 0.001);
		assertEquals(TradeGeneticEnumAdventurer.GOLD.getValue(ftr, 0.00), 10.0, 0.001);
	}

	@Test
	public void testVariableAC() {
		// check that with a Variable Gene, this gives the expected result
		assertEquals(TradeGeneticEnumAdventurer.VARIABLE.getValue(ftr, 1.20), 1.2, 0.001);
		
		assertEquals(ftr.getValue(Armour.LEATHER_ARMOUR), 22.0, 0.001);
		// 20 for AC, plus 0.5 * 2 * 2 for a total of 22.0
	}
}
