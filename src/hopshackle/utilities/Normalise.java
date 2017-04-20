package hopshackle.utilities;

public class Normalise {

	public static double[] range(double[] input, double min, double max) {
		double maxInArray = Double.MIN_VALUE;
		double minInArray = Double.MAX_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (Double.isNaN(input[i])) continue;
			if (input[i] > maxInArray) maxInArray = input[i];
			if (input[i] < minInArray) minInArray = input[i];
		}

		double[] retValue = new double[input.length];
		for (int i = 0; i < retValue.length; i++) retValue[i] = Double.NaN;
		if (maxInArray == minInArray) {
			// leave everything as 0.0
		} else {
			for (int i = 0; i < input.length; i++) {
				if (Double.isNaN(input[i])) continue;
				retValue[i] = (input[i] - minInArray) / (maxInArray - minInArray) * (max - min) + min;
			}
		}
		return retValue;
	}
	
	public static double[] asProbabilityDistribution(double[] input) {
		double[] retValue = new double[input.length];
		double inputTotal = 0.0;
		for (int i = 0; i < input.length; i++) {
			if (Double.isNaN(input[i])) continue;
			inputTotal += input[i];
		}
		for (int i = 0; i < input.length; i++) {
			if (Double.isNaN(input[i])) continue;
			retValue[i] = input[i] / inputTotal;
		}
		return retValue;
	}

}
