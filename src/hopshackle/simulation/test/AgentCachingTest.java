package hopshackle.simulation.test;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;
import hopshackle.simulation.basic.BasicAgentRetriever;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AgentCachingTest {

    private BasicAgent testAgent;
    private World world;
    private DatabaseAccessUtility testJunit;
    private Thread dbuThread;

    @Before
    public void setUp() {
        AgentArchive.switchOn(true);
        world = new World();
        world.setName("testWorld");
        SimProperties.setProperty("DatabaseWriterBufferLimit", "1");
        testJunit = new DatabaseAccessUtility("junit", "root", "Metternich", "");
        dbuThread = new Thread(testJunit);
        dbuThread.start();
        world.setDatabaseAccessUtility(testJunit);
        BasicAgent.setDBU(testJunit);
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
        assertNull(agentReturnedFromCache);
    }

    @Test
    public void deadAgentIsReturnedFromCacheOfTheDead() {
        testAgent.die("ooops");
        BasicAgent test2 = new BasicAgent(world);
        test2.die("Ooops");
        wasteTime();
        Agent agentReturnedFromCache = AgentArchive.getAgent(testAgent.getUniqueID());
        assertNull(agentReturnedFromCache);
        AgentRetriever<BasicAgent> agentRetriever = new BasicAgentRetriever(ConnectionFactory.getConnection("junit", "root", "Metternich", "", true));
        agentReturnedFromCache = AgentArchive.getAgent(testAgent.getUniqueID(), agentRetriever, world);
        assertNotNull(agentReturnedFromCache);
        assertNotSame(agentReturnedFromCache, testAgent);
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
        assertNull(agentReturnedFromCache);
    }

    private void wasteTime() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
