package hopshackle.simulation;

import java.util.Random;

public final class Dice {

	static int lastRoll;
	static int nextRoll;
	private static Random rnd = new Random();

	public static void setSeed(long seed) {
		rnd = new Random(seed);
	}
	
	public static int roll (int n, int max) {
		int total = 0;
		if (nextRoll > 0 && nextRoll < max * n) {
			total = nextRoll;
			nextRoll = 0;
		} else 
			for (int i=0; i<n; i++) total+=rnd.nextInt(max)+1;
		lastRoll = total;
		return total;
	}

	public static int lastRoll() {
		return lastRoll;
	}
	
	public static void setNextRoll(int n) {
		nextRoll = n;
	}
	
	public static int bestOf(int diceType, int diceNumber, int takeHighest) {
		int[] allDice = new int[diceNumber];
		for (int n = 0; n<diceNumber; n++)
			allDice[n] = Dice.roll(1, diceType);
			
		int[] finalDice = new int[takeHighest];
		

		for (int n = 0; n < takeHighest; n++) {
			int currentHigh = 0;
			int index = 0;
			for (int m = 0; m < diceNumber; m++) {
				if (allDice[m] > currentHigh) {
					currentHigh = allDice[m];
					index = m;
				}
			}
			finalDice[n] = allDice[index];
			allDice[index] = 0;
		}
	
		int retValue = 0;
		for (int n : finalDice) 
			retValue+=n;
		
		return retValue;
	}

	public static int stressDieResult() {
		int result = Dice.roll(1, 10) - 1;
		int multiplier = 1;
		while (result == 1) {
			multiplier++;
			result = Dice.roll(1, 10);
		}
		return result * multiplier;
	}

	public static int getBotchResult(int botchDice) {
		int botches = 0;
		for (int i = 0; i < botchDice; i++) {
			if (Dice.roll(1, 10) == 10)
				botches++;
		}
		return botches;
	}
}
