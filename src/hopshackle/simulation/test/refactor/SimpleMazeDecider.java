package hopshackle.simulation.test.refactor;

import java.util.*;

import hopshackle.simulation.*;

public class SimpleMazeDecider extends BaseStateDecider<TestAgent> {
	
	static List<GeneticVariable<TestAgent>> genVar = new ArrayList<GeneticVariable<TestAgent>>(EnumSet.allOf(TestGenVar.class));
	List<TestActionEnum> lastMoves = new ArrayList<TestActionEnum>();

	public SimpleMazeDecider() {
		super(new LinearStateFactory<TestAgent>(genVar));
	}

	@Override
	public ActionEnum<TestAgent> makeDecision(TestAgent agent, List<ActionEnum<TestAgent>> options) {
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
	public double valueOption(ActionEnum<TestAgent> option,	State<TestAgent> state) {
		return 0;
	}
	@Override
	public void learnFrom(ExperienceRecord<TestAgent> exp, double maxResult) {
	}
}

