package hopshackle.simulation.test;

import static org.junit.Assert.*;
import hopshackle.simulation.*;

import java.io.File;
import java.util.List;

import org.junit.Test;
public class HopshackleUtilitiesTest {
	
	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");

	@Test
	public void loadListFromFile() {
		List<String> names = HopshackleUtilities.createListFromFile(new File(baseDir + "\\Genomes\\Standard\\DNDNames.txt"));
		assertTrue(names.get(0).equals("Conan"));
		assertEquals(names.size(), 126);
	}

	@Test
	public void loadEnumsFromFile() {
		SimProperties.setProperty("ActionEnumsToUse", baseDir + "\\Genomes\\Standard\\ActionEnumsDnD.txt");
		File actionEnumFile = new File(SimProperties.getProperty("ActionEnumsToUse", ""));
		List<String> classNames = HopshackleUtilities.createListFromFile(actionEnumFile);
		List<Object> actionEnums = HopshackleUtilities.loadEnums(classNames);
		assertFalse(actionEnums.isEmpty());
		assertTrue(actionEnums.get(0) instanceof ActionEnum);
	}
	
	@Test
	public void extractStringArguments() {
		String[] args = null;
		String output = HopshackleUtilities.getArgument(args, 0, "D");
		assertTrue(output.equals("D"));
		
		args = new String[2];
		args[1] = "E";
		
		output = HopshackleUtilities.getArgument(args, 1, "F");
		assertTrue(output.equals("E"));
		output = HopshackleUtilities.getArgument(args, 0, "F");
		assertTrue(output.equals("F"));
		output = HopshackleUtilities.getArgument(args, 2, "G");
		assertTrue(output.equals("G"));
		output = HopshackleUtilities.getArgument(args, 3, "H");
		assertTrue(output.equals("H"));
	}
	
	@Test
	public void extractIntegerArguments() {
		String[] args = null;
		int output = HopshackleUtilities.getArgument(args, 0, 23);
		assertEquals(output, 23);
		
		args = new String[2];
		args[1] = "12";
		
		output = HopshackleUtilities.getArgument(args, 1, 1001);
		assertEquals(output, 12);
		output = HopshackleUtilities.getArgument(args, 0, 78);
		assertEquals(output, 78);
		output = HopshackleUtilities.getArgument(args, 2, 2);
		assertEquals(output, 2);
		output = HopshackleUtilities.getArgument(args, 3, 3);
		assertEquals(output, 3);
	}
}
