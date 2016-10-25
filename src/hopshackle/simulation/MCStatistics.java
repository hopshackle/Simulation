package hopshackle.simulation;

import java.util.*;

public class MCStatistics<P extends Agent, A extends ActionEnum<P>> {
	
	private List<A> allActions;
	private int totalVisits = 0;
	private static double C = SimProperties.getPropertyAsDouble("MonteCarloUCTC", "0.1");

	public MCStatistics(List<A> possibleActions) {
		allActions = HopshackleUtilities.cloneList(possibleActions);
	}

	Map<String, MCData> map = new HashMap<String, MCData>();
	
	public void update(A action, double reward) {
		if (!allActions.contains(action))
			throw new AssertionError("Unexpected Action " + action);
		String key = action.toString();
		if (map.containsKey(key)) {
			MCData old = map.get(key);
			map.put(key, new MCData(old, reward));
		} else {
			map.put(key, new MCData(1, reward));
		}
		totalVisits++;
	}

	public int getVisits() {
		return totalVisits;
	}
	public int getVisits(A action) {
		String key = action.toString();
		if (map.containsKey(key)) {
			return map.get(key).visits;
		} else {
			return 0;
		}
	}
	public double getMean(A action) {
		String key = action.toString();
		if (map.containsKey(key)) {
			return map.get(key).mean;
		} else {
			return 0.0;
		}
	}

	public boolean hasUntriedAction() {
		for (A action : allActions) {
			if (!map.containsKey(action.toString()))
				return true;
		}
		return false;
	}
	
	public A getRandomUntriedAction() {
		List<A> untried = new ArrayList<A>();
		for (A action : allActions) {
			if (!map.containsKey(action.toString()))
				untried.add(action);
		}
		if (untried.isEmpty())
			throw new AssertionError("Cannot call getRandomUntriedAction is there aren't any");
		int diceRoll = Dice.roll(1, untried.size());
		return untried.get(diceRoll-1);
	}
	
	public A getUCTAction() {
		if (hasUntriedAction()) 
			throw new AssertionError("Should not be looking for UCT action while there are still untried actions");
		double best = Double.MIN_VALUE;
		A retValue = null;
		for (A action : allActions) {
			String key = action.toString();
			MCData data = map.get(key);
			double score = data.mean + C * Math.sqrt(Math.log(totalVisits) / (double)data.visits);
			if (score > best) {
				best = score;
				retValue = action;
			}
		}
		return retValue;
	}
	
	@Override
	public String toString() {
		StringBuffer retValue = new StringBuffer("MC Statistics: \n\tTotal Visits\t" + totalVisits + "\n");
		for (A action : allActions) {
			String key = action.toString();
			retValue.append(key);
			if (map.containsKey(key)) {
				retValue.append("/t" + map.get(key).toString());
			} else {
				retValue.append("\t No Data");
			}
		}
		return retValue.toString();
	}
}

class MCData {
	double mean;
	int visits;
	
	public MCData(int n, double r) {
		visits = n;
		mean = r;
	}
	
	public MCData(MCData old, double r) {
		visits = old.visits + 1;
		mean = old.mean + (r - old.mean) / (double) visits;
	}
	
	@Override
	public String toString() {
		return String.format("Visits:%d \tScore:%.2f", visits, mean);
	}
}
