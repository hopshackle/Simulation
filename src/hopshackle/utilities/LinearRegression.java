package hopshackle.utilities;

import org.apache.commons.math3.stat.regression.*;
import org.encog.neural.data.basic.BasicNeuralDataSet;

public class LinearRegression {

	/** the weight to learn */
	private double[] weights;
	private OLSMultipleLinearRegression reg = new OLSMultipleLinearRegression();
	
	/*
	 * x is n x k; y is n x 1
	 */
	public LinearRegression(double[][] x, double[] y) {
		reg.newSampleData(y, x);
		weights = reg.estimateRegressionParameters();
	}

	public static LinearRegression createFrom(BasicNeuralDataSet data, int labelIndex) {
		double[][] allData = new double[(int) data.getRecordCount()][data.getInputSize()];
		double[] target = new double[(int) data.getRecordCount()];
		for (int i = 0; i < data.getRecordCount(); i++) {
			allData[i] = data.get(i).getInputArray();
			target[i] = data.get(i).getIdealArray()[labelIndex];
		}
		return new LinearRegression(allData, target);
	}

	/* 
	 * [0] element is always the intercept, so total length is k+1
	 */
	public double[] getWeights() {
		return weights;
	}
	
	public double getError() {
		return reg.calculateResidualSumOfSquares();
	}
}
