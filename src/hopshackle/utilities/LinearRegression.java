package hopshackle.utilities;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.regression.*;
import org.encog.neural.data.basic.BasicNeuralDataSet;

public class LinearRegression {

	/** the weight to learn */
	private double[] weights;
	private double[][] X;
	private double[] Y;
	
	/*
	 * x is n x k; y is n x 1
	 */
	public LinearRegression(double[][] x, double[] y) {
		X = x;
		Y = y;
		DecompositionSolver solver = new SingularValueDecomposition(new Array2DRowRealMatrix(x, false)).getSolver();
		weights = solver.solve(new ArrayRealVector(y)).toArray();
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
		double retValue = 0.0;
		for (int i = 0; i < Y.length; i++) {
			double predicted = predict(X[i]);
			retValue += Math.pow(Y[i] - predicted, 2);
		}
		return retValue / (double) Y.length;
	}
	
	public double predict(double[] x) {
		if (x.length != weights.length) throw new AssertionError("Data not same dimension as weights " + x.length + " vs " + weights.length);
		double retValue = 0.0;
		for (int i = 0; i < x.length; i++) {
			retValue += x[i] * weights[i];
		}
		return retValue;
	}
}
