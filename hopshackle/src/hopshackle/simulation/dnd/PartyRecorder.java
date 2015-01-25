package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class PartyRecorder implements AgentDataExtractor {
	/* this records a history of parties
	 * It takes an argument of a World.
	 * It then takes a full agent List from that World at rgeular intervals.
	 * And then extracts all Agents that are Parties
	 * 
	 * We then record summary information for each party:
	 * - id
	 * - size
	 * - number of clerics
	 * - number of fighters
	 * - average level of party
	 * - current CR of location
	 * - age of party
	 * - std dev of party level
	 * - Wound of party
	 * - magic of party
	 * - id of leader
	 * 
	 * We also record overall summary information
	 * - total world population (clerics)
	 * - total world population (fighters)
	 * - clerics in parties
	 * - fighters in parties
	 * - number of parties
	 * 
	 */

	public static String newline = System.getProperty("line.separator");
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");

	private File outSummary;
	private int[] popByClass = new int[2];
	private int[] partyPopByClass = new int[2];
	private Connection con;
	private Statement st = null;
	private String dbStem;
	private ArrayList<PartyRecord> partyDetails;

	public PartyRecorder (File output) {
		String fileNameStem = output.getAbsolutePath();
		dbStem = output.getName();
		dbStem = dbStem.substring(0, dbStem.indexOf('.'));
		fileNameStem = fileNameStem.substring(0, fileNameStem.indexOf('.'));
		outSummary = new File(fileNameStem + "_summary.txt");
	}

	@Override
	public void initialise() {	
		con = ConnectionFactory.getConnection();
		try {
			st = con.createStatement();
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_Summary;");
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_Detail;");
			st.executeUpdate(
					"CREATE TABLE " + dbStem + "_Summary ( " +
					"Period 		INT 			NOT NULL," +
					"CLRTotal		INT 			NOT NULL," +
					"FTRTotal 		INT 			NOT NULL," +
					"CLRParty 		INT 			NOT NULL," +
					"FTRParty 		INT 			NOT NULL," +
					"Number			INT				NOT NULL" +
			");");

			st.executeUpdate(
					"CREATE TABLE " + dbStem + "_Detail (" +
					"Period 		INT 			NOT NULL," +
					"id				INT 			NOT NULL," +
					"size 			INT 			NOT NULL," +
					"CLR	 		INT 			NOT NULL," +
					"FTR	 		INT 			NOT NULL," +
					"AvgLevel		DOUBLE			NOT NULL," +
					"LocCR	 		INT 			NOT NULL," +
					"Age			INT				NOT NULL," +
					"StdDevLevel	DOUBLE			NOT NULL," +
					"Wound			DOUBLE			NOT NULL," +
					"Magic			DOUBLE			NOT NULL," +
					"leader			INT				NOT NULL" +
			");");


		} catch(Exception e) {
			logger.severe("Exception: " + e.getMessage());
			logger.severe(e.toString());
		}

		initialiseLoopVariables();
	}

	@Override
	public void initialiseLoopVariables() {
		partyDetails = new ArrayList<PartyRecord>();
		popByClass = new int[2];
		partyPopByClass = new int[2];
	}

	@Override
	public void extractDataFrom(Agent a) {

		if (a instanceof Character) {
			Character tempC = (Character)a;
			int index = -1;
			if (tempC.getChrClass()==CharacterClass.CLERIC)
				index = 0;
			if (tempC.getChrClass()==CharacterClass.FIGHTER)
				index = 1;
			if (index > -1) {
				popByClass[index]++;
				if (tempC.getParty()!=null)
					partyPopByClass[index]++;
			}
			return;
		}
		if (!(a instanceof Party))
			return;

		Party p = (Party)a;
		Character leader = p.getLeader();
		if (leader == null) {
			logger.warning("Null leader for Party: " + p.toString());
			return;
		}
		if (!(leader.getRace() == Race.HUMAN)) return;

		PartyRecord pr = new PartyRecord();

		pr.age = p.getAge();
		pr.avgLevel = p.getLevel();
		int clerics = 0;
		int fighters = 0;
		for (Character member : p.getMembers()) {
			if (member.getChrClass() == CharacterClass.CLERIC)
				clerics++;
			if (member.getChrClass() == CharacterClass.FIGHTER)
				fighters++;
		}
		pr.clerics = clerics;
		pr.fighters = fighters;
		pr.id = p.getUniqueID();
		pr.leader = leader.getUniqueID();
		Location l = p.getLocation();
		pr.locationCR = -1;
		if (l!=null && l instanceof Square) {
			Square s = (Square) l;
			pr.locationCR = s.getX() + s.getY();
		}
		pr.magic = p.getMagic();
		pr.wound = p.getWound();
		pr.size = p.getSize();
		pr.stddevLevel = p.getLevelStdDev();

		partyDetails.add(pr);

	}

	@Override
	public void dataExtractFinishedForPeriod(int period) {
		recordSample(period);
	}

	public void recordSample(int period) {
		/* we want one line per sample period
		 * Header line is Gene.toString
		 * Each line is then the count of that Gene
		 */
		try {
			FileWriter summaryWriter = new FileWriter(outSummary, true);
			if (period == 1){
				summaryWriter.write("Period, Parties, CLR, FTR, CLRParty, FTRParty" + newline);
			}
			StringBuffer line = new StringBuffer();
			line.append(period + ", ");
			line.append(popByClass[0] + ", ");
			line.append(popByClass[1] + ", ");
			line.append(partyPopByClass[0] + ", ");
			line.append(partyPopByClass[1] + ", ");
			line.append(partyDetails.size());

			String sqlUpdate = String.format("INSERT INTO %s SET " +
					" Period = %d, CLRTotal = %d, FTRTotal = %d, CLRParty = %d, " +
					" FTRParty = %d, Number = %d;" ,
					dbStem + "_Summary",
					period,
					popByClass[0],
					popByClass[1],
					partyPopByClass[0],
					partyPopByClass[1],
					partyDetails.size()
			);

			summaryWriter.write(line.toString() + newline);
			summaryWriter.close();
			st.executeUpdate(sqlUpdate);

			for (PartyRecord pr : partyDetails) {
				sqlUpdate = String.format("INSERT INTO %s SET " +
						"Period = %d, id = %d, size = %d, CLR = %d, FTR = %d, Avglevel = %.2f, " +
						"LocCR = %d, Age = %d, StdDevlevel = %.3f, Wound = %.3f, Magic = %.3f, leader = %d",
						dbStem + "_Detail",
						period,
						pr.id,
						pr.size,
						pr.clerics,
						pr.fighters,
						pr.avgLevel,
						pr.locationCR,
						pr.age,
						pr.stddevLevel,
						pr.wound,
						pr.magic,
						pr.leader);

				st.executeUpdate(sqlUpdate);
			}

		} catch (IOException e) {
			e.printStackTrace();
			logger.severe("Error writing PartyRecord: " +e.toString());
		} catch (SQLException e) {
			e.printStackTrace();
			logger.severe("SQL Error updating database: "+e.toString());
		} 
	}
	
	@Override
	public void closeDown() {
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculates the standard deviation of an array
	 * of numbers.
	 * see http://davidmlane.com/hyperstat/A16252.html
	 *
	 * @param data Numbers to compute the standard deviation of.
	 * Array must contain two or more numbers.
	 * @return standard deviation estimate of population
	 * ( to get estimate of sample, use n instead of n-1 in last line )
	 */

	public static double mean (ArrayList<Double> data) {

		if (data.size() < 1)
			return 0.0;

		double mean = 0;
		final int n = data.size();
		for ( int i=0; i<n; i++ )
			mean += data.get(i);

		mean /= n;
		return mean;
	}
	public static double stddev ( ArrayList<Double> data ) {
		// sd is sqrt of sum of (values-mean) squared divided by n - 1
		final int n = data.size();
		if ( n < 2 )
			return 0.0;

		double mean = mean(data);

		// calculate the sum of squares
		double sum = 0;
		for ( int i=0; i<n; i++ )
		{
			final double v = data.get(i) - mean;
			sum += v * v;
		}
		return Math.sqrt( sum / ( n - 1 ) );
	}

	class PartyRecord {
		long	id;
		int		size;
		int		clerics;
		int		fighters;
		double	avgLevel;
		int		locationCR;
		int		age;
		double	stddevLevel;
		double	wound;
		double	magic;
		long	leader;
	}
}


