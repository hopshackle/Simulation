package hopshackle.simulation;

import java.sql.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class DatabaseAccessUtility implements Runnable{

	private Connection mainConnection;
	private BlockingQueue<String> queue;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private boolean done = false;
	private long startTime = System.currentTimeMillis();

	public DatabaseAccessUtility() {
		mainConnection = ConnectionFactory.getConnection();
		otherSetup();
	}

	public DatabaseAccessUtility(String db, String user, String password) {
		mainConnection = ConnectionFactory.getConnection(db, user, password, true);
		otherSetup();
	}

	private void otherSetup(){
		queue = new LinkedBlockingQueue<String>();
	}

	public synchronized void addUpdate(String update) {
		if (isAlive())
			queue.offer(update);
		else
			logger.severe("DBU getting new data after shut-down: " + update);
	}

	public boolean isAlive() {
		return !done;
	}

	public void run() {
		String nextUpdate = null;
		try {
			do { 
				nextUpdate = queue.poll(10, TimeUnit.MINUTES);
				if (nextUpdate == null || nextUpdate.equals("EXIT")) {
					done = true;
					System.out.println("DBU exiting at " + (System.currentTimeMillis()-startTime));
				} else {
					try {
						long start = System.currentTimeMillis();
						Statement st = mainConnection.createStatement();
						st.executeUpdate(nextUpdate);
						st.close();
						long finish = System.currentTimeMillis();
						//		System.out.println("DBU access time = " + (finish - start));
						nextUpdate = null;
					} catch (SQLException e) {
						logger.severe("DBU: Invalid SQL: " + nextUpdate);
						logger.severe("DBU: SQL Error: " + e.toString());
						e.printStackTrace();
					}
				}
			} while (!done);
		} catch (InterruptedException e) {
			logger.severe("DatabaseAccessUtility interrupted: " + e.toString());
			e.printStackTrace();
		} 
		try {
			mainConnection.close();
		} catch (SQLException e) {
			logger.severe("Error closing connection in DatabaseAccessUtility:" + e.toString());
			e.printStackTrace();
		}
	}

	public boolean isCongested() {
		return queue.size() > 20;
	}

}
