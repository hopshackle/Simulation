package hopshackle.simulation.test;


import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.sql.*;

import org.junit.*;

public class DatabaseServiceTest {
	
	DatabaseAccessUtility testJunit;
	Thread t;
	
	@Before
	public void setUp() throws Exception {
		testJunit = new DatabaseAccessUtility("junit", "root", "Metternich", "");
		t = new Thread(testJunit);
		t.start();
		testJunit.addUpdate("DROP TABLE IF EXISTS dau_test;");
	}

	@After
	public void tearDown() throws Exception {
		testJunit.addUpdate("EXIT");
	}
	
	private void wasteTime() {
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testDefaultAccess() {
		DatabaseAccessUtility testDefault = new DatabaseAccessUtility();
	}
	
	@Test
	public void testCreatingTable() {
		String sqlUpdate;
		sqlUpdate = "CREATE TABLE IF NOT EXISTS dau_test (id INT NOT NULL, text VARCHAR(20) NOT NULL);";
		testJunit.addUpdate(sqlUpdate);
		wasteTime();
		Connection tempConn = ConnectionFactory.getConnection("junit", "root", "Metternich", "", true);
		String sqlQuery = "SELECT * FROM dau_test;";
		try {
			Statement st = tempConn.createStatement();
			ResultSet rs = st.executeQuery(sqlQuery);
			assertFalse(rs.first());
		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	@Test
	public void testWritingData() {
		String sqlUpdate;
		sqlUpdate = "CREATE TABLE IF NOT EXISTS dau_test (id INT NOT NULL, text VARCHAR(20) NOT NULL);";
		testJunit.addUpdate(sqlUpdate);
		wasteTime();
		sqlUpdate = "INSERT INTO dau_test SET id = 1, text = 'Hello';";
		testJunit.addUpdate(sqlUpdate);
		Connection tempConn = ConnectionFactory.getConnection("junit", "root", "Metternich", "", true);
		wasteTime();
		String sqlQuery = "SELECT * FROM dau_test;";
		try {
			Statement st = tempConn.createStatement();
			ResultSet rs = st.executeQuery(sqlQuery);
			assertTrue(rs.first());
			assertEquals("Hello", rs.getString("text"));
		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	@Test
	public void testErrorInSql() {
		String sqlUpdate;
		sqlUpdate = "CREATE TABLE IF NOT EXISTS dau_test (id INT NOT NULL, text STRING NOT NULL);";
		testJunit.addUpdate(sqlUpdate);
		wasteTime();
		Connection tempConn = ConnectionFactory.getConnection("junit", "root", "Metternich", "", true);
		String sqlQuery = "SELECT * FROM dau_test;";
		try {
			Statement st = tempConn.createStatement();
			ResultSet rs = st.executeQuery(sqlQuery);
			assertFalse(rs.first());
		} catch (SQLException e) {
			assertTrue(true);
			assertTrue(testJunit.isAlive());
			return;
		}
		assertTrue(false);
	}
	
	@Test
	public void testVolumeUpdate() {
		testCreatingTable();
		long startTime = System.currentTimeMillis();
		for (int loop = 1; loop <= 200; loop++) 
			testJunit.addUpdate("INSERT INTO dau_test SET id = " + loop + ", text = 'Hello';");
		assertTrue(System.currentTimeMillis() - startTime < 100);
		
		int firstRecord = getCurrentRecordWritten();
		wasteTime();
		int secondRecord = getCurrentRecordWritten();
		assertTrue(secondRecord > firstRecord + 20);
	}
	
	private int getCurrentRecordWritten() {
		Connection tempConn = ConnectionFactory.getConnection("junit", "root", "Metternich", "", true);
		String sqlQuery = "SELECT * FROM dau_test;";
		int currentRecordWritten = 0;
		try {
			Statement st = tempConn.createStatement();
			ResultSet rs = st.executeQuery(sqlQuery);
			rs.last();
			currentRecordWritten = rs.getInt("id");
		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
			return 0;
		}
		return currentRecordWritten;
	}
	
	@Test
	public void testExit() {
		assertTrue(testJunit.isAlive());
		testJunit.addUpdate("EXIT");
		wasteTime();
		assertFalse(testJunit.isAlive());
	}
}
