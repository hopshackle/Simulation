package hopshackle.simulation.metric;

import hopshackle.simulation.ConnectionFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.logging.Logger;

public class MySQLDataSet implements DataSet {

	private String databaseName;
	private static HashMap<String, Connection> dbConnectionMap = new HashMap<String, Connection>();
	private String tableName;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private Connection connection;

	public MySQLDataSet(String db, String table, String user, String password) {
		databaseName = db;
		tableName = table;
		if (dbConnectionMap.containsKey(databaseName)) {
			connection = dbConnectionMap.get(databaseName);
		} else {
			connection = ConnectionFactory.getConnection(databaseName, user, password, true);
			dbConnectionMap.put(databaseName, connection);
		}
	}

	public ResultSet getResultSet(String sqlQuery) {

		sqlQuery = sqlQuery.replaceAll("&Table", tableName);

		try {
			Statement s = connection.createStatement();
			return s.executeQuery(sqlQuery);
		} catch (SQLException e) {
			logger.severe("Error in getting Result for DataSet: " + this.toString());
			logger.severe(e.getMessage());
			System.err.println("Exception: " + e.getMessage());
			System.err.println(e.toString());
			return null;
		}
	}

	public String toString() {
		return tableName;
	}
}
