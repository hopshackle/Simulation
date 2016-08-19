package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.metric.*;

import java.sql.*;

import org.junit.*;

public class MySQLMetricTest {
	
	Connection con;

	@Before
	public void setUp() throws Exception {
		SimProperties.setProperty("DatabaseSchema", "junit");
		SimProperties.setProperty("DatabasePassword", "Metternich");
		SimProperties.setProperty("DatabaseUser", "root");
		con = ConnectionFactory.getConnection("junit", "root", "Metternich", "", true);
		try {
			Statement st = con.createStatement();

			String sqlQuery = "CREATE TABLE IF NOT EXISTS MySQLMetricTest " +
			" ( testStr 		VARCHAR(50)	NOT NULL,"		+
			" testInt		INT			NOT NULL" +
			");";

			st.executeUpdate(sqlQuery);

			sqlQuery = "INSERT INTO MySQLMetricTest (testStr, testInt) VALUES " +
			"('A', 1), ('B', 2), ('C', 5), ('Default', 0);";

			st.executeUpdate(sqlQuery);

			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testVanilla() {
		String sqlQuery = "SELECT MAX(testInt) FROM &Table";
		MySQLMetric testMetric = new MySQLMetric("TestMetric", sqlQuery);
		
		MySQLDataSet testDataSet = new MySQLDataSet("MySQLMetricTest");
		
		assertTrue(testMetric.toString().equals("TestMetric"));
		assertEquals(testMetric.getResult(testDataSet), 5.0, 0.0001);
	}

	@Test
	public void testFailure() {
		String sqlQuery = "SELECT TestStr FROM MySQLMetricTest";
		MySQLMetric testMetric = new MySQLMetric("TestMetric", sqlQuery);
		
		MySQLDataSet testDataSet = new MySQLDataSet("MySQLMetricTest");
		
		assertTrue(testMetric.toString().equals("TestMetric"));
		assertEquals(testMetric.getResult(testDataSet), 0.0, 0.0001);
	}

	@After
	public void tearDown() throws Exception {
		try {
			Statement st = con.createStatement();

			String sqlQuery = "DROP TABLE IF  EXISTS MySQLMetricTest;";

			st.executeUpdate(sqlQuery);

			st.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

}
