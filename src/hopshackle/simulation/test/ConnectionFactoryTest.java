package hopshackle.simulation.test;

import hopshackle.simulation.ConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class ConnectionFactoryTest {
	
	Connection c;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testVanilla() {
		c = ConnectionFactory.getConnection();
		assertNotNull(c);
	}

	@Test
	public void testFailure() {
		c = ConnectionFactory.getConnection("dummy", "who", "noidea", "", true);
		assertNull(c);
	}

	@Test
	public void testVariants() {
		c = ConnectionFactory.getConnection("junit", "root", "Metternich", "", false);
		assertTrue (c != null);
		try {
			c.close();
		} catch (SQLException e) {
		}

		c = ConnectionFactory.getConnection("junit", "root", "Metternich", "", true);
		assertTrue (c != null);
		try {
			c.close();
		} catch (SQLException e) {
		}
	}

	@Test
	public void moreThanFiftyConnections() {
		Connection[] c = new Connection[50];
		for (int loop = 0; loop<50; loop++) {
			c[loop] = ConnectionFactory.getConnection();
			assertTrue(c[loop] !=null);
		}
		try {
			for (int loop = 0; loop<50; loop++) {
				c[loop].close();
			}
		} catch (SQLException e) {
		}
	}
	
	@After
	public void tearDown() throws Exception {
		try {
			if (c != null && !c.isClosed())
				c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
