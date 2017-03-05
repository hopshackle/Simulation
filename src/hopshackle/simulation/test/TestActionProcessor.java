package hopshackle.simulation.test;

import static org.junit.Assert.assertTrue;
import hopshackle.simulation.*;

import java.util.concurrent.TimeUnit;
public class TestActionProcessor {

	private ActionProcessor ap;
	public World w;

	public TestActionProcessor(World world) {
		w = world;
		ap = new ActionProcessor("test", false);
		w.setActionProcessor(ap);
		ap.start();
		ap.setTestingMode(true);
	}

	public void makeValidateAndRunFirstDecision(Agent testAgent, Class<? extends Action<?>> classType) {
		assertTrue(testAgent.getNextAction() == null);
		testAgent.decide();
		validateAndRunAction(classType);
	}

	public void validateAndRunAction(Class<? extends Action<?>> classType) {
		validateAndRunNextAction(classType);
	}
	
	public Action<?> getNextAction() {
		return ap.getNextUndeletedAction();
	}

	public void clearQueue() {
		Action<?> retAction = null;
		do {
			retAction = ap.getNextUndeletedAction(1, TimeUnit.MILLISECONDS);
	//		System.out.println(retAction);
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
			e.printStackTrace();
		}
	}
	
	public Action<?> processOneStep() {
		Action<?> retValue = null;
		try {
			synchronized (ap) {
				retValue = ap.processNextAction();	// run
				ap.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return retValue;
	}
	
	public void validateAndRunNextAction(Class<? extends Action<?>> classType) {
		try {
			synchronized (ap) {
				Action<?> next = ap.processNextAction();	//start
				if (!classType.isInstance(next)) { 
					System.out.println(next);
				}
				assertTrue(classType.isInstance(next));
				ap.wait();
				next = ap.processNextAction();	// run
				ap.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		ap.stop();
	}
}
