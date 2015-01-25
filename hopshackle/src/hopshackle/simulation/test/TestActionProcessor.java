package hopshackle.simulation.test;

import static org.junit.Assert.assertTrue;
import hopshackle.simulation.*;

import java.util.concurrent.TimeUnit;
public class TestActionProcessor {

	private ActionProcessor actionProcessor;

	public TestActionProcessor(World w) {
		w.setCalendar(new FastCalendar(10));
		actionProcessor = new ActionProcessor("test", false);
		w.setActionProcessor(actionProcessor);
	}

	public void getValidateAndRunNextAction(Class<? extends Action> classType) {
		Action nextAction = getNextAction();
		validateAndRunAction(nextAction, classType);
	}

	public void makeValidateAndRunFirstDecision(Agent testAgent, Class<? extends Action> classType) {
		assertTrue(testAgent.getNextAction() == null);
		Action nextAction = testAgent.decide();
		validateAndRunAction(nextAction, classType);
	}

	public void validateAndRunAction(Action nextAction, Class<? extends Action> classType) {
		assertTrue(classType.isInstance(nextAction));
		nextAction.run();
	}
	
	public Action getNextAction() {
		return actionProcessor.getNextUndeletedAction();
	}

	public void clearQueue() {
		Action retAction = null;
		do {
			retAction = actionProcessor.getNextUndeletedAction(1, TimeUnit.MILLISECONDS);
			System.out.println(retAction);
		} while (retAction != null);
	}
}
