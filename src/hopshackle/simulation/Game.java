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

		while (!gameOver()) {
			oneAction();
		}
		double[] retValue = new double[getAllPlayers().size()];
		for (int i = 1; i <= retValue.length; i++) {
			retValue[i-1] = getPlayer(i).getScore();
		}
		return retValue;
	}

	public final void oneAction() {
		oneAction(false, false);
	}

	public final void oneAction(boolean noUpdate, boolean singleAction) {
		P currentPlayer = null;
		List<ActionEnum<P>> options = null, startOptions = null, startStartOptions = null;
		Decider<P> decider = null;
		Action<P> action = null, startAction = null, startStartAction = null;

		do {
			startStartOptions = startOptions;
			startStartAction = startAction;
			startOptions = options;
			startAction = action;
			if (action != null) {
				// this occurs if we popped an action off the stack on the last iteration
				// so we already have the action we wish to execute
			} else {
				// we have completed the last one, so pick a new action
				if (!actionStack.isEmpty()) { // actionStack first, to complete interrupts and consequences
					options = actionStack.peek().getNextOptions();
					currentPlayer = actionStack.peek().getNextActor();
					decider = currentPlayer.getDecider();
					action = decider.decide(currentPlayer, options);
				} else {	// otherwise we get a new action
					currentPlayer = getCurrentPlayer();
					options = getPossibleCurrentActions();
					decider = currentPlayer.getDecider();
					action = decider.decide(currentPlayer, options);
				}
			}

			action.addToAllPlans(); // this is for compatibility with Action statuses in a real-time simulation
			action.start();
			action.run();
			options = action.getNextOptions();
			if (options.isEmpty()) {
				if (actionStack.isEmpty()) {
					action = null;
				} else {
					do {
						action = actionStack.pop().getFollowOnAction();
					} while (action == null && !actionStack.isEmpty());
				}
			} else {
				actionStack.push(action);
				action = null;
			}

			if (action == null && actionStack.isEmpty() && !noUpdate)
				updateGameStatus(); // finished the last cycle, so move game state forward
			// otherwise, we still have interrupts/consequences to deal with

			if (singleAction && action == null) break;

		} while (action != null || !actionStack.isEmpty());
	}

}

