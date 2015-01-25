package hopshackle.simulation.metric;

import java.sql.*;
import java.util.logging.Logger;

public class MySQLMetric implements Metric {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private String sqlQuery, metricName;
	
	/**
	 * MySQLMetric is set up with a basic SQL text string
	 * @param sql
	 * No validation is conducted on creation that the SQL is valid
	 * Where a table needs to be specified, this must be marked with &Table
	 * 
	 * @param name
	 * This is purely used to identify the Metric for reporting
	 * It should be user-friendly
	 */
	public MySQLMetric(String name, String sql) {
		sqlQuery = sql;
		metricName = name;
	}

	public double getResult(DataSet ds) {
		if (!(ds instanceof MySQLDataSet)) {
			return 0.0;
		}
		MySQLDataSet dataSet = (MySQLDataSet) ds;

		ResultSet result = dataSet.getResultSet(sqlQuery);

		try {
			result.next();
			return result.getDouble(1);
		} catch (SQLException e) {
			logger.severe("Error in getting Result for Metric: " + this.toString());
			logger.severe(e.getMessage());
			System.err.println("Exception: " + e.getMessage());
			System.err.println(e.toString());
			return 0.0;
		}
	}

	public String toString() {
		return metricName;
	}
	
}
