package hopshackle.simulation.test.refactor;

import hopshackle.simulation.*;

import java.util.*;

public class SimpleMazeGame implements Game<TestAgent, TestActionEnum>{

	int target = 0;
	TestAgent player;
	double[] reward = {0.0};

	public SimpleMazeGame(int target, TestAgent player) {
		this.target = target;
		this.player = player;
		TestActionEnum.defaultMakeNextDecision = false;
		TestActionEnum.waitTime = 0;
		player.setGame(this);
	}

	@Override
	public Game<TestAgent, TestActionEnum> clone(TestAgent perspectivePlayer) {
		World clonedWorld = new World();
		long currentTime = perspectivePlayer.getWorld().getCurrentTime();
		clonedWorld.setCalendar(new FastCalendar(currentTime));
		
		TestAgent clonedPlayer = new TestAgent(clonedWorld);
		clonedPlayer.setAge(perspectivePlayer.getAge());
		clonedPlayer.position = perspectivePlayer.position;
		
		SimpleMazeGame retValue = new SimpleMazeGame(target, clonedPlayer);
		retValue.reward[0] = reward[0];
		return retValue;
	}
	
	public boolean gameOver() {
		return player.position >= target;
	}

	@Override
	public double[] playGame() {
		do {
			oneMove();
		} while (!gameOver() && reward[0] > -100);
		player.die("Game Over");
		return reward;
	}
	
	public void oneMove() {
		Action<?> action = player.getDecider().decide(player);
		action.start();
		player.getWorld().setCurrentTime(player.getWorld().getCurrentTime() + 1000);
		action.run();
		reward[0] = reward[0] - 1.0;
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

}
