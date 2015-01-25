package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.NeuralDecider;

import org.encog.neural.NeuralNetworkError;
import org.encog.neural.data.basic.BasicNeuralData;
import org.encog.neural.networks.BasicNetwork;
import org.junit.*;

public class NNFactoryTest {

	
	@Before
	public void setUp() {
		// nothing yet
	}

	@Test
	public void threeLayer() {
		int[] layers = {3, 2, 1};
		BasicNetwork n = NeuralDecider.newFFNetwork(layers);
		
		assertTrue (n instanceof BasicNetwork);
		assertEquals(n.calculateNeuronCount(), 6, 0);
		
		// if I now put in a test input of 3, I should get a test output of 2
		BasicNeuralData input = new BasicNeuralData(new double[]{0.5, 0.5, 0.75});
		
		BasicNeuralData output = (BasicNeuralData)n.compute(input);
		double[] outArray = output.getData();
		
		assertEquals(outArray.length, 1);
		
		// If I try a test input of 2 I should get an error
		input = new BasicNeuralData(new double[]{0.5, 0.5});
		
		output = null;
		try {
		output = (BasicNeuralData)n.compute(input);
		assertTrue(false);
		} catch (NeuralNetworkError e){
			assertTrue(true);
		} 
		assertTrue(output == null);
	}
	
	
	
	@After
	public void tearDown() {
		// for later
	}
}
