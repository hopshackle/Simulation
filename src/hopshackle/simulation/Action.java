package hopshackle.simulation;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.*;

/**
 * Re-write May 2016 The new arrangement is that an Action has an internal
 * StateMachine that moves it between: PROPOSED PLANNED EXECUTING FINISHED
 * CANCELLED
 * <p>
 * On instantiation we enter the PROPOSED state. An Action has lists of
 * mandatory and optional Agents. We add the PROPOSED action to the ActionPlan
 * of all Agents and wait for their responses. Once all mandatory Agents have
 * responded, we either enter PLANNED (if they said yes), or CANCELLED
 * (otherwise). [Note that previously Agent maintenance occurred in decide(),
 * *before* a decision was made. We now move this to after.] Once the Action
 * starts, we enter EXECUTING, which lasts until we complete the Action (on the
 * basis that Actions may take time, especially in a real-time simulation). At
 * this point we enter FINISHED. We can move to CANCELLED from any state except
 * FINISHED, which means the main life-cycle was interrupted, and the Action has
 * not been fully executed.
 * <p>
 * agree(Agent) is invoked to indicate the Agent accept the plan reject(Agent)
 * is invoked to indicate the Agent rejects the plan We move to PLANNED once all
 * mandatory agents have agreed.
 * <p>
 * start() Moves to EXECUTING, and is only valid if current state is PLANNED
 * run() Moves to FINISHED, and is only valid if the current state is EXECUTING
 * This is unchanged - and has the same four steps as previously, namely:
 * doAdmin() doStuff() doCleanup() doNextDecision()
 * <p>
 * cancel() Moves from any state to CANCELLED (not valid if we are in a FINISHED
 * state)
 * <p>
 * The run() method executes four methods in turn:
 * doAdmin(); doStuff(); doCleanUp(); doNextDecision();
 * The start() method executes initialisation()
 * The cancel() method executes delete()
 * <p>
 * The intention being that these are overridden in children classes to
 * implement behaviour the only ones that do anything in this base
 * implementation are:
 * <p>
 * doAdmin - records this action against the actor's log doNextDecision - the
 * agent decides on next decision, and implements this.
 * <p>
 * the most commonly overridden method should be doStuff(), and possibly
 * doNextDecision() if the default decider is not to be called
 * <p>
 * The contract states that no logic will ever be put into delete(), initialisation(), doStuff() or
 * doCleanUp() in this Action superclass
 *
 * @author James
 */

public abstract class Action<A extends Agent> implements Delayed {

    public enum State {
        PROPOSED, PLANNED, EXECUTING, FINISHED, CANCELLED;
    }

    private static AtomicLong idFountain = new AtomicLong(1);
    protected static Logger logger = Logger.getLogger("hopshackle.simulation");

    protected List<A> mandatoryActors;
    protected List<A> optionalActors;
    protected Map<A, Boolean> agentAgreement = new HashMap<A, Boolean>();
    protected A actor, nextActor; // currently left in for backwards compatibility
    protected World world;
    protected long startTime = -1;
    protected long endTime = -1;
    protected long plannedEndTime;
    protected long plannedStartTime;
    protected State currentState = State.PROPOSED;
    private ActionEnum<A> actionType;
    private long uniqueId = idFountain.getAndIncrement();

    public Action(ActionEnum<A> type, A a, boolean recordAction) {
        this(type, a, 1000, recordAction);
    }

    public Action(ActionEnum<A> type, A a, long duration, boolean recordAction) {
        this(type, a, 0l, duration, recordAction);
    }

    public Action(ActionEnum<A> type, A a, long startOffset, long duration, boolean recordAction) {
        this(type, HopshackleUtilities.listFromInstance(a), new ArrayList<A>(), startOffset, duration, recordAction);
    }

    public Action(ActionEnum<A> type, List<A> mandatory, List<A> optional, long startOffset, long duration, boolean recordAction) {
        this.actionType = type;
        mandatoryActors = HopshackleUtilities.cloneList(mandatory);
        optionalActors = HopshackleUtilities.cloneList(optional);
        optionalActors.removeAll(mandatoryActors);
        if (!mandatory.isEmpty()) {
            actor = mandatory.get(0);
            world = actor.getWorld();
            nextActor = actor;
        } else {
            world = optional.get(0).getWorld();
        }
        plannedStartTime = world.getCurrentTime() + startOffset;
        plannedEndTime = plannedStartTime + duration;

        if (recordAction) world.recordAction(this);
    }

    public void agree(A a) {
        switch (currentState) {
            case PROPOSED:
            case PLANNED:
                updateAgreement(a, true);
                AgentEvent learningEvent = new AgentEvent(a, AgentEvent.Type.ACTION_AGREED, this);
                eventDispatch(learningEvent);
                break;
            default:
                a.log("Attempts to agree to Action: " + a + " irrelevant from " + currentState);
                logger.warning("Attempts to agree to Action: " + a + " irrelevant from " + currentState);
        }
    }

    protected void eventDispatchToAllAgents(AgentEvent.Type type, List<A> recipients) {
        for (A a : recipients) {
            AgentEvent learningEvent = new AgentEvent(a, type, this);
            eventDispatch(learningEvent);
        }
    }

    protected void eventDispatch(AgentEvent learningEvent) {
        Agent a = learningEvent.getAgent();
        a.eventDispatch(learningEvent);
    }

    public void reject(A a) {
        reject(a, false);
    }

    public void reject(A a, boolean overrideExecuting) {

        switch (currentState) {
            case EXECUTING:
                if (overrideExecuting) {
                    a.log("Withdraws from " + this + " early.");
                } else {
                    a.log("Attempts to reject Action: " + this + " irrelevant from " + currentState + " for " + a);
                    break;
                }
            case PROPOSED:
            case PLANNED:
                //          a.log("Declines to participate in " + this.toString());
                updateAgreement(a, false);
                AgentEvent learningEvent = new AgentEvent(a, AgentEvent.Type.ACTION_REJECTED, this);
                eventDispatch(learningEvent);
                break;
            default:
                a.log("Attempts to reject Action: " + this + " irrelevant from " + currentState + " for " + a);
        }
    }

    private void updateAgreement(A a, boolean confirmed) {
        agentAgreement.put(a, confirmed);
        // Now check for change of state
        boolean changeState = (currentState == State.PROPOSED);
        for (A m : mandatoryActors) {
            if (agentAgreement.containsKey(m)) {
                if (!agentAgreement.get(m)) {
                    this.cancel();
                    changeState = false;
                    break;
                }
            } else {
                changeState = false;
            }
        }
        if (optionalActors.contains(a) && !confirmed) {
            optionalActors.remove(a);
            a.actionPlan.actionCompleted(this);
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
            case EXECUTING:
                throw new InvalidStateTransition("Cannot start() from " + currentState);
            case PLANNED:
                startTime = world.getCurrentTime();
                long duration = plannedEndTime - plannedStartTime;
                plannedEndTime = startTime + duration;
                changeState(State.EXECUTING);
                initialisation();
                if (!isDeleted()) break;
            case FINISHED:
            case CANCELLED:
                doCleanUp();
                doNextDecision();
        }
    }

    public final void run() {
        switch (currentState) {
            case PROPOSED:
            case PLANNED:
                throw new InvalidStateTransition("Cannot run() from " + currentState);
            case EXECUTING:
                endTime = world.getCurrentTime();
                doAdmin();
                doStuff();
                changeState(State.FINISHED);
                eventDispatchToAllAgents(AgentEvent.Type.ACTION_EXECUTED, getAllConfirmedParticipants());
            case CANCELLED:
            case FINISHED:
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
        if (getState() == State.CANCELLED)
            eventDispatchToAllAgents(AgentEvent.Type.ACTION_CANCELLED, getAllConfirmedParticipants());
        for (A a : mandatoryActors) {
            a.actionPlan.actionCompleted(this);
            a.maintenance();
        }
        for (A a : optionalActors) {
            a.actionPlan.actionCompleted(this);
            a.maintenance();
        }
    }

    public void doNextDecision() {
        eventDispatchToAllAgents(AgentEvent.Type.DECISION_STEP_COMPLETE, getAllInvitedParticipants());
        for (A actor : getAllInvitedParticipants()) {
            doNextDecision(actor);
        }
    }

    protected void doNextDecision(A actor) {
        if (!actor.isDead()) {
            actor.decide();
        }
    }

    @Override
    public long getDelay(TimeUnit tu) {
        switch (getState()) {
            case EXECUTING:
                return tu.convert(getEndTime() - world.getCurrentTime() - 1, TimeUnit.MILLISECONDS);
            default:
                return tu.convert(getStartTime() - world.getCurrentTime(), TimeUnit.MILLISECONDS);
        }

    }

    @Override
    public int compareTo(Delayed d) {
        return (int) (getDelay(TimeUnit.MILLISECONDS) - d.getDelay(TimeUnit.MILLISECONDS));
    }

    public A getActor() {
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
                return startTime;
            case CANCELLED:
                if (startTime > -1) return startTime;
            default:
                return plannedStartTime;
        }
    }

    public boolean isCancelled() {
        return currentState == State.CANCELLED;
    }

    public boolean isDeleted() {
        return (currentState == State.CANCELLED || currentState == State.FINISHED);
    }

    public void cancel() {
        if (!isDeleted()) {
            changeState(State.CANCELLED);
            endTime = world.getCurrentTime();
        }
        eventDispatchToAllAgents(AgentEvent.Type.ACTION_REJECTED, getAllConfirmedParticipants());

        //	System.out.println( actor + " cancelling " + this);
        delete();
        //		doCleanUp();
        //		doNextDecision();
    }

    protected void delete() {
    }

    public boolean isOptionalParticipant(Agent p) {
        return optionalActors.contains(p);
    }

    public boolean isMandatoryParticipant(Agent p) {
        return mandatoryActors.contains(p);
    }

    @SuppressWarnings("unchecked")
    public List<A> getAllConfirmedParticipants() {
        List<A> retValue = new ArrayList<A>();
        for (Agent a : agentAgreement.keySet()) {
            if (agentAgreement.get(a)) {
                retValue.add((A) a);
            }
        }
        return retValue;
    }

    public List<A> getAllInvitedParticipants() {
        List<A> allAgents = new ArrayList<A>();
        allAgents.addAll(mandatoryActors);
        allAgents.addAll(optionalActors);
        return allAgents;
    }

    public void addToAllPlans() {
        if (actor.getWorld() != null) {
            for (Agent participant : getAllInvitedParticipants()) {
                participant.actionPlan.addAction(this);
                //			System.out.println(participant + " adds " + this + " to plan");
            }
            actor.getWorld().addAction(this);
        }
    }

    public ActionEnum<A> getType() {
        return actionType;
    }

    public long getId() {
        return uniqueId;
    }

    @Override
    public int hashCode() {
        return (int) uniqueId;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Action) {
            return ((Action<?>) o).getId() == getId();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return actionType.toString();
    }

}
