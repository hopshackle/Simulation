package hopshackle.simulation;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class ConnectionFactory {

    protected static Logger logger = Logger.getLogger("hopshackle.simulation");

    public static synchronized Connection getConnection() {
        String password = SimProperties.getProperty("DatabasePassword", "");
        String user = SimProperties.getProperty("DatabaseUser", "");
        String schema = SimProperties.getProperty("DatabaseSchema", "");
        String hostname = SimProperties.getProperty("DatabaseHost", "");
        return getConnection(schema, user, password, hostname, false);
    }

    public static synchronized Connection getConnection(String db, String user, String password, String hostname, boolean truncate) {
        Connection con = null;
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", user);
        connectionProperties.setProperty("password", password);
        connectionProperties.setProperty("jdbcCompliantTruncation",
                Boolean.toString(truncate));

        String connectionStr = "jdbc:mysql://" + hostname + ":3306/" + db +" ?serverTimezone=UTC&useSSL=false";
        //		if (hostname == "") // NamedPipes more efficient if we're on the local machine
//			connectionStr = connectionStr + "?socketFactory=com.mysql.jdbc.NamedPipeSocketFactory";

//        String connectionStr = "jdbc:mysql:///" + db;

        try {
            //		Class.forName("com.mysql.cj.jdbc.Driver").newInstance();

            con = DriverManager.getConnection(connectionStr,
                    connectionProperties);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
            logger.info("Failed in attempt without pipes");
        }


        return con;

    }
}
