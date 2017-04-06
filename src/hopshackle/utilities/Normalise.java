package hopshackle.utilities;

public class Normalise {

	public static double[] range(double[] input, double min, double max) {
		double maxInArray = Double.MIN_VALUE;
		double minInArray = Double.MAX_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (input[i] > maxInArray) maxInArray = input[i];
			if (input[i] < minInArray) minInArray = input[i];
		}

		double[] retValue = new double[input.length];
		if (maxInArray == minInArray) {
			// leave everything as 0.0
		} else {
			for (int i = 0; i < input.length; i++) {
				retValue[i] = (input[i] - minInArray) / (maxInArray - minInArray) * (max - min) + min;
			}
		}
		return retValue;
	}

}
