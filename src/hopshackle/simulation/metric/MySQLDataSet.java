package hopshackle.simulation.metric;

import hopshackle.simulation.ConnectionFactory;

import java.sql.*;
import java.util.logging.Logger;

public class MySQLDataSet implements DataSet {

	private String tableName;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private static Connection connection;

	public MySQLDataSet(String table) {
		tableName = table;
		if (connection == null)
			connection = ConnectionFactory.getConnection();
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
