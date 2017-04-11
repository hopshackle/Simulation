package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;

import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.junit.*;

import hopshackle.utilities.*;

public class RegressionTests {
	
	double[][] x;
	double[] y;

	@Before
	public void setUp() throws Exception {
		x = new double[10][3];
		double[] x0 = {1.0, 5.0, 2.0};
		double[] x1 = {2.0, 5.0, 3.0};
		double[] x2 = {1.0, 10.0, 4.0};
		double[] x3 = {2.0, 10.0, 5.0};
		double[] x4 = {1.0, 15.0, 6.0};
		double[] x5 = {2.0, 15.0, 7.0};
		double[] x6 = {1.0, 25.0, 8.0};
		double[] x7 = {2.0, 25.0, 9.0};
		double[] x8 = {1.0, 50.0, 12.0};
		double[] x9 = {2.0, 50.0, 16.0};
		x[0] = x0; x[1] = x1; x[2] = x2 ; x[3] = x3; x[4] = x4; x[5] = x5;
		x[6] = x6; x[7] = x7; x[8] = x8; x[9] = x9;
		
		double[] y0 = {3.18, 6.18, 3.48, 6.48, 3.65, 6.65, 3.88, 6.88, 4.18, 7.18};
		y = y0;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void simpleLinearRegression() {
		LinearRegression lr = new LinearRegression(x, y);
		
		assertEquals(lr.getWeights().length, 4);
		assertEquals(lr.getWeights()[0], 0.27925, 0.001);
		assertEquals(lr.getWeights()[1], 2.86369, 0.001);
		assertEquals(lr.getWeights()[2], -0.000675, 0.001);
		assertEquals(lr.getWeights()[3], 0.08519, 0.001);
		assertEquals(lr.getError(), 0.076518, 0.001);
	}
	
	@Test
	public void linearRegressionWithThreeOutputs() {
		double[] y1 = {3, 6, 3, 6, 4, 5, 4, 7, 4, 7};
		double[] y2 = {8.71, 10.71, 11.5, 13.5, 13.6, 15.6, 17, 19, 23.2, 25};
		double[][] ideal = new double[10][3];
		for (int i = 0; i < 10; i++) {
			ideal[i][0] = y[i];
			ideal[i][1] = y1[i];
			ideal[i][2] = y2[i];
		}
		BasicNeuralDataSet data = new BasicNeuralDataSet(x, ideal);
		
		LinearRegression lr = LinearRegression.createFrom(data, 0);
		
		assertEquals(lr.getWeights().length, 4);
		assertEquals(lr.getWeights()[0], 0.27925, 0.001);
		assertEquals(lr.getWeights()[1], 2.86369, 0.001);
		assertEquals(lr.getWeights()[2], -0.000675, 0.001);
		assertEquals(lr.getWeights()[3], 0.08519, 0.001);
		assertEquals(lr.getError(), 0.076518, 0.001);
		
		lr = LinearRegression.createFrom(data, 2);
		
		assertEquals(lr.getWeights().length, 4);
		assertEquals(lr.getWeights()[0], 6.58207, 0.001);
		assertEquals(lr.getWeights()[1], 1.06527, 0.001);
		assertEquals(lr.getWeights()[2], 0.1703, 0.001);
		assertEquals(lr.getWeights()[3], 0.5592, 0.001);
		assertEquals(lr.getError(), 4.24134, 0.001);
	}
	
	@Test
	public void simpleLogisticRegression() {
		double[] log_x = {0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 1.75, 2, 2.25, 2.5, 2.75, 3, 3.25, 3.5, 4, 4.25, 4.5, 4.75, 5, 5.5};
		double[] log_y = {0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1};
		x = new double[20][2];
		double[][] ideal = new double[20][2];
		for (int i = 0; i < 20; i ++) {
			x[i][0] = 1.0;
			x[i][1] = log_x[i];
			ideal[i][0] = log_y[i];
			ideal[i][1] = 1.0 - log_y[i];
		}
		BasicNeuralDataSet data = new BasicNeuralDataSet(x, ideal);
		LogisticRegression logR1 = new LogisticRegression();
		logR1.train(data, 0);
		LogisticRegression logR2 = new LogisticRegression();
		logR2.train(data, 1);
		
		double[] testData = {1.0, 1.87};
		assertEquals(logR1.classify(testData), 0.52, 0.02);
		assertEquals(logR2.classify(testData), 0.48, 0.02);
		testData[0] = 1.0;
		testData[1] = 7.0;
		assertEquals(logR1.classify(testData), 0.90, 0.02);
		assertEquals(logR2.classify(testData), 0.1, 0.02);
	}

}
