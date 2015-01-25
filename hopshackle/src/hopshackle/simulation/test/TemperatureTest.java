package hopshackle.simulation.test;

import static org.junit.Assert.assertEquals;
import hopshackle.simulation.Temperature;

import org.junit.*;
public class TemperatureTest {

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void vanillaTest() {
		Temperature t = new Temperature(0, 1.0);
		assertEquals(t.getTemperature(), 0.0, 0.00001);
		
		t.setTime(0.75);
		assertEquals(t.getTemperature(), 0.75, 0.00001);
		
		t.setTime(-0.5);
		assertEquals(t.getTemperature(), 0.0, 0.00001);
	}
	
	@Test
	public void reverseTest() {	
		Temperature t = new Temperature(-20, 30);
		assertEquals(t.getTemperature(), -20.0, 0.00001);
		
		t.setTime(0.8);
		assertEquals(t.getTemperature(), 20, 0.00001);
		
		t.setTime(-0.5);
		assertEquals(t.getTemperature(), -20.0, 0.00001);
	}
	

}
