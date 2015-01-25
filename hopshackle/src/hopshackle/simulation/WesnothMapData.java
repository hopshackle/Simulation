package hopshackle.simulation;

import java.io.*;

public class WesnothMapData {

	private String[][] rawMapData;
	private int rows;
	private int columns;

	public WesnothMapData(File sourceFile) throws IOException {
		rawMapData = new String[200][]; // assume 200 row maximum
		FileReader fr = new FileReader(sourceFile);
		BufferedReader br = new BufferedReader(fr);
		String nextLine = br.readLine(); // first line is ignored
		nextLine = br.readLine(); // second line is ignored
		nextLine = br.readLine(); // third line is ignored

		int countOfRows = 0;
		while ((nextLine = br.readLine()) != null) {
			String[] mapRow = nextLine.split(",");
			rawMapData[countOfRows] = mapRow;
			countOfRows++;
		}

		rows = countOfRows;
		columns = rawMapData[0].length;
		br.close();
	}

	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return columns;
	}

	public TerrainType getTerrainType(int row, int column) {
		if (row <= rows && column <= columns) {
			String hexText = rawMapData[row][column];
			if (hexText.contains("F")) {
				return TerrainType.FOREST;
			}
			char firstChar = hexText.charAt(0);
			switch (firstChar) {
			case 'G':
				return TerrainType.PLAINS;
			case 'D':
				return TerrainType.DESERT;
			case 'W':
				return TerrainType.OCEAN;
			case 'H':
				return TerrainType.HILLS;
			case 'M':
				return TerrainType.MOUNTAINS;
			case 'S':
				return TerrainType.SWAMP;
			default:
				return TerrainType.PLAINS;
			}
		}
		return TerrainType.OCEAN; // beyond the borders
	}

}
