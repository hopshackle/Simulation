package hopshackle.simulation;

import java.util.*;

public class MCStatistics<P extends Agent> {
	
	private List<ActionEnum<P>> allActions;
	private Map<String, MCData> map = new HashMap<String, MCData>();
	private int totalVisits = 0;
	private static double C = SimProperties.getPropertyAsDouble("MonteCarloUCTC", "1.0");

	public MCStatistics(List<ActionEnum<P>> possibleActions) {
		allActions = HopshackleUtilities.cloneList(possibleActions);
	}
	
	public void update(ActionEnum<P> action, double reward) {
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
	private void addAction(ActionEnum<P> newAction) {
		if (!allActions.contains(newAction))
			allActions.add(newAction);
	}
	public List<ActionEnum<P>> getPossibleActions() {
		return allActions;
	}

	public int getVisits() {
		return totalVisits;
	}
	public int getVisits(ActionEnum<P> action) {
		String key = action.toString();
		if (map.containsKey(key)) {
			return map.get(key).visits;
		} else {
			return 0;
		}
	}
	public double getMean(ActionEnum<P> action) {
		String key = action.toString();
		if (map.containsKey(key)) {
			return map.get(key).mean;
		} else {
			return 0.0;
		}
	}

	public boolean hasUntriedAction(List<ActionEnum<P>> availableActions) {
		for (ActionEnum<P> action : availableActions) {
			if (!map.containsKey(action.toString())) {
				addAction(action);
				return true;
			}
		}
		return false;
	}
	
	public ActionEnum<P> getRandomUntriedAction(List<ActionEnum<P>> availableActions) {
		List<ActionEnum<P>> untried = new ArrayList<ActionEnum<P>>();
		for (ActionEnum<P> action : availableActions) {
			if (!map.containsKey(action.toString())) {
				untried.add(action);
				addAction(action);
			}
		}
		if (untried.isEmpty())
			throw new AssertionError("Cannot call getRandomUntriedAction is there aren't any");
		int diceRoll = Dice.roll(1, untried.size());
		return untried.get(diceRoll-1);
	}
	
	public ActionEnum<P> getUCTAction(List<ActionEnum<P>> availableActions) {
		if (hasUntriedAction(availableActions)) 
			throw new AssertionError("Should not be looking for UCT action while there are still untried actions");
		double best = Double.NEGATIVE_INFINITY;
		ActionEnum<P> retValue = null;
		for (ActionEnum<P> action : availableActions) {
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
		StringBuffer retValue = new StringBuffer("MC Statistics\tTotal Visits\t" + totalVisits + "\n");
		for (ActionEnum<P> action : allActions) {
			String key = action.toString();
			retValue.append("\t"+key);
			if (map.containsKey(key)) {
				retValue.append("\t" + map.get(key).toString());
			} else {
				retValue.append("\t No Data\n");
			}
		}
		return retValue.toString();
	}
	/*
	private List<String> keysInVisitOrder() {
		List<String> retValue = new ArrayList<String>();
		List<MCData> sortedByVisit = new ArrayList<MCData>();
		sortedByVisit.addAll(map.values());
		Collections.sort(sortedByVisit);
		List<String> allKeys = new ArrayList<String>();
		allKeys.addAll(map.keySet());
		for (MCData mcd : sortedByVisit) {
			
		}
	}
*/
	public ActionEnum<P> getBestAction(List<ActionEnum<P>> availableActions) {
		ActionEnum<P> retValue = null;
		double score = Double.NEGATIVE_INFINITY;
		for (ActionEnum<P> action : availableActions) {
			String key = action.toString();
			if (map.containsKey(key)) {
				if (map.get(key).mean > score) {
					score = map.get(key).mean;
					retValue = action;
				}
			}
		}
		return retValue;
	}
}

class MCData implements Comparable<MCData> {
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
		return String.format("Visits:%d \tScore:%.2f\n", visits, mean);
	}

	@Override
	public int compareTo(MCData other) {
		return (this.visits - other.visits);
	}
}
