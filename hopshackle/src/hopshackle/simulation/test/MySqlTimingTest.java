package hopshackle.simulation.test;

import hopshackle.simulation.*;

import java.sql.*;

public class MySqlTimingTest {

	private static Connection conn = ConnectionFactory.getConnection("junit", "root", "Metternich", true);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String tableCreation = "DROP TABLE IF EXISTS test_table;";
		try {
			Statement st = conn.createStatement();
			st.executeUpdate(tableCreation);
			tableCreation = "CREATE TABLE test_table ( " +
							"id 	INT		NOT NULL, " +
							"text	VARCHAR(20)	NOT NULL, " +
							"number	DOUBLE	NOT NULL);";
			st.executeUpdate(tableCreation);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		long startTime = System.currentTimeMillis();
		for (int loop = 1; loop <=1000; loop++) 
			writeData(loop);
		
		long middleTime = System.currentTimeMillis();
		
		for (int loop = 1; loop <= 1000; loop++)
			queryData();

		long endTime = System.currentTimeMillis();
		
		System.out.println("Writing took " + (middleTime - startTime));
		System.out.println("Reading took " + (endTime - middleTime));
	}

	public static void queryData() {
		try {
			Statement st = conn.createStatement();
			st.executeQuery("SELECT * FROM test_table where id = " + Dice.roll(1, 1000) + ";");
			st.close();
		} catch (SQLException e) { 
			e.printStackTrace();
		}
	
	}
	
	public static void writeData(int number) {
		try {
			Statement st = conn.createStatement();
			st.executeUpdate("INSERT INTO test_table SET id = " + number + ", text = 'hello world', number = 4.56;");
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
