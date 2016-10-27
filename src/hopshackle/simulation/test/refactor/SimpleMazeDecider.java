package hopshackle.simulation.test.refactor;

import java.util.*;

import hopshackle.simulation.*;

public class SimpleMazeDecider extends BaseDecider<TestAgent> {
	
	static List<TestActionEnum> actionList = new ArrayList<TestActionEnum>(EnumSet.allOf(TestActionEnum.class));
	static List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	List<TestActionEnum> lastMoves = new ArrayList<TestActionEnum>();

	public SimpleMazeDecider() {
		super(new LinearStateFactory<TestAgent>(genVar), actionList);
	}

	@Override
	public ActionEnum<TestAgent> makeDecision(TestAgent agent) {
		TestActionEnum retValue = TestActionEnum.RIGHT;
		if (lastMoves.size() < 2) {
			retValue = TestActionEnum.LEFT;
			lastMoves.add(retValue);
		} else {
			if (lastMoves.contains(TestActionEnum.RIGHT))
				retValue = TestActionEnum.LEFT;
			lastMoves.remove(0);
			lastMoves.add(retValue);
		} 
		return retValue;
	}
	
	@Override
	public double valueOption(ActionEnum<TestAgent> option,	TestAgent decidingAgent) {
		return 0;
	}

	@Override
	public void learnFrom(ExperienceRecord<TestAgent> exp, double maxResult) {
	}
}

