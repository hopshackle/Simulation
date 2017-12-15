package hopshackle.simulation;

import java.sql.Connection;
import java.util.Hashtable;
import java.util.logging.Logger;

public class RecordActions {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private Hashtable<String, Integer> actionRecord;
	public static String newline = System.getProperty("line.separator");
	private Connection con;
	private Long originalStartTime, lastRecordTime;
	private World w;
	private boolean tableCreated = false;

	public RecordActions(World world) {
		w = world;
		actionRecord = new Hashtable<String, Integer>();
		originalStartTime = 0L;
		lastRecordTime = originalStartTime;
	}

	private void createTable() {
		String sqlQuery = "DROP TABLE IF EXISTS Actions_" + w.toString() +";";
		w.updateDatabase(sqlQuery);

		sqlQuery = "CREATE TABLE IF NOT EXISTS Actions_" + w.toString()  +
				" ( Minute 		INT			NOT NULL,"		+
				" Action	VARCHAR(50)	NOT NULL,"		+
				" Agent		VARCHAR(50) NOT NULL,"	+
				" Number	INT			NOT NULL"		+
				");";

		w.updateDatabase(sqlQuery);
	}

	public synchronized void recordAction(Action a) {
		if (!tableCreated) {
			createTable();
			tableCreated = true;
		}
		String actionName = a.getType().toString();
		String s = actionName + ":" + ((a.getActor() == null) ? "null" : a.getActor().getType());
		if (actionName==null) return;
		Integer i = 0;
		if (actionRecord.containsKey(s)) 
			i = actionRecord.get(s);
		i++;
		actionRecord.put(s, i);
		World w = a.getActor().getWorld();
		Long t = w.getCurrentTime();

		if (w.getCurrentTime() - lastRecordTime > 60000) {
			int minute = (int)((t - originalStartTime)/60000);
			lastRecordTime = t;
			String suffix = "NULL";
			if (w != null) 
				suffix = w.toString();

			boolean hasEntries = false;
			StringBuffer sqlQuery = new StringBuffer("INSERT INTO Actions_" + suffix + 
					" (Minute, Action, Agent, Number) VALUES ");
			for(String temp : actionRecord.keySet()) {
				String[] t2 = temp.split(":");
				if (hasEntries) sqlQuery.append(", ");
				sqlQuery.append(String.format(" (%d, '%s', '%s', %d)", 
						minute, t2[0], t2[1], actionRecord.get(temp)));
				hasEntries = true;
			}
			w.updateDatabase(sqlQuery.toString());
			actionRecord.clear();
		}
	}
}
