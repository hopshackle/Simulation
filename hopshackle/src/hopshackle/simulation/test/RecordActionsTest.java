package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;
import hopshackle.simulation.dnd.actions.*;

import java.sql.*;

import org.junit.*;
public class RecordActionsTest {

	private World w;
	private Connection con;
	private Character ftr;

	@Before
	public void setUp() throws Exception {
		w = new World(new ActionProcessor(), "JUNIT", 2*60000L);
		w.setCalendar(new FastCalendar(0L));
		w.setLocationMap(new SquareMap(1, 1));
		DatabaseAccessUtility dbu = new DatabaseAccessUtility("junit", "root", "Metternich");
		Thread t = new Thread(dbu);
		t.start();
		w.setDatabaseAccessUtility(dbu);
		con = ConnectionFactory.getConnection("junit", "root", "Metternich", true);
		ftr = new Character(Race.HUMAN, CharacterClass.FIGHTER, w);
	}

	@After
	public void tearDown() throws Exception {
		w.updateDatabase("DELETE FROM actions_junit");
		w.updateDatabase("EXIT");
		con.close();
	}


	private void wasteTime() {
		String timeWaster = "1";
		for (int loop=0; loop< 10000; loop++) {
			timeWaster += loop;
		}
	}


	@Test
	public void testTableCreation() {
		wasteTime();
		try {
			Statement st = con.createStatement();
			String sqlQuery = "SELECT * FROM Actions_JUNIT";

			ResultSet rs = st.executeQuery(sqlQuery);
			assertFalse(rs.first());
			assertFalse(rs.next());
			// i.e. has no records - and doesn't give Table not found error

			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void testRecordMultipleActions() {
		new DoNothing(ftr, 0);
		new DoNothing(ftr, 0);

		w.setCurrentTime(w.getCurrentTime()+59500);

		new Rest(ftr, true);
		
		testTableCreation();
		// checks that no data has yet been written

		w.setCurrentTime(w.getCurrentTime()+1000);
		new DoNothing(ftr, 0);

		wasteTime();
		// should have written away the results above
		try {
			Statement st = con.createStatement();
			String sqlQuery = "SELECT * FROM Actions_JUNIT";

			ResultSet rs = st.executeQuery(sqlQuery);
			assertTrue(rs.first());
			for (int loop=0; loop<2; loop++) {
				String actionType = rs.getString(2);
				if (actionType.equals("STAY")) {
					assertTrue(rs.getString(3).equals("FIGHTER"));
					assertEquals(rs.getInt(4), 3);
				} else if (actionType.equals("REST")) {
					assertTrue(rs.getString(3).equals("FIGHTER"));
					assertEquals(rs.getInt(4), 1);
				} else {
					assertFalse(true);
				}
				rs.next();
			}
			assertFalse(rs.next());
			assertTrue(rs.isAfterLast());
			// i.e. has no records - and doesn't give Table not found error

			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
			assertFalse(true);
		}
	}

}
