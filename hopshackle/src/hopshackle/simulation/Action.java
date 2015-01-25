package hopshackle.simulation;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * The run() method executes four methods in turn:
 * 
 * doAdmin();
 * doStuff();
 * doCleanUp();
 * doNextDecision();
 * 
 * the intention being that these are overridden in children classes to implement behaviour
 * the only ones that do anything in this base implementation are:
 * 
 *  doAdmin - records this action against the actor's log
 *  doNextDecision - the agent decides on next decision, and implements this.
 * 
 *  the most commonly overridden method should be doStuff(), and possibly doNextDecision() 
 *  if the default decider is not to be called
 *  
 *  The contract states that no logic will ever be put into doStuff() or doCleanUp() at the Action level
 *  
 * @author James
 *
 */
public abstract class Action implements Delayed{

	protected Agent actor;
	protected World world;
	protected long startTime;
	protected long endTime = -1;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private static double throttle;

	static {
		reset();
	}

	public static void reset() {
		throttle = SimProperties.getPropertyAsDouble("ActionThrottle", "1.0");
	}

	public Action(Agent a, boolean recordAction) {
		this(a, 1000, recordAction);
	}
	public Action (Agent a, long duration, boolean recordAction) {
		actor = a;
		world = a.getWorld();
		setDuration(duration);
		if (recordAction) world.recordAction(this);
	}
	
	public void setDuration(long duration) {
		startTime = (long)((duration*throttle))+world.getCurrentTime();
	}
	
	public final void run() {
		doAdmin();
		doStuff();
		doCleanUp();
		doNextDecision();
	}

	protected void doAdmin() {
		actor.actionExecuted(this);
	}
	protected void doStuff() {

	}
	protected void doCleanUp() {
	}
	
	/**
	 * If actor is dead, then does nothing. 
	 * Otherwise calls the decide method, and queues the resultant action up
	 */
	protected void doNextDecision() {
		if (!actor.isDead()) {
			Action newAction = actor.decide();
			actor.addAction(newAction);
		}
	}

	public long getDelay(TimeUnit tu) {
		return tu.convert(startTime - world.getCurrentTime(), TimeUnit.MILLISECONDS);
	}

	public int compareTo(Delayed d) {
		return (int) (getDelay(TimeUnit.MILLISECONDS)-d.getDelay(TimeUnit.MILLISECONDS));
	}

	public Agent getActor() {
		return actor;
	}
	public void setActor(Agent actor) {
		this.actor = actor;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public boolean isDeleted() {
		if (endTime > -1) {
			return true;
		}
		return false;
	}

	protected void delete() {
		// For use when action is purged before execution
	}

}
