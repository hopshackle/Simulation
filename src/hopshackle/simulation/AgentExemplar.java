package hopshackle.simulation;

import java.util.List;

public class AgentExemplar {


	public static <T extends Agent> T getExemplar(List<T> agentList,
			double sampleSize, ScoringFunction breedingScore) {
		
		double maxScore = Double.NEGATIVE_INFINITY;
		T exemplar = null;
		T testAgent = null;
		if (agentList.size() < 1) return null;
		for (int initialLoop = 0; initialLoop < sampleSize; initialLoop++) {
			testAgent = agentList.get(Dice.roll(1, agentList.size())-1);
			if (breedingScore.getScore(testAgent) > maxScore) {
				exemplar = testAgent;
				maxScore = breedingScore.getScore(testAgent);
			}
		}
		return exemplar;
	}
}
