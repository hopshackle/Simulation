package hopshackle.simulation.test;

import static org.junit.Assert.assertEquals;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.sql.*;

import org.junit.*;

public class CharacterDAOTest {
	
	private AgentWriter<Character> characterWriter;
	private World w;
	private Thread t;
	private DatabaseAccessUtility testJunit;
	
	@Before
	public void setUp() {
		testJunit = new DatabaseAccessUtility("junit", "root", "Metternich");
		t = new Thread(testJunit);
		t.start();
		clearTable();
		
		w = new World();
		w.setDatabaseAccessUtility(testJunit);
		w.setName("test");
		characterWriter = new AgentWriter<Character>(new CharacterDAO());
		Character.setAgentWriter(characterWriter);
	}
	
	@After
	public void tearDown() {
		characterWriter.writeBuffer(w);
		testJunit.addUpdate("EXIT");
	}
	
	private void clearTable() {
		String sqlUpdate = "DELETE FROM Allchr_test;";
		testJunit.addUpdate(sqlUpdate);
	}

	@Test
	public void characterDeathUpdatesTable() {
		characterWriter.setBufferLimit(1);
		assertEquals(recordsInTable(), 0);
		createAndKillCharacter();
		assertEquals(recordsInTable(), 1);
	}

	@Test
	public void characterDeathHeldInBuffer() {
		characterWriter.setBufferLimit(2);
		assertEquals(recordsInTable(), 0);
		createAndKillCharacter();
		assertEquals(recordsInTable(), 0);
		characterWriter.writeBuffer(w);
		assertEquals(recordsInTable(), 1);
	}

	@Test
	public void characterDeathUpdatesTableWithDelay() {
		characterWriter.setBufferLimit(2);
		assertEquals(recordsInTable(), 0);
		createAndKillCharacter();
		createAndKillCharacter();
		assertEquals(recordsInTable(), 2);
	}

	@Test
	public void tableUpdatedOnWorldDeath() {
		characterWriter.setBufferLimit(10);
		assertEquals(recordsInTable(), 0);
		createAndKillCharacter();
		createAndKillCharacter();
		assertEquals(recordsInTable(), 0);
		w.worldDeath();
		assertEquals(recordsInTable(), 2);
	}

	private int recordsInTable() {
		wasteTime();
		int counter = 0;
		Connection tempConn = ConnectionFactory.getConnection("junit", "root", "Metternich", true);
		String sqlQuery = "SELECT * FROM AllChr_test;";
		try {
			Statement st = tempConn.createStatement();
			ResultSet rs = st.executeQuery(sqlQuery);
			if (!rs.first())
				return 0;	// table empty

			do {
				rs.next();
				counter++;
			} while (!rs.isAfterLast());
		
			
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;	// table does not exist
		}  
		
		return counter;
	}
	
	private void createAndKillCharacter() {
		Character victim = new Character(w);
		victim.die("Oops");
	}
	
	private void wasteTime() {
		String timeWaster = "1";
		for (int loop=0; loop<13000; loop++) {
			timeWaster += loop;
		}
	}
}
