package hopshackle.simulation.test;

import static org.junit.Assert.assertTrue;
import hopshackle.simulation.*;

import java.util.concurrent.TimeUnit;
public class TestActionProcessor {

	private ActionProcessor ap;
	World w;

	public TestActionProcessor() {
		ap = new ActionProcessor("test", false);
		w = new World(ap, "test");
		w.setCalendar(new FastCalendar(0l));
		ap.start();
		ap.setTestingMode(true);
	}

	public void makeValidateAndRunFirstDecision(Agent testAgent, Class<? extends Action> classType) {
		assertTrue(testAgent.getNextAction() == null);
		Action nextAction = testAgent.decide();
		validateAndRunAction(nextAction, classType);
	}

	public void validateAndRunAction(Action nextAction, Class<? extends Action> classType) {
		nextAction.addToAllPlans();
		validateAndRunNextAction(classType);
	}
	
	public Action getNextAction() {
		return ap.getNextUndeletedAction();
	}

	public void clearQueue() {
		Action retAction = null;
		do {
			retAction = ap.getNextUndeletedAction(1, TimeUnit.MILLISECONDS);
			System.out.println(retAction);
		} while (retAction != null);
	}
	
	public void processActionsInQueue(int number) {
		try {
			synchronized (ap) {
				for (int i = 0; i < number; i++) {
					ap.processNextAction();
					ap.wait();
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void validateAndRunNextAction(Class<? extends Action> classType) {
		try {
			synchronized (ap) {
				Action next = ap.processNextAction();	//start
				assertTrue(classType.isInstance(next));
				ap.wait();
				next = ap.processNextAction();	// run
				ap.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop() {
		ap.stop();
	}
}
