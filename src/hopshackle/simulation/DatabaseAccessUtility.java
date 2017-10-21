package hopshackle.simulation;

import java.sql.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.*;

public class DatabaseAccessUtility implements Runnable{

	private Connection mainConnection;
	private BlockingQueue<String> queue;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private long timeout = (long) SimProperties.getPropertyAsDouble("DatabaseUtilityTimeout", "10");
	private volatile boolean done = false;
	private long startTime = System.currentTimeMillis();
	private List<DatabaseWriter> writers = new ArrayList<>();

	public DatabaseAccessUtility() {
		mainConnection = ConnectionFactory.getConnection();
		otherSetup();
	}

	public DatabaseAccessUtility(String db, String user, String password, String hostname) {
		mainConnection = ConnectionFactory.getConnection(db, user, password, hostname, true);
		otherSetup();
	}

	private void otherSetup(){
		queue = new LinkedBlockingQueue<String>();
	}

	public synchronized void addUpdate(String update) {
//		System.out.println("Adding to DBU: " +update);
		if (isAlive())
			queue.offer(update);
		else
			logger.severe("DBU getting new data after shut-down: " + update);
	}

	public void registerDatabaseWriter(DatabaseWriter<?> writer) {
		writers.add(writer);
	}
	public void flushWriters() {
		for (DatabaseWriter writer : writers) {
			writer.writeBuffer(this);
//			System.out.println("Flushing buffer for " + writer.toString());
		}
	}

	public boolean isAlive() {
		return !done;
	}

	public void run() {
		String nextUpdate = null;
		try {
			do { 
				nextUpdate = queue.poll(timeout, TimeUnit.MINUTES);
	//			System.out.println("Executing " + nextUpdate);
				if (nextUpdate == null || nextUpdate.equals("EXIT")) {
					done = true;
	//				System.out.println("DBU exiting at " + (System.currentTimeMillis()-startTime));
				} else {
					try {
						long start = System.currentTimeMillis();
						Statement st = mainConnection.createStatement();
						st.executeUpdate(nextUpdate);
						st.close();
						long finish = System.currentTimeMillis();
		//						System.out.println("DBU access time = " + (finish - start));
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
