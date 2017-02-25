package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.NeuralNetworkError;
import org.encog.neural.networks.BasicNetwork;
import org.junit.*;

public class NNFactoryTest {

	DeciderProperties localProp;
	
	@Before
	public void setUp() {
		localProp = SimProperties.getDeciderProperties("GLOBAL");
	}

	@Test
	public void threeLayer() {
		int[] layers = {3, 2, 1};
		BasicNetwork n = BrainFactory.newFFNetwork(layers, localProp);
		
		assertTrue (n instanceof BasicNetwork);
		assertEquals(n.getLayerCount(), 3, 0);
		assertEquals(n.getLayerNeuronCount(0), 3, 0);
		assertEquals(n.getLayerNeuronCount(1), 2, 0);
		assertEquals(n.getLayerNeuronCount(2), 1, 0);
		
		// if I now put in a test input of 3, I should get a test output of 2
		BasicMLData input = new BasicMLData(new double[]{0.5, 0.5, 0.75});
		
		BasicMLData output = (BasicMLData)n.compute(input);
		double[] outArray = output.getData();
		
		assertEquals(outArray.length, 1);
		
		// If I try a test input of 2 I should get an error
		input = new BasicMLData(new double[]{0.5, 0.5});
		
		output = null;
		try {
		output = (BasicMLData)n.compute(input);
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
