package hopshackle.simulation;

import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.*;

/**
 * 
 * Re-write May 2016 The new arrangement is that an Action has an internal
 * StateMachine that moves it between: PROPOSED PLANNED EXECUTING FINISHED
 * CANCELLED
 * 
 * On instantiation we enter the PROPOSED state. An Action has lists of
 * mandatory and optional Agents. We add the PROPOSED action to the ActionPlan
 * of all Agents ans wait for their responses. Once all mandatory Agents have
 * responded, we either enter PLANNED (if they said yes), or CANCELLED
 * (otherwise). [Note that previously Agent maintenance occurred in decide(),
 * *before* a decision was made. We now move this to after.] Once the Action
 * starts, we enter EXECUTING, which lasts until we complete the Action (on the
 * basis that Actions may take time, especially in a real-time simulation). At
 * this point we enter FINISHED. We can move to CANCELLED from any state except
 * FINISHED, which means the main life-cycle was interrupted, and the Action has
 * not been fully executed.
 * 
 * agree(Agent) is invoked to indicate the Agent accept the plan reject(Agent)
 * is invoked to indicate the Agent rejects the plan We move to PLANNED once all
 * mandatory agents have agreed.
 *
 * start() Moves to EXECUTING, and is only valid if current state is PLANNED
 * run() Moves to FINISHED, and is only valid if the current state is EXECUTING
 * This is unchanged - and has the same four steps as previously, namely:
 * doAdmin() doStuff() doCleanup() doNextDecision()
 * 
 * cancel() Moves from any state to CANCELLED (not valid if we are in a FINISHED
 * state)
 * 
 * 	The run() method executes four methods in turn:
 * 		doAdmin(); doStuff(); doCleanUp(); doNextDecision();
 *  The start() method executes initialisation()
 *  The cancel() method executes delete()	
 * 
 * The intention being that these are overridden in children classes to
 * implement behaviour the only ones that do anything in this base
 * implementation are:
 * 
 * doAdmin - records this action against the actor's log doNextDecision - the
 * agent decides on next decision, and implements this.
 * 
 * the most commonly overridden method should be doStuff(), and possibly
 * doNextDecision() if the default decider is not to be called
 * 
 * The contract states that no logic will ever be put into delete(), initialisation(), doStuff() or
 * doCleanUp() in this Action superclass
 * 
 * @author James
 *
 */

public abstract class Action implements Delayed {

	public enum State {
		PROPOSED, PLANNED, EXECUTING, FINISHED, CANCELLED;
	}
	public class InvalidStateTransition extends RuntimeException {
		private static final long serialVersionUID = -8459076735758399835L;
		public InvalidStateTransition(String message) {
			super(message);
		}
	}

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	
	protected List<Agent> mandatoryActors;
	protected List<Agent> optionalActors;
	protected Map<Long, Boolean> agentAgreement = new HashMap<Long, Boolean>();
	protected Agent actor; // currently left in for backwards compatibility
	protected World world;
	protected long startTime = -1;
	protected long endTime = -1;
	protected long plannedEndTime;
	protected long plannedStartTime;
	protected State currentState = State.PROPOSED;
	protected int value = 0;

	public Action(Agent a, boolean recordAction) {
		this(a, 1000, recordAction);
	}

	public Action(Agent a, long duration, boolean recordAction) {
		this(HopshackleUtilities.listFromInstance(a), new ArrayList<Agent>(), 0l, duration, recordAction);
	}

	public Action(List<Agent> mandatory, List<Agent> optional, long startOffset, long duration, boolean recordAction) {
		mandatoryActors = HopshackleUtilities.cloneList(mandatory);
		optionalActors = HopshackleUtilities.cloneList(optional);
		if (!mandatory.isEmpty()) {
			actor = mandatory.get(0);
			world = actor.getWorld();
		} else {
			world = optional.get(0).getWorld();
		}
		// startTime used to be endTime??
		// startTime = (long)((duration*throttle))+world.getCurrentTime() + startOffset;
		plannedStartTime = world.getCurrentTime() + startOffset;
		plannedEndTime = plannedStartTime + duration ;
		if (recordAction) world.recordAction(this);
	}

	public void agree(Agent a) {
		switch (currentState) {
		case PROPOSED:
		case PLANNED:
			updateAgreement(a, true);
			break;
		default:
			a.log("Attempts to agree to Action: " + a + " irrelevant from " + currentState);
			logger.warning("Attempts to agree to Action: " + a + " irrelevant from " + currentState);
		}
	}

	public void reject(Agent a) {
		switch (currentState) {
		case PROPOSED:
		case PLANNED:
			updateAgreement(a, false);
			a.actionPlan.actionQueue.remove(this);
			break;
		default:
			a.log("Attempts to reject Action: " + a + " irrelevant from " + currentState);
			logger.warning("Attempts to reject Action: " + a + " irrelevant from " + currentState);
		}
	}

	private void updateAgreement(Agent a, boolean choice) {
		agentAgreement.put(a.getUniqueID(), choice);
		// Now check for change of state
		boolean changeState = (currentState == State.PROPOSED);
		for (Agent m : mandatoryActors) {
			if (agentAgreement.containsKey(m.getUniqueID())) {
				if (agentAgreement.get(m.getUniqueID()) == false) {
					this.cancel();
					changeState = false;
					break;
				}
			} else {
				changeState = false;
			}
		}
		if (changeState) changeState(State.PLANNED);
	}

	public State getState() {
		return currentState;
	}

	private void changeState(State newState) {
		currentState = newState;
	}

	public final void start() {
		switch (currentState) {
		case PROPOSED:
		case FINISHED:
		case EXECUTING:
		case CANCELLED:
			throw new Action.InvalidStateTransition("Cannot start() from " + currentState);
		case PLANNED:
			startTime = world.getCurrentTime();
			changeState(State.EXECUTING);
			initialisation();
		}
	}
	
	public final void run() {
		switch (currentState) {
		case PROPOSED:
		case FINISHED:
		case PLANNED:
		case CANCELLED:
			throw new Action.InvalidStateTransition("Cannot run() from " + currentState);
		case EXECUTING:
			endTime = world.getCurrentTime();
			doAdmin();
			doStuff();
			changeState(State.FINISHED);
			doCleanUp();
			doNextDecision();
		}
	}

	protected void initialisation() {
		
	}
	
	protected void doAdmin() {

	}

	protected void doStuff() {

	}

	protected void doCleanUp() {
		for (Agent a : mandatoryActors) {
			a.actionPlan.actionCompleted(this);
		}
		for (Agent a : optionalActors) {
			if (agentAgreement.getOrDefault(a.getUniqueID(), false)) {
				a.actionPlan.actionCompleted(this);
			}
		}
	}

	protected void doNextDecision() {
		List<Agent> allActors = HopshackleUtilities.cloneList(mandatoryActors);
		allActors.addAll(optionalActors);
		for (Agent actor : allActors) {
			if (!actor.isDead()) {
				Action newAction = actor.decide();
				actor.addAction(newAction);
			}
		}
	}

	public long getDelay(TimeUnit tu) {
		return tu.convert(startTime - world.getCurrentTime(), TimeUnit.MILLISECONDS);
	}

	public int compareTo(Delayed d) {
		return (int) (getDelay(TimeUnit.MILLISECONDS) - d.getDelay(TimeUnit.MILLISECONDS));
	}

	public Agent getActor() {
		return actor;
	}

	public long getEndTime() {
		switch (currentState) {
		case FINISHED:
		case CANCELLED:
			return endTime;
		default:
			return plannedEndTime;
		}
	}

	public long getStartTime() {
		switch (currentState) {
		case EXECUTING:
		case FINISHED:
		case CANCELLED:
			return startTime;
		default:
			return plannedStartTime;
		}
	}
	
	public boolean isDeleted() {
		return (currentState == State.CANCELLED);
	}

	public void cancel() {
		if (currentState != State.FINISHED) {
			changeState(State.CANCELLED);
		}
		delete();
		doCleanUp();
		doNextDecision();
	}

	protected void delete() {

	}

	public boolean isOptionalParticipant(Agent p) {
		return optionalActors.contains(p);
	}
	public boolean isMandatoryParticipant(Agent p) {
		return mandatoryActors.contains(p);
	}
	public List<Agent> getAllConfirmedParticipants() {
		List<Agent> retValue = new ArrayList<Agent>();
		for (long id : agentAgreement.keySet()) {
			if (agentAgreement.get(id)) {
				retValue.add(Agent.getAgent(id));
			}
		}
		return retValue;
	}
}
