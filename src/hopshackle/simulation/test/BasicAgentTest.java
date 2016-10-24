package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;

import org.junit.*;

public class BasicAgentTest {

	private BasicAgent testAgent;
	private World w;
	private FastCalendar testCalendar;
	
	@Before
	public void setUp() {
		SimProperties.setProperty("MaximumAgentAgeInSeconds", "100");
		w = new World();
		testCalendar = new FastCalendar(0);
		w.setCalendar(testCalendar);
		testAgent = new BasicAgent(w);
	}
	
	@Test
	public void agentDoesNotLoseHealthIfAlreadyDead() {
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
		testCalendar.setTime(100*1001);
		testAgent.maintenance();
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
		assertTrue(testAgent.isDead());
		testAgent.addHealth(-30);
		assertEquals(testAgent.getHealth(), 20.0, 0.001);
	}
	
	@Test
	public void genderCanBeChanged() {
		assertTrue(testAgent.isMale() && !testAgent.isFemale() || testAgent.isFemale() && !testAgent.isMale());
		testAgent.setMale(true);
		assertTrue(testAgent.isMale());
		assertFalse(testAgent.isFemale());
		testAgent.setMale(false);
		assertTrue(testAgent.isFemale());
		assertFalse(testAgent.isMale());
	}
	
	@Test
	public void ageCanBeChanged() {
		assertEquals(testAgent.getAge(), 0, 1);
		testAgent.setAge(20000);
		assertEquals(testAgent.getAge(), 20000, 1);
		testAgent.setAge(10000);
		assertEquals(testAgent.getAge(), 10000, 1);
	}
	
	@After
	public void cleanup() {
		SimProperties.setProperty("MaximumAgentAgeInSeconds", "180");
	}
	
}
