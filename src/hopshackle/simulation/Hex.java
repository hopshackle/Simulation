package hopshackle.simulation;

import java.awt.*;

public class Hex extends Location {

	private int row;
	private int column;
	protected TerrainType terrain;
	protected final double cellSize = 36.0;	// Pixels per Square
	private final double rowSeparation = cellSize*0.5 + Math.pow(Math.pow(cellSize/2.0, 2)/2.0, 0.5);
	private final double columnSeparation = cellSize * 0.75;
	private static HexFactory hexFactory = new HexFactory();
	private final double[] xCoords = new double[]{-0.25*cellSize, 0.25*cellSize, 0.5*cellSize, 0.25*cellSize, -0.25*cellSize, -0.5*cellSize};
	private final double root3over2 = Math.pow(3, 0.5) / 2.0;
	private final double[] yCoords = new double[]{root3over2*cellSize*0.5, root3over2*cellSize*0.5, 0l,
			-root3over2*cellSize*0.5, -root3over2*cellSize*0.5, 0l};
	private static Font agentFont = new Font("Helvetica", Font.PLAIN, 12);
	
	protected Hex(int row, int column) {
		super();
		this.row = row;
		this.column = column;
		terrain = TerrainType.PLAINS;
	}

	public int getRow() {
		return row;
	}
	public int getColumn() {
		return column;
	}
	public void setTerrain(TerrainType terrain) {
		this.terrain = terrain;
	}
	public TerrainType getTerrainType() {
		return terrain;
	}
	public Hex getHex(int row, int column) {
		return new Hex(row, column);
	}

	public void fill(Graphics g) {
		g.setColor(getTerrainType().getDisplayColour());
		Polygon hex = createPolygonFromHex();
		g.fillPolygon(hex);
	}

	public void draw(Graphics g) {
		drawOutline(g);
		drawContent(g);
	}
	
	protected void drawOutline(Graphics g) {
		g.setColor(Color.black);
		Polygon hex = createPolygonFromHex();
		g.drawPolygon(hex);
	}
	protected void drawContent(Graphics g) {
		g.setFont(agentFont);
		String text = Integer.toString(getAgents().size());
		char[] cA = text.toCharArray();
		g.drawChars(cA, 0, cA.length, (int)(getCentralX()-cellSize/4.0), (int)getCentralY());
	}
	
	protected Polygon createPolygonFromHex() {
		int[] xNodes = new int[6];
		int[] yNodes = new int[6];
		double xCentreOfHex = getCentralX();
		double yCentreOfHex = getCentralY();

		for (int loop = 0; loop<6; loop++) {
			xNodes[loop] = (int) Math.round(xCentreOfHex + xCoords[loop]);
			yNodes[loop] = (int) Math.round(yCentreOfHex + yCoords[loop]);
		}
		return new Polygon(xNodes, yNodes, 6);
	}
	protected double getCentralX() {
		return getColumn()*columnSeparation + cellSize/2.0;
	}
	protected double getCentralY() {
		double yCentreOfHex = getRow()*rowSeparation + cellSize/2.0;
		if (getColumn()%2 == 0) yCentreOfHex += rowSeparation/2.0;
		return yCentreOfHex;
	}

	@Override
	public String toString() {
		return "R:"+getRow() + " C:" + getColumn();
	}

	public static HexFactory getHexFactory() {
		return hexFactory;
	}
}

