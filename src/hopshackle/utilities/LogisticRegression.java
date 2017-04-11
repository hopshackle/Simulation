package hopshackle.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import org.encog.neural.data.basic.BasicNeuralDataSet;

/**
 * Performs simple logistic regression.
 * User: tpeng
 * Date: 6/22/12
 * Time: 11:01 PM
 * 
 * @author tpeng
 * @author Matthieu Labas
 */
public class LogisticRegression {

	boolean debug = false;
	
	/** the learning rate */
	private double rate;

	/** the weight to learn */
	private double[] weights;

	/** the number of iterations */
	private int ITERATIONS = 3000;

	/*
	 * n is the number of features 
	 */
	public LogisticRegression() {
		this.rate = 0.0001;
	}

	private static double sigmoid(double z) {
		return 1.0 / (1.0 + Math.exp(-z));
	}

	public void train(BasicNeuralDataSet data, int labelIndex) {
		// BasicNeuralDataSet is a list, each element of which is a pair of arrays, for input and output
		// Any given LogisticRegression applies  to a single label..which is referenced by the labelIndex
		List<Instance> convertedData = new ArrayList<Instance>();
		for (int i = 0; i < data.getRecordCount(); i++) {
			double[] input = data.get(i).getInputArray();
			double label = data.get(i).getIdealArray()[labelIndex];
			convertedData.add(new Instance(label, input));
		}
		weights = new double[data.getInputSize()];
		train(convertedData);
	}
	
	public double train(List<Instance> instances) {
		double lik = 0.0;
		for (int n=0; n<ITERATIONS; n++) {
			lik = 0.0;
			for (int i=0; i<instances.size(); i++) {
				double[] x = instances.get(i).x;
				double predicted = classify(x);
				double label = instances.get(i).label;
				for (int j=0; j<weights.length; j++) {
					weights[j] = weights[j] + rate * (label - predicted) * x[j];
				}
				// not necessary for learning
				lik += label * Math.log(classify(x)) + (1-label) * Math.log(1- classify(x));
			}
			if (debug && n%500 == 0) System.out.println("iteration: " + n + " " + Arrays.toString(weights) + " mle: " + lik);
		}
		return lik;
	}

	public double classify(double[] x) {
		double logit = .0;
		for (int i=0; i<weights.length;i++)  {
			logit += weights[i] * x[i];
		}
		return sigmoid(logit);
	}

	public static class Instance {
		public double label;
		public double[] x;

		public Instance(double label, double[] x) {
			this.label = label;
			this.x = x;
		}
	}

	public static List<Instance> readDataSet(String file) throws FileNotFoundException {
		List<Instance> dataset = new ArrayList<Instance>();
		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(file));
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("#")) {
					continue;
				}
				String[] columns = line.split("\\s+");

				// skip first column and last column is the label
				int i = 1;
				double[] data = new double[columns.length-2];
				for (i=1; i<columns.length-1; i++) {
					data[i-1] = Integer.parseInt(columns[i]);
				}
				double label = Double.parseDouble(columns[i]);
				Instance instance = new Instance(label, data);
				dataset.add(instance);
			}
		} finally {
			if (scanner != null)
				scanner.close();
		}
		return dataset;
	}

}
