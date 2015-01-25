package hopshackle.simulation;


import java.awt.Point;
import java.io.*;
import java.util.*;

/**
 * A HexMap follows the convention that each odd column is shifted up compared to the even columns (of you look at it graphically), 
 * or down (if you look at it numerically). [The difference is because for display the topmost row is row = 0.]
 * Hence (1,1) connects to (0,0) and (1,0) and (0,2) and (1,2) [i.e. to row and row-1]
 * While (1,2) connects to (1,1) and (2,1) and (1,3) and (2,3) [i.e. to row and row+1]
 * And   (1,3) connects to (0,2) and (1,2) and (0,4) and (1,4) [i.e. to row and row-1]
 * etc....
 * 
 * @author James
 *
 */
public class HexMap<T extends Hex> extends LocationMap {

	private int rows;
	private int columns;
	private HashMap<Point, T> mapToHexes;
	private HexFactory hexFactory;

	public HexMap(int rows, int columns, HexFactory factory) {
		this.rows = rows;
		this.columns = columns;
		hexFactory = factory;
		createHexes();
	}

	public HexMap(File sourceFile, HexFactory factory) {
		hexFactory = factory;
		WesnothMapData mapData = null;
		try {
			mapData = new WesnothMapData(sourceFile);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		this.rows = mapData.getRows();
		this.columns = mapData.getColumns();
		createHexes();
		for (Point hexCoordinates : mapToHexes.keySet()) {
			T hex = mapToHexes.get(hexCoordinates);
			hex.setTerrain(mapData.getTerrainType(hex.getRow(), hex.getColumn()));
		}
	}

	@SuppressWarnings("unchecked")
	private void createHexes() {
		mapToHexes = new HashMap<Point, T>(rows * columns);
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				T h = (T) hexFactory.getHex(i,j);
				mapToHexes.put(new Point(i,j), h);
			}
		}
		for (Hex h : mapToHexes.values())
			addNeighboursToHex(h);
	}

	private void addNeighboursToHex(Hex h) {
		int row = h.getRow();
		int column = h.getColumn();
		int offset = (((column)%2 == 1) ? -1 : +1);

		if (row < this.rows-1)
			h.addAccessibleLocation(mapToHexes.get(new Point(row+1, column)));
		if (row > 0)
			h.addAccessibleLocation(mapToHexes.get(new Point(row-1, column)));
		if (column < this.columns-1) {
			h.addAccessibleLocation(mapToHexes.get(new Point(row, column+1)));
			if (row+offset < rows && row+offset > 0)
				h.addAccessibleLocation(mapToHexes.get(new Point(row + offset, column+1)));
		} 
		if (column > 0) {
			h.addAccessibleLocation(mapToHexes.get(new Point(row, column-1)));
			if (row+offset < rows && row+offset > 0)
				h.addAccessibleLocation(mapToHexes.get(new Point(row + offset, column-1)));
		}
	}

	public int getRows() {
		return rows;
	}
	public int getColumns() {
		return columns;
	}

	public T getHexAt(int row, int column) {
		return mapToHexes.get(new Point(row, column));
	}

	@Override
	public List<T> getAllLocations() {
		return HopshackleUtilities.cloneList(mapToHexes.values());
	}

	@Override
	public void registerWorld(World world) {
		super.registerWorld(world);
		for (T h : mapToHexes.values())
			h.setParentLocation(world);
	}
}
