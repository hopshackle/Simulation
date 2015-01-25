package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.sql.*;
import java.util.List;
import java.util.logging.Logger;

public class BasicAgentRecorder implements AgentDataExtractor {

	private String dbStem;
	private Connection con;
	private Statement st;
	private int population, withHut, withChildren, inHut, totalFood, totalWood, totalHuts, totalChildren;
	private long totalAge, totalHealth;
	private World world;

	public static String newline = System.getProperty("line.separator");
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");

	public BasicAgentRecorder(String name) {
		dbStem = name;
	}

	@Override
	public void initialise() {
		con = ConnectionFactory.getConnection();
		try {
			st = con.createStatement();
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_Summary;");
			st.executeUpdate(
					"CREATE TABLE " + dbStem + "_Summary ( " +
					"Period 		INT 			NOT NULL," +
					"Number 		INT 			NOT NULL," +
					"OwnHut			INT				NOT NULL," +
					"HaveChildren	INT				NOT NULL," +
					"InHut			INT				NOT NULL," +
					"AvgHealth		DOUBLE			NOT NULL," +
					"AvgFood		DOUBLE			NOT NULL," +
					"AvgWood		DOUBLE			NOT NULL," +
					"AvgHuts		DOUBLE			NOT NULL," +
					"AvgAge			DOUBLE			NOT NULL," +
					"AvgChildren	DOUBLE			NOT NULL," +
					"Forest			DOUBLE			NOT NULL," +
					"Plains			DOUBLE			NOT NULL" +
			");");
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
			System.err.println(e.toString());
		}
	}

	@Override
	public void initialiseLoopVariables() {
		population = 0;
		withHut = 0;
		withChildren = 0;
		inHut = 0;
		totalHealth = 0;
		totalFood = 0;
		totalWood = 0;
		totalHuts = 0;
		totalAge = 0;
		totalChildren = 0;
	}

	@Override
	public void extractDataFrom(Agent a) {
		if (a instanceof BasicAgent && !a.isDead()) {
			world = a.getWorld();
			BasicAgent sampledAgent = (BasicAgent) a;
			population++;
			int huts = sampledAgent.getNumberInInventoryOf(BuildingType.HUT);
			totalHuts += huts;
			if (huts > 0) {
				withHut++;
				GoalMatcher hutFinder = new HutsOwnedByMatcher(sampledAgent);
				if (hutFinder.matches(sampledAgent.getLocation()))
					inHut++;
			}
			totalHealth += sampledAgent.getHealth();
			totalFood += sampledAgent.getNumberInInventoryOf(Resource.FOOD);
			totalWood += sampledAgent.getNumberInInventoryOf(Resource.WOOD);
			totalAge += sampledAgent.getAge();
			int children = sampledAgent.getNumberOfChildren();
			totalChildren += children;
			if (children > 0) {
				withChildren++;
			}
		}
	}
	@Override
	public void dataExtractFinishedForPeriod(int period) {
		double terrainCover[] = getTerrainCoverage();
		String sqlUpdate = "";
		if (population > 0) {
			double avgHealth = (double)totalHealth / (double)population;
			double avgFood = (double)totalFood / (double)population;
			double avgWood = (double)totalWood / (double)population;
			double avgHuts = (double)totalHuts / (double)population;
			double avgAge = (double)totalAge / (double)population;
			double avgChildren = (double)totalChildren / (double)population;
			sqlUpdate = String.format("INSERT INTO %s_Summary (Period, Number, OwnHut, HaveChildren, InHut, AvgHealth, " +
					"AvgFood, AvgWood, AvgHuts, AvgAge, AvgChildren, Forest, Plains) " +
					"VALUES (%d, %d, %d, %d, %d, %.1f, %.2f, %.2f, %.2f, %.0f, %.2f, %.3f, %.3f);",
					dbStem, period, population, withHut, withChildren, inHut, avgHealth, avgFood, avgWood, avgHuts, avgAge, avgChildren, 
					terrainCover[0], terrainCover[1]);
		} else {
			sqlUpdate = "INSERT INTO " + dbStem + "_Summary (Period, Number, OwnHut, HaveChildren, InHut, AvgHealth, " +
			"AvgFood, AvgWood, AvgHuts, AvgAge, AvgChildren, Forest, Plains) " +
			"VALUES ("+period+", 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);";
		}
		try {
			st = con.createStatement();
			st.executeUpdate(sqlUpdate);
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Error writing BasicAgent summary: " +e.toString());
		}
	}

	private double[] getTerrainCoverage() {
		double[] terrainCover = new double[2];
		terrainCover[0] = 0.0;
		terrainCover[1] = 0.0;

		if (world == null) return terrainCover;

		List<Location> hexes = world.getChildLocations();
		int totalHexes = 0;
		int forestHexes = 0;
		int plainsHexes = 0;

		for (Location loc : hexes) {
			if (loc instanceof Hex) {
				Hex hex = (Hex)loc;
				TerrainType terrain = hex.getTerrainType();
				totalHexes++;
				switch (terrain) {
				case FOREST:
					forestHexes++;
					break;
				case PLAINS:
					plainsHexes++;
					break;
				default:
					break;
				}
			}
		}
		terrainCover[0] = ((double)forestHexes) / ((double)totalHexes);
		terrainCover[1] = ((double)plainsHexes) / ((double)totalHexes);
		return terrainCover;
	}


	@Override
	public void closeDown() {
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
