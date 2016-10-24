package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.ConnectionFactory;
import hopshackle.simulation.metric.MySQLResultSet;

import java.sql.*;

import org.junit.*;

public class MySQLResultsetTest {

	private Connection conn;

	@Before
	public void setUp() throws Exception {
		conn = ConnectionFactory.getConnection("junit", "root", "Metternich", "", false);
	}

	@Test
	public void testVanilla() {
		/*
		 * Set up a tets dataset, and check write it away
		 *
		 * then check that results are as expected
		 */

		String[] metricNames = new String[] {"metric1", "metric2", "metric3"};
		String[][] dsNames = new String[][] {new String[] {"run1", "run2", "run3"}, new String[]{"run1a", "run2a", "run3a"}};
		String[] dsgNames = new String[] {"dsGroup1", "dsGroup2"};
		double[][][] values = new double[2][3][3];
		for (int n=0; n<3; n++) 
			for (int m=0; m<3; m++)
				for (int p=0; p<2; p++)
					values[p][n][m] = (double)(p+1) * (double)(n+1) / (double)(m+1);

		try {
			new MySQLResultSet("junit", dsgNames, dsNames, metricNames, values);

			String sqlQuery = "SELECT * from datasetgroupanalysis WHERE " +
			"datasetGroup = 'dsGroup1' " +
			"AND dataset = 'run1' AND metric = 'metric2'";

			Statement st = conn.createStatement();

			ResultSet rs = st.executeQuery(sqlQuery);

			rs.next();
			assertEquals(rs.getDouble(4), 0.50, 0.00001);
			assertTrue(rs.getString(1).equals("dsGroup1"));
			assertTrue(rs.getString(2).equals("run1"));
			assertTrue(rs.getString(3).equals("metric2"));

			rs.close();

			sqlQuery = "SELECT * from datasetgroupanalysis WHERE " +
			"datasetGroup = 'dsGroup2' " +
			"AND dataset = 'run1a' AND metric = 'metric3'";
			rs = st.executeQuery(sqlQuery);
			rs.next();
			assertEquals(rs.getDouble(4), 0.66666666667, 0.00001);
			assertTrue(rs.getString(1).equals("dsGroup2"));
			assertTrue(rs.getString(2).equals("run1a"));
			assertTrue(rs.getString(3).equals("metric3"));



		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void testThreeDatasets() {
		testVanilla();

		String sqlQuery = "SELECT * from datasetgroupanalysis WHERE " +
		"datasetGroup = 'dsGroup1' " +
		"AND dataset = 'run3' AND metric = 'metric3'";

		try {
			Statement st = conn.createStatement();

			ResultSet rs = st.executeQuery(sqlQuery);

			rs.next();
			assertEquals(rs.getDouble(4), 1.0000, 0.00001);
			assertTrue(rs.getString(1).equals("dsGroup1"));
			assertTrue(rs.getString(2).equals("run3"));
			assertTrue(rs.getString(3).equals("metric3"));

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void testVanillaSummary() {
		/*
		 * Set up a tets dataset, and check write it away
		 *
		 * then check that results are as expected for summary statistics
		 */
		testVanilla();

		try {
			String sqlQuery = "SELECT * from dsgsummaryanalysis WHERE " +
			"metric = 'metric2'";


			Statement st = conn.createStatement();

			ResultSet rs = st.executeQuery(sqlQuery);

			rs.next();
			assertTrue(rs.getString(1).equals("dsGroup1"));
			assertTrue(rs.getString(2).equals("metric2"));
			assertEquals(rs.getDouble(3), 1.0000, 0.0001);
			assertEquals(rs.getDouble(4), 0.25, 0.00001);
			assertEquals(rs.getDouble(5), 0.50, 0.0001);

		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void testVanillaSig() {
		testVanilla();

		try {
			String sqlQuery = "SELECT * from siganalysis WHERE " +
			"metric = 'metric2'";


			Statement st = conn.createStatement();

			ResultSet rs = st.executeQuery(sqlQuery);

			rs.next();
			assertTrue(rs.getString(1).equals("dsGroup1"));
			assertTrue(rs.getString(2).equals("dsGroup2"));
			assertTrue(rs.getString(3).equals("metric2"));
			assertEquals(rs.getDouble(4), 0.22088, 0.0001);

		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@After
	public void tearDown() throws Exception {

		try {
			String sqlQuery = "DROP TABLE datasetgroupanalysis;";
			Statement st = conn.createStatement();
			st.executeUpdate(sqlQuery);

			sqlQuery = "DROP TABLE dsgsummaryanalysis;";
			st = conn.createStatement();
			st.executeUpdate(sqlQuery);

			sqlQuery = "DROP TABLE sigAnalysis;";
			st = conn.createStatement();
			st.executeUpdate(sqlQuery);

		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}

}
