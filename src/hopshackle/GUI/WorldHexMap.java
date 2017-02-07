package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicHex;

import java.awt.*;
import java.util.List;

import javax.swing.JFrame;

public class WorldHexMap extends SimpleAnimationPanel {

	private static final long serialVersionUID = 1L;
	private HexMap<BasicHex> worldMap;
	private final double cellSize = 36.0;	// Pixels per Square
	private final double rowSeparation = cellSize*0.5 + Math.pow(Math.pow(cellSize/2.0, 2)/2.0, 0.5);
	private final double columnSeparation = cellSize * 0.75;
	private World world;


	@SuppressWarnings("unchecked")
	public WorldHexMap(World w) {
		super();
		world = w;
		LocationMap tempMap = world.getLocationMap();
		if (tempMap instanceof HexMap) {
			worldMap = (HexMap<BasicHex>)tempMap;
		} else {
			throw new AssertionError("Must have a World with a HexMap");
		}
		
		this.setDoubleBuffered(true);

		this.setMillisecondsPerFrame(250);
		this.start();
	}

	@Override
	public void drawFrame(Graphics g) {

		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.drawRect(0,0, getWidth(), getHeight());

		List<BasicHex> allLocations = worldMap.getAllLocations();
		for (BasicHex h : allLocations) {
			h.fill(g);
		}
		for (BasicHex h : allLocations) {
			h.draw(g);
		}
		
		g.setColor(Color.white);
		g.fillRect(getWidth() - 150, 0, getWidth(), 25);
		g.setColor(Color.black);
		g.drawString(String.format("Time: %.1f, Pop: %d", ((float)world.getCurrentTime())/1000f/60f, world.getAgents().size()), getWidth()-145, 20);
	}

	@Override
	public Dimension getPreferredSize() {
		double maxX = columnSeparation * worldMap.getColumns() + cellSize;
		double maxY = rowSeparation * worldMap.getRows() + cellSize;
		return new Dimension((int)maxX, (int)maxY);
	}
	
	public static void main(String[] args) {
		HexMap<BasicHex> testMap = new HexMap<BasicHex>(5, 5, BasicHex.getHexFactory());
		World w = new World(new SimpleWorldLogic<Agent>(null));
		w.setLocationMap(testMap);
		JFrame frame = new JFrame("Test of Hex Map");

		WorldHexMap worldMap = new WorldHexMap(w);
		worldMap.setPreferredSize(new Dimension(500,250));

		frame.getContentPane().add(worldMap);

		//Display the window.
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
