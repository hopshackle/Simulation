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
		StringBuffer retValue = new StringBuffer();
		for (double d : values) {
			retValue.append(String.format("%.2f|", d));
		}
		return retValue.toString();
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

}
