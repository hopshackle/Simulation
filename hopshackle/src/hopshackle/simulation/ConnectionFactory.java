package hopshackle.simulation;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class ConnectionFactory {

	protected static String password = SimProperties.getProperty("DatabasePassword", "");
	protected static String user = SimProperties.getProperty("DatabaseUser", "");
	protected static String schema = SimProperties.getProperty("DatabaseSchema", "");
	protected static String hostname = SimProperties.getProperty("DatabaseHost", "");
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");

	public static synchronized Connection getConnection() {
		return getConnection(schema, user, password, hostname, false);
	}

	public static synchronized Connection getConnection(String db, String user, String password, String hostname, boolean truncate) {
		Connection con = null;
		Properties connectionProperties = new Properties();
		connectionProperties.setProperty("user", user);
		connectionProperties.setProperty("password", password);
		connectionProperties.setProperty("jdbcCompliantTruncation",
				Boolean.toString(truncate));

		String connectionStr = "jdbc:mysql://" + hostname + "/" + db;
		if (hostname == "") // NamedPipes more efficient if we're on the local machine
			connectionStr = connectionStr + "?socketFactory=com.mysql.jdbc.NamedPipeSocketFactory";

		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();

			con = DriverManager.getConnection(connectionStr,
					connectionProperties);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logger.severe(e.getMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			logger.severe(e.getMessage());
		} catch (InstantiationException e) {
			e.printStackTrace();
			logger.severe(e.getMessage());
		} catch (SQLException e) {
			// try once more without named pipe
			connectionStr = "jdbc:mysql:///" + db;
			try {
				con = DriverManager.getConnection(connectionStr,
						connectionProperties);
			} catch (SQLException e1) {
				logger.info("Failed in attempt with pipes");
				e.printStackTrace();
				logger.severe(e.getMessage());
				logger.info("Failed in attempt without pipes");
				e1.printStackTrace();
				logger.severe(e1.getMessage());
			}
		}

		return con;

	}
}
