package hopshackle.simulation;

public class Temperature {

	private double currentTemp;
	private double startTemp;
	private double endTemp;

	public Temperature (double start, double end) {
		startTemp = start;
		endTemp = end;

		currentTemp = start;
	}
	/**
	 * time must be between 0.0 and 1.0. 
	 * Temperature will be graded over this period
	 * with start Temp at 0.0, to end Temp at 1.0
	 * 
	 * @param time
	 */
	public void setTime(double time) {
		if (time < 0.0) time = 0.0;
		if (time > 1.0) time = 1.0;

		currentTemp = startTemp + time * (endTemp - startTemp);
	}
	public double getTemperature() {
		return currentTemp;
	}


}
