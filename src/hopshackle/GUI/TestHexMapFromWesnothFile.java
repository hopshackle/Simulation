package hopshackle.GUI;

import hopshackle.simulation.*;

import java.io.File;

import javax.swing.*;

public class TestHexMapFromWesnothFile {

	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File f = new File(baseDir + "\\maps", args[0]);
		HexMap<Hex> map = new HexMap<Hex>(f, Hex.getHexFactory());
		
		World w = new World();
		w.setLocationMap(map);
		
		JFrame frame = new JFrame("Test of Wesnoth Map");

		WorldHexMap worldMap = new WorldHexMap(w);
		JScrollPane mapPane = new JScrollPane(worldMap);
		frame.getContentPane().add(mapPane);

		//Display the window.
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
