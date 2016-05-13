package hopshackle.simulation;

import java.util.*;

public class ActionPlan {
	
	private static Policy<Action> defaultActionPolicy = new Policy<Action>("action") {
		@Override
		public double getValue(Action action, Agent agent) {
			return 0.0;
		}
	};
	
	protected Agent agent;
	protected PriorityQueue<Action> actionQueue = new PriorityQueue<Action>();
	protected List<Action> executedActions = new ArrayList<Action>();

	public ActionPlan(Agent agent) {
		this.agent = agent;
	}
	public long timeToNextActionStarts() {
		long currentTime = agent.getWorld().getCurrentTime();
		long timeToGo = Long.MAX_VALUE;
		for (Action a : actionQueue) {
			timeToGo = Math.min(timeToGo, a.getStartTime() - currentTime);
		}
		return timeToGo;	
	}
	public void addAction(Action newAction) {
		if (!agent.isDead() && (agent.getWorld() != null) && newAction != null) {
			List<Action> overriddenActions = new ArrayList<Action>();
			@SuppressWarnings("unchecked")
			Policy<Action> actionPolicy = (Policy<Action>) agent.getPolicy("action");
			if (actionPolicy == null) actionPolicy = defaultActionPolicy;
			double newActionValue = actionPolicy.getValue(newAction, agent);
			boolean willFitInPlan = true;
			for (Action a : actionQueue) {
				if (overlap(a, newAction)) {
					if ((a.getState() != Action.State.EXECUTING) && actionPolicy.getValue(a, agent) < newActionValue) {
						overriddenActions.add(a);
					} else {
						newAction.reject(agent);
						willFitInPlan = false;
						break;
					}
				}
			}
			if (willFitInPlan) {
				for (Action a : overriddenActions) {
					a.reject(agent);
					// currently all agents accept/reject when the action is created
					// this will ultimately need to change for asynchronous decisions
					// TODO: 
				}
				newAction.agree(agent);
				actionQueue.add(newAction);
				agent.getWorld().addAction(newAction);
				// TODO: really only want to add action to queue once...not once per agent
				// although this should not cause any problems as the later entries will be discarded
			}
		}
	}
	private boolean overlap(Action a1, Action a2) {
		// two actions do not overlap iff the one that starts first finishes before the later one starts
		// otherwise there is an overlap
		Action earlier = a1;
		Action later = a2;
		if (a1.getStartTime() > a2.getStartTime()) {
			earlier = a2;
			later = a1;
		}
		return !(earlier.getEndTime() <= later.getStartTime());
	}
	
	public void actionCompleted(Action oldAction) {
		if (oldAction != null) {
			actionQueue.remove(oldAction);
			if (oldAction.getState() == Action.State.FINISHED) {
				executedActions.add(oldAction);
			}
		} else Agent.errorLogger.warning("Null action sent");
	}
	public void purgeActions(){
		for (Action a : HopshackleUtilities.cloneList(actionQueue)) {
			a.reject(agent);
		}
	}
	public Action getNextAction() {
		return actionQueue.peek();
	}

}

