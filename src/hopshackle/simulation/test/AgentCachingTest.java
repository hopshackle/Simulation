package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import org.junit.*;

public class AgentCachingTest {
	
	private BasicAgent testAgent;
	private World world;
	private DatabaseAccessUtility testJunit;

	@Before
	public void setUp() {
        AgentArchive.switchOn(true);
		world = new World();
		world.setName("testWorld");
		SimProperties.setProperty("DatabaseWriterBufferLimit", "1");
		testJunit = new DatabaseAccessUtility("junit", "root", "Metternich", "");
		Thread t = new Thread(testJunit);
		t.start();
		world.setDatabaseAccessUtility(testJunit);
		AgentArchive.clearAndResetCacheBuffer(1);
		testAgent = new BasicAgent(world);
	}
	
	@Test
	public void livingAgentIsReturnedFromCache() {
		Agent agentReturnedFromCache = AgentArchive.getAgent(testAgent.getUniqueID());
		assertEquals(agentReturnedFromCache, testAgent);
	}
	
	@Test
	public void nullReturnedIfAgentNotKnown() {
		Agent agentReturnedFromCache = AgentArchive.getAgent(2302739);
		assertTrue(agentReturnedFromCache == null);
	}
	
	@Test
	public void deadAgentIsReturnedFromCacheOfTheDead() {
		testAgent.die("ooops");
		BasicAgent test2 = new BasicAgent(world);
		test2.die("Ooops");
		wasteTime();
		wasteTime();
		Agent agentReturnedFromCache = AgentArchive.getAgent(testAgent.getUniqueID());
		assertTrue(agentReturnedFromCache == null);
		AgentRetriever<BasicAgent> agentRetriever = new BasicAgentRetriever(ConnectionFactory.getConnection("junit", "root", "Metternich", "", true));
		agentReturnedFromCache = AgentArchive.getAgent(testAgent.getUniqueID(), agentRetriever, world);
		assertTrue(agentReturnedFromCache != null);
		assertFalse(agentReturnedFromCache == testAgent);
		assertEquals(agentReturnedFromCache.getUniqueID(), testAgent.getUniqueID());
		agentRetriever.closeConnection();
	}
	
	@Test
	public void agentIsRemovedFromCacheOnDeathIfNotStoredInDatabase() {
		testJunit.addUpdate("EXIT");
		testAgent.die("ooops");
		BasicAgent test2 = new BasicAgent(world);
		test2.die("Ooops");
		wasteTime();
		Agent agentReturnedFromCache = AgentArchive.getAgent(testAgent.getUniqueID());
		assertTrue(agentReturnedFromCache == null);
	}
	
	private void wasteTime() {
		String timeWaster = "1";
		for (int loop=0; loop<12000; loop++) {
			timeWaster += loop;
		}
	}
}
