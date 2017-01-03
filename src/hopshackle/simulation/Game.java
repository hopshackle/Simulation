package hopshackle.simulation;

import java.util.*;

public abstract class Game<P extends Agent, A extends ActionEnum<P>> {

	protected Stack<Action<P>> actionStack = new Stack<Action<P>>(); 

	public abstract Game<P, A> clone(P perspectivePlayer);

	public abstract P getCurrentPlayer();

	public abstract List<P> getAllPlayers();

	public abstract int getPlayerNumber(P player);

	public abstract P getPlayer(int n);

	public abstract int getCurrentPlayerNumber();

	public abstract List<ActionEnum<P>> getPossibleCurrentActions();

	public abstract boolean gameOver();
	
	public abstract void updateGameStatus();

	public final double[] playGame() {
		
		P currentPlayer = null;
		List<ActionEnum<P>> options = null;
		Decider<P> decider = null;
		Action<P> action = null;

		while (!gameOver()) {
			if (action != null) {
				// this occurs if we popped an action off the stack on the last iteration
				// so we already have the action we wish to execute
			} else {
				// we have completed the last one, so pick a new action
				if (!actionStack.isEmpty()) { // actionStack first, to complete interrupts and consequences
					options = actionStack.peek().getNextOptions();
					currentPlayer = actionStack.peek().getActor();
					decider = currentPlayer.getDecider();
					action = decider.decide(currentPlayer, options);
				} else {	// otherwise we get a new action
					updateGameStatus();
					currentPlayer = getCurrentPlayer();
					options = getPossibleCurrentActions();
					decider = currentPlayer.getDecider();
					action = decider.decide(currentPlayer, options);
				}
			}
			// this is for compatibility with Action statuses in a real-time simulation
			List<P> participants = action.getAllInvitedParticipants();
			for (P player : participants) {
				player.actionPlan.addAction(action);
			}
			action.start();
			action.run();
			options = action.getNextOptions();
			if (options.isEmpty()) {
				if (actionStack.isEmpty()) {
					action = null;
				} else {
					action = actionStack.pop().getFollowOnAction();
				}
			} else {
				actionStack.push(action);
				action = null;
			}
		}
		
		double[] retValue = new double[getAllPlayers().size()];
		for (int i = 1; i <= retValue.length; i++) {
			retValue[i-1] = getPlayer(i).getScore();
		}
		return retValue;
	}

}
