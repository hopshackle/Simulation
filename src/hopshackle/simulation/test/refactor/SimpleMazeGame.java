package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;

import java.util.*;

public class SimpleMazeGame extends Game<TestAgent, ActionEnum<TestAgent>>{

	int target = 0;
	TestAgent player;
	double[] reward = {0.0};
	List<ActionEnum<TestAgent>> allActions = new ArrayList<ActionEnum<TestAgent>>(EnumSet.allOf(TestActionEnum.class));


	public SimpleMazeGame(int target, TestAgent player) {
		this.target = target;
		this.player = player;
		TestActionEnum.defaultMakeNextDecision = false;
		TestActionEnum.waitTime = 0;
		player.setGame(this);
	}

	@Override
	public Game<TestAgent, ActionEnum<TestAgent>> clone(TestAgent perspectivePlayer) {
		World clonedWorld = new World();
		long currentTime = perspectivePlayer.getWorld().getCurrentTime();
		clonedWorld.setCalendar(new FastCalendar(currentTime));
		
		TestAgent clonedPlayer = new TestAgent(clonedWorld);
		clonedPlayer.setAge(perspectivePlayer.getAge());
		clonedPlayer.position = perspectivePlayer.position;
		
		SimpleMazeGame retValue = new SimpleMazeGame(target, clonedPlayer);
		retValue.reward[0] = this.reward[0];
		return retValue;
	}
	
	public boolean gameOver() {
		boolean retValue  = (player.position >= target || reward[0] < -100);
		if (retValue && !player.isDead())
			player.die("End of game");
		return retValue;
	}
	
	public void oneMove() {
		Action<?> action = player.getDecider().decide(player, HopshackleUtilities.convertList(allActions));
		action.start();
		action.run();
	}

	@Override
	public TestAgent getCurrentPlayer() {
		return player;
	}

	@Override
	public int getCurrentPlayerNumber() {
		return 1;
	}

	@Override
	public List<TestAgent> getAllPlayers() {
		List<TestAgent> retValue = new ArrayList<TestAgent>();
		retValue.add(player);
		return retValue;
	}

	@Override
	public int getPlayerNumber(TestAgent player) {
		if (player == this.player)
			return 1;
		return 0;
	}

	@Override
	public TestAgent getPlayer(int n) {
		if (n == 1) return player;
		return null;
	}

	@Override
	public List<ActionEnum<TestAgent>> getPossibleCurrentActions() {
		return allActions;
	}

	@Override
	public void updateGameStatus() {
		if (gameOver()) {
			player.die("Game Over");
		}
	}

}
