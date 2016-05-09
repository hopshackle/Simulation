package hopshackle.simulation;

import java.util.*;

public class ActionPlan {
	
	protected Agent agent;
	protected PriorityQueue<Action> actionQueue = new PriorityQueue<Action>();
	protected List<Action> executedActions = new ArrayList<Action>();

	public ActionPlan(Agent agent) {
		this.agent = agent;
	}
	
	public boolean requiresDecision() {
		return true;
	}
	public void addAction(Action newAction) {
		if (!agent.isDead() && (agent.getWorld() != null) && newAction != null) {
			List<Action> overriddenActions = new ArrayList<Action>();
			boolean willFitInPlan = true;
			for (Action a : actionQueue) {
				if (overlap(a, newAction)) {
					if ((a.getState() != Action.State.EXECUTING) && a.getValue() >= newAction.getValue()) {
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
					cancelOrReject(a);
					// currently all agents accept/reject when the action is created
					// this will ultimately need to change for asynchronous decisions
					// TODO: 
				}
				actionQueue.add(newAction);
				agent.getWorld().addAction(newAction);
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
	
	public void actionExecuted(Action oldAction) {
		if (oldAction != null) {
			actionQueue.remove(oldAction);
			executedActions.add(oldAction);
		} else Agent.errorLogger.warning("Null action sent");
	}
	public void purgeActions(){
		for (Action a : actionQueue) {
			cancelOrReject(a);
		}
	}
	public Action getNextAction() {
		return actionQueue.peek();
	}
	private void cancelOrReject(Action a) {
		if (a.isOptionalParticipant(agent)) {
			a.reject(agent);
		} else {
			a.cancel();
		}
	}
}

