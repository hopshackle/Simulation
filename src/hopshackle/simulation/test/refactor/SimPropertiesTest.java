package hopshackle.simulation.test.refactor;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.util.*;

import org.junit.*;

public class SimPropertiesTest {
	
	Properties testProperties;

	@Before
	public void setUp() throws Exception {
		testProperties = new Properties();
		testProperties.setProperty("base", "100.0");
		testProperties.setProperty("dec1.base", "200.0");
		testProperties.setProperty("dec2.other", "300");
		testProperties.setProperty("other", "400");
		testProperties.setProperty("dec2.noch", "500.0");
		testProperties.setProperty("dec1.noch", "600");
	}
	
	@After
	public void tearDown() {
		SimProperties.clear();
	}

	@Test
	public void extractDeciderNames() {
		Set<String> deciderNames = SimProperties.extractDeciderNames(testProperties);
		assertEquals(deciderNames.size(), 2);
		assertTrue(deciderNames.contains("dec1"));
		assertTrue(deciderNames.contains("dec2"));
	}
	
	@Test
	public void createsMapOfDeciderProperties() {
		SimProperties.initialiseProperties(testProperties);
		DeciderProperties d1 = SimProperties.getDeciderProperties("dec1");
		DeciderProperties d2 = SimProperties.getDeciderProperties("dec2");
		assertTrue(SimProperties.getDeciderProperties("dec3") == null);
		assertTrue(d1 != d2);
		assertTrue(d1 != null);
		assertTrue(d2 != null);
		assertTrue(d1.getProperty("base", "0").equals("200.0"));
		assertTrue(d2.getProperty("base", "0").equals("100.0"));
		assertTrue(SimProperties.getProperty("base", "0").equals("100.0"));
		assertTrue(d1.getProperty("other", "0").equals("400"));
		assertTrue(d2.getProperty("other", "0").equals("300"));
		assertTrue(SimProperties.getProperty("other", "0").equals("400"));
		assertTrue(d1.getProperty("noch", "0").equals("600"));
		assertTrue(d2.getProperty("noch", "0").equals("500.0"));
		assertTrue(SimProperties.getProperty("noch", "0").equals("0"));
	}

}
