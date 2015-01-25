package hopshackle.GUI;
import static org.junit.Assert.*;

import org.junit.Test;
public class RealTimeLBChart2DTest {

	@Test
	public void checkNewValuesAvailable() {
		double[][] testArray = new double[100][1];
		for (int loop = 0; loop < 100; loop++)
			testArray[loop][0] = Double.NaN;
		RealTimeLBChart2D testGraph = new RealTimeLBChart2D("test", new String[] {"test"}, testArray, 20, 5);
		
		assertFalse(testGraph.newValuesExist());
		
		testArray[0][0] = 100.0;
		
		assertTrue(testGraph.newValuesExist());
	}
}
