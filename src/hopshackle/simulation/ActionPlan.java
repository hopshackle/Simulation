package hopshackle.simulation;
import java.util.*;

public class ActionPlan {

	public static long timeUntilAllAvailable(List<? extends Agent> agents) {
		long retValue = 0;
		for (Agent a : agents) {
			retValue = Math.max(retValue, a.actionPlan.timeToEndOfQueue());
		}
		return retValue;
	}

	private static Policy<Action<?>> defaultActionPolicy = new Policy<Action<?>>("action") {
		@Override
		public double getValue(Action<?> action, Agent agent) {
			return 0.0;
		}
	};

	protected Agent agent;
	protected PriorityQueue<Action<?>> actionQueue = new PriorityQueue<Action<?>>();
	protected List<Action<?>> executedActions = new ArrayList<Action<?>>();

	public ActionPlan(Agent agent) {
		this.agent = agent;
	}
	public long timeToNextActionStarts() {
		long currentTime = agent.getWorld().getCurrentTime();
		long timeToGo = Long.MAX_VALUE;
		for (Action<?> a : actionQueue) {
			if (!a.isDeleted()) timeToGo = Math.min(timeToGo, a.getStartTime() - currentTime);
		}
		return timeToGo;	
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void addAction(Action newAction) {
		if (!agent.isDead() && (agent.getWorld() != null) && newAction != null) {
			if (actionQueue.contains(newAction)) return;
			List<Action> overriddenActions = new ArrayList<Action>();
			Policy<Action<?>> actionPolicy = (Policy<Action<?>>) agent.getPolicy("action");
			if (actionPolicy == null) actionPolicy = defaultActionPolicy;
			double newActionValue = actionPolicy.getValue(newAction, agent);
			boolean willFitInPlan = true;
			for (Action a : actionQueue) {
				if (a != newAction && overlap(a, newAction)) {
					if ((a.getState() != Action.State.EXECUTING) && actionPolicy.getValue(a, agent) < newActionValue) {
						overriddenActions.add(a);
					} else {
						//					System.out.println(agent + " rejects " + newAction);
						newAction.reject(agent);
						willFitInPlan = false;
						break;
					}
				}
			}
			if (willFitInPlan) {
				newAction.agree(agent);
				actionQueue.add(newAction);
				for (Action a : overriddenActions) {
					a.reject(agent);
					// TODO: currently all agents accept/reject when the action is created
					// this will ultimately need to change for asynchronous decisions
				}
			}
		}
	}

	private boolean overlap(Action<?> a1, Action<?> a2) {
		// two actions do not overlap iff the one that starts first finishes before the later one starts
		// otherwise there is an overlap
		Action<?> earlier = a1;
		Action<?> later = a2;
		if (a1.getStartTime() > a2.getStartTime()) {
			earlier = a2;
			later = a1;
		}
		return !(earlier.getEndTime() <= later.getStartTime());
	}

	public void actionCompleted(Action<?> oldAction) {
		if (oldAction != null) {
			actionQueue.remove(oldAction);
			if (oldAction.getState() == Action.State.FINISHED) {
				executedActions.add(oldAction);
			}
		} else Agent.errorLogger.warning("Null action sent");
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void purgeActions(boolean overrideExecuting){
		for (Action a : HopshackleUtilities.cloneList(actionQueue)) {
			a.reject(agent, overrideExecuting);
		}
	}
	/*
	 * Only used for testing
	 */
	public Action<?> getNextAction() {
		Action<?> retValue = null;
		do {
			retValue = actionQueue.peek();
			if (retValue != null) {
				if (retValue.getState() == Action.State.CANCELLED || retValue.getState() == Action.State.FINISHED) {
					actionCompleted(retValue);
					retValue = null;
				}
			}
		} while (retValue == null && !actionQueue.isEmpty());
		return retValue;
	}
	public Action<?> getLastAction() {
		if (!executedActions.isEmpty()) 
			return executedActions.get(executedActions.size()-1);
		else
			return null;
	}
	public List<Action<?>> getExecutedActions() {
		return executedActions;
	}
	public int sizeOfQueue() {
		return actionQueue.size();
	}
	public long timeToEndOfQueue() {
		long currentTime = agent.getWorld().getCurrentTime();
		long timeToGo = 0;
		for (Action<?> a : actionQueue) {
			timeToGo = Math.max(timeToGo, a.getEndTime() - currentTime);
		}
		return timeToGo;	
	}

	@Override
	public String toString() {
		StringBuffer retValue = new StringBuffer();
		for (Action<?> a : actionQueue) {
			retValue.append(a.toString() + "\n");
		}
		return retValue.toString();
	}

	public boolean contains(ActionEnum<?> forwardAction) {
		for (Action<?> a : actionQueue) {
			if (a.actionType == forwardAction)
				return true;
		}
		return false;
	}
	public static int getNextAvailableSlot(List<? extends Agent> requiredAgents) {
		int retValue = 0;
		for (Agent a : requiredAgents) {
			int endTime = (int) a.getActionPlan().timeToEndOfQueue();
			if (a.getActionPlan().timeToEndOfQueue() > retValue)
				retValue = endTime;
		}
		return retValue;
	}
}

