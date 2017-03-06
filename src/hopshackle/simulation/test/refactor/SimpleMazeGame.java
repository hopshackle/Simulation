package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;

import java.util.*;

public class SimpleMazeGame extends Game<TestAgent, ActionEnum<TestAgent>>{

	int target = 0;
	int numberOfPlayers;
	int playerToMove;
	TestAgent[] players;
	double[] reward;
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));

	public SimpleMazeGame(int target, TestAgent player) {
		this(target, new TestAgent[] {player});
	}

	public SimpleMazeGame(int target, TestAgent[] players) {
		numberOfPlayers = players.length;
		this.target = target;
		this.players = new TestAgent[numberOfPlayers];
		reward = new double[numberOfPlayers];
		TestActionEnum.defaultMakeNextDecision = false;
		TestActionEnum.waitTime = 0;
		for (int i = 0; i <numberOfPlayers; i++) {
			this.players[i] = players[i];
			this.players[i].setGame(this);
		}
		playerToMove = 0;
	}

	@Override
	public Game<TestAgent, ActionEnum<TestAgent>> clone(TestAgent perspectivePlayer) {
		World clonedWorld = new World();
		long currentTime = perspectivePlayer.getWorld().getCurrentTime();
		clonedWorld.setCalendar(new FastCalendar(currentTime));

		TestAgent[] clonedPlayers = new TestAgent[numberOfPlayers];
		for (int i = 0; i < numberOfPlayers; i++) {
			TestAgent original = players[i];
			TestAgent clonedPlayer = new TestAgent(clonedWorld);
			clonedPlayer.setAge(original.getAge());
			clonedPlayer.position = original.position;
			clonedPlayer.addGold(original.getGold());
			clonedPlayers[i] = clonedPlayer;
		}
		
		SimpleMazeGame retValue = new SimpleMazeGame(target, clonedPlayers);
		retValue.playerToMove = this.playerToMove;
		retValue.reward = new double[numberOfPlayers];
		for (int i = 0; i < numberOfPlayers; i++) {
			retValue.reward[i] = this.reward[i];
		}
		return retValue;
	}

	@Override
	public boolean gameOver() {
		for (int i = 0; i < numberOfPlayers; i++) {
			TestAgent player = players[i];
			if (player.position >= target || player.decisionsTaken > 100){
				return true;
			}
		}
		return false;
	}

	@Override
	protected void endOfGameHouseKeeping() {

	}

	public void oneMove() {
		Action<?> action = players[playerToMove].getDecider().decide(players[playerToMove], HopshackleUtilities.convertList(allActions));
		action.start();
		action.run();
		nextPlayer();
	}

	private void nextPlayer() {
		playerToMove++;
		if (playerToMove == numberOfPlayers) playerToMove = 0;
	}

	@Override
	public TestAgent getCurrentPlayer() {
		return players[playerToMove];
	}

	@Override
	public int getCurrentPlayerNumber() {
		return playerToMove+1;
	}

	@Override
	public List<TestAgent> getAllPlayers() {
		List<TestAgent> retValue = new ArrayList<TestAgent>();
		for (int i = 0; i < numberOfPlayers; i++) 
			retValue.add(players[i]);
		return retValue;
	}

	@Override
	public int getPlayerNumber(TestAgent player) {
		for (int i = 0; i < numberOfPlayers; i++) {
			if (player == players[i]) return i+1;
		}
		return 0;
	}

	@Override
	public TestAgent getPlayer(int n) {
		return players[n-1];
	}

	@Override
	public List<ActionEnum<TestAgent>> getPossibleActions(TestAgent a) {
		return allActions;
	}

	@Override
	public void updateGameStatus() {
		nextPlayer();
	}
}
