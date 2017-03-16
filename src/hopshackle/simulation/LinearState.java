package hopshackle.simulation;

import java.util.List;

public class LinearState<A extends Agent> implements State<A> {

	private List<GeneticVariable<A>> variables;
	private double[] values;
	private double[] score;
	private int actingAgentNumber;

	public LinearState(A agent, List<GeneticVariable<A>> var) {
		if (agent.getGame() != null) {
			actingAgentNumber = agent.getGame().getPlayerNumber(agent)-1;
			List<Agent> players = agent.getGame().getAllPlayers();
			score = new double[players.size()];
			for (int i = 0; i < players.size(); i++){
				score[i] = agent.getScore();
			}
		} else {
			actingAgentNumber = 0;
			score = new double[1];
			score[0] = agent.getScore();
		}
		variables = var;
		values = new double[variables.size()];
		for (int i = 0; i < variables.size(); i ++) {
			GeneticVariable<A> gv = variables.get(i);
			values[i] = gv.getValue(agent);
		}
	}

	@Override
	public double[] getAsArray() {
		return values;
	}

	@Override
	public String getAsString() {
		double[] values = getAsArray();
		StringBuffer retValue = new StringBuffer();
		for (double d : values) {
			boolean negative = false;
			if (d < 0.00) {
				negative = true;
				d = -d;
			}
			int asInt = (int) ((d + 0.005) * 100.0);
			String padded = String.valueOf(asInt);
			while(padded.length() < 3) padded = "0" + padded;
			if (negative) padded = "-" + padded;
			retValue.append(padded + "|");
		}
		retValue.deleteCharAt(retValue.length()-1);
		String asString = retValue.toString();
		return asString;
	}

	@Override
	public LinearState<A> clone() {
		return this;
	}

	@Override
	public LinearState<A> apply(ActionEnum<A> action) {
		return this;
	}

	@Override
	public int getActorRef() {
		return actingAgentNumber;
	}

	@Override
	public double[] getScore() {
		return score;
	}
	
	@Override
	public String toString() {
		return getAsString();
	}

}
