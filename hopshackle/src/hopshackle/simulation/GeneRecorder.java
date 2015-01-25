package hopshackle.simulation;

import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class GeneRecorder implements AgentDataExtractor {
	/* this records a history of gene frequencies
	 * It takes an argument of a World.
	 * It then takes a full agent List from that World at regular intervals.
	 * And then extracts all Agents with Genomes from that World.
	 * And sorts their Genes into similar ones to keep a count.
	 * 
	 * Each new type is given an identifier.
	 * 
	 * We end up with one Array of Gene types.
	 * After each count though, the results are appended to a file.
	 * Each column of this file is one gene type. Each row represents a sample period.
	 * 
	 * So we have a Hashtable to linke identifiers to Genes
	 * And then a Hashtable for each sample set.
	 * With these stored in an Arraylist of Hashtables
	 * 
	 */

	public static String newline = System.getProperty("line.separator");
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private List<Gene> geneArray;
	private List<Integer> geneCountArray;
	private List<ArrayList<Double>> geneMod2Array;
	private List<Integer> sortedByFrequency;
	private List<Integer> worldPopulation;

	private List<Hashtable<ActionEnum,Integer>> codonCountArray;
	private List<Hashtable<ActionEnum, ArrayList<Double>>> codonMod2Array;
	private static volatile Integer codonCount = 0;
	private static Hashtable<String, Integer> codonHash = new Hashtable<String, Integer>();
	// These are to hold all codons, plus the count of 
	// Genes - identified by ActionEnum - that use them
	// codonModifierArray keeps track of the average modifier for the codons

	//codonHash is to hold the master record of what Codon has what id (and hence index)

	private File outGeneFull, outGeneList, outSummary, outCodonFull;
	private int lastGene;
	private int[] popByLevel;
	private long sumXP;
	private int worldPop;
	private CharacterClass classFilter;
	private Connection con;
	private Statement st = null;
	private String dbStem;
	private boolean recordGenes = false;
	private File output;

	public GeneRecorder (File output, CharacterClass classFilter) {
		this.output = output;
		this.classFilter = classFilter;
	}

	public void initialise() {
		String fileNameStem = output.getAbsolutePath();
		dbStem = output.getName();
		dbStem = dbStem.substring(0, dbStem.indexOf('.'));
		fileNameStem = fileNameStem.substring(0, fileNameStem.indexOf('.'));
		outCodonFull = new File(fileNameStem + "_codonFull.txt");
		outSummary = new File(fileNameStem + "_summary.txt");
		outGeneFull = new File(fileNameStem + "_geneFull.txt");
		outGeneList = new File(fileNameStem + "_geneList.txt");

		geneArray = new ArrayList<Gene>();
		geneCountArray = new ArrayList<Integer>();
		geneMod2Array = new ArrayList<ArrayList<Double>>();
		worldPopulation = new ArrayList<Integer>();
		lastGene = 0;
		codonCountArray = new ArrayList<Hashtable<ActionEnum,Integer>>();
		codonMod2Array = new ArrayList<Hashtable<ActionEnum,ArrayList<Double>>>();
		codonCountArray.add(new Hashtable<ActionEnum,Integer>());
		codonMod2Array.add(new Hashtable<ActionEnum,ArrayList<Double>>());
		// Add in the first 0 element - for no codons at all
		sortedByFrequency = new ArrayList<Integer>();

		// and set up link to database

		con = ConnectionFactory.getConnection();
		try {
			st = con.createStatement();
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_CodonExp;");
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_CodonList;");
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_GeneExp;");
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_GeneList;");
			st.executeUpdate("DROP TABLE IF EXISTS " +dbStem +"_Pop;");
			st.executeUpdate(
					"CREATE TABLE " + dbStem + "_CodonExp ( " +
					"Period 		INT 			NOT NULL," +
					"Codon 			INT 			NOT NULL," +
					"Action 		VARCHAR(25) 	NOT NULL," +
					"Number 		INT 			NOT NULL," +
					"Percentage		DOUBLE			NOT NULL," +
					"AvgModifier	DOUBLE			NOT NULL," +
					"StdDevMod		DOUBLE			NOT NULL" +
			");");

			if (recordGenes)
				st.executeUpdate(
						"CREATE TABLE " + dbStem + "_GeneExp (" +
						"Period 		INT 			NOT NULL," +
						"Gene			INT 			NOT NULL," +
						"Action 		VARCHAR(25) 	NOT NULL," +
						"Number 		INT 			NOT NULL," +
						"Percentage		DOUBLE			NOT NULL," +
						"AvgBase		DOUBLE			NOT NULL," +
						"StdDevBase		DOUBLE			NOT NULL" +
				");");

			if (recordGenes)
				st.executeUpdate(
						"CREATE TABLE " + dbStem + "_GeneList (" +
						"id				INT				PRIMARY KEY," +
						"Description 	VARCHAR(500)	NOT NULL" +
				");");

			st.executeUpdate(
					"CREATE TABLE " + dbStem + "_Pop (" +
					"Period 	INT		PRIMARY KEY," +
					"Population	INT 	NOT NULL," +
					"Level1		INT 	NOT NULL," +
					"Level2		INT 	NOT NULL," +
					"Level3		INT 	NOT NULL," +
					"Level4		INT 	NOT NULL," +
					"Level5		INT 	NOT NULL," +
					"Level6		INT 	NOT NULL," +
					"Level7		INT 	NOT NULL," +
					"Level8		INT 	NOT NULL," +
					"Level9		INT 	NOT NULL," +
					"Level10	INT 	NOT NULL," +
					"Level11	INT 	NOT NULL," +
					"Level12	INT 	NOT NULL," +
					"Level13	INT 	NOT NULL," +
					"Level14	INT 	NOT NULL," +
					"Level15	INT 	NOT NULL," +
					"Level16	INT 	NOT NULL," +
					"Level17	INT 	NOT NULL," +
					"Level18	INT 	NOT NULL," +
					"Level19	INT 	NOT NULL," +
					"Level20	INT 	NOT NULL," +
					"AvgXP		INT		NOT NULL" +
			");");

			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS CodonList (" +
					"id				INT				PRIMARY KEY," +
					"Description 	VARCHAR(200)	NOT NULL" +
			");");

			// This is the static bit of set up for the first GeneRecorder
			synchronized (codonCount) {
				if (codonCount == 0) {
					ResultSet rs;
					rs = st.executeQuery("SELECT * FROM CodonList");
					String codonDesc;
					int codonIndex;
					for (rs.first(); !rs.isAfterLast(); rs.next()) {
						codonDesc = rs.getString(2);
						codonIndex = rs.getInt(1);
						codonHash.put(codonDesc, codonIndex);
						codonCount = Math.max(codonCount, codonIndex);
					}
				}
			}

		} catch(Exception e) {
			System.err.println("Exception: " + e.getMessage());
			System.err.println(e.toString());
		}

		if (recordGenes) {
			geneCountArray = new ArrayList<Integer>(geneArray.size()+50);
			geneMod2Array = new ArrayList<ArrayList<Double>>(geneArray.size()+50);
			for (int n=0; n<geneArray.size(); n++) {
				geneCountArray.add(0);
				geneMod2Array.add(new ArrayList<Double>());
			}
		}
	}

	@Override
	public void initialiseLoopVariables() {
		sumXP = 0;
		worldPop = 0;
		popByLevel = new int[21];

		// Then clear out the codon list for next time
		codonCountArray.clear();
		codonMod2Array.clear();

		codonCountArray.add(new Hashtable<ActionEnum, Integer>());
		codonMod2Array.add(new Hashtable<ActionEnum, ArrayList<Double>>());
		// add in the zeroth element only for Null
		// others will be added as needed

		if (recordGenes) {
			Collections.sort(sortedByFrequency, new Comparator<Integer>(){
				public int compare(Integer arg0, Integer arg1) {
					return geneCountArray.get(arg1) - geneCountArray.get(arg0);
				}
			});
		}

		logger.info("Finished Gene Recorder: " + outSummary.toString());
	}

	@Override
	public void extractDataFrom(Agent a) {

		if (!(a instanceof Character)) {
			return;
		}
		Character chr = (Character)a;
		if (!(chr.getRace() == Race.HUMAN)) return;
		if (classFilter != null) {
			if (chr.getChrClass() != classFilter) return;
		}
		worldPop++;
		sumXP += chr.getXp();
		popByLevel[(int)chr.getLevel()]++;

		List<Gene> allGenes = a.getGenome().getAllGenes();
		for (Gene g : allGenes) {
			if (g==null) continue;
			if (recordGenes) {
				Gene comparator;
				boolean matchFound = false;
				int maxLoop = geneArray.size();
				for (int n = 0; n<maxLoop; n++) {
					int actualIndex = sortedByFrequency.get(n);
					comparator = geneArray.get(actualIndex);
					if (comparator.isSimilar(g)) {
						// we have a match for this
						int currentCount = geneCountArray.get(actualIndex);
						ArrayList<Double> currentTotalMod = geneMod2Array.get(actualIndex);
						geneCountArray.set(actualIndex, currentCount+1);
						currentTotalMod.add(g.getBase());
						matchFound = true;
						break;
					}
				}
				if (!matchFound) {
					geneArray.add(g);
					geneCountArray.add(1);
					ArrayList<Double> newGeneModList = new ArrayList<Double>();
					newGeneModList.add(g.getBase());
					geneMod2Array.add(newGeneModList);
					sortedByFrequency.add(geneArray.size()-1);
				}
			}

			//Now we do the same for the individual codons

			ActionEnum geneAction = g.getAction();
			List<Codon> codonList = g.getCodons();
			// the zero position in the Array is reserved for 
			// the lack of any codons
			if (codonList.isEmpty()) {
				Hashtable<ActionEnum,Integer> codonTable;
				Hashtable<ActionEnum,ArrayList<Double>> codonModList;
				codonTable = codonCountArray.get(0);
				codonModList = codonMod2Array.get(0);
				ArrayList<Double> currentModList = null;
				if (codonTable.containsKey(geneAction)) {
					int currentCount = codonTable.get(geneAction);
					currentModList = codonModList.get(geneAction);
					codonTable.put(geneAction, (currentCount+1));
				} else {
					codonTable.put(geneAction, 1);
				}
				if (currentModList == null) {
					currentModList = new ArrayList<Double>();
				}
				currentModList.add(g.getBase());
				codonModList.put(geneAction, currentModList);
			}
			ArrayList<Codon> previousCodons = new ArrayList<Codon>();
			// this checks to make sure we don't double count codons

			boolean seenBefore;
			for (Codon c : codonList) {
				if (c == null) continue;
				seenBefore = false;
				for (Codon c1 : previousCodons) {
					if (c1.isSimilar(c)) {
						// seen before
						seenBefore = true;
					}
				}
				if (seenBefore) continue;

				Integer codonIndex;

				codonIndex = codonHash.get(c.toPrettyString());
				if (codonIndex != null) {
					// we have a match
					// So we pick up the table of ActioEnums to codonCount
					// If this doesn't exist, then add a new one
					// then increment the count for this ActionEnum

					if (codonIndex+1 > codonCountArray.size()) {
						for (int loop = codonCountArray.size(); loop <= codonIndex+1; loop++) {
							codonCountArray.add(null);
							codonMod2Array.add(null);
						}
					}

					Hashtable<ActionEnum,Integer> codonTable;
					Hashtable<ActionEnum, ArrayList<Double>> codonModifierTable;
					codonTable = codonCountArray.get(codonIndex);
					codonModifierTable = codonMod2Array.get(codonIndex);
					if (codonTable == null){
						codonTable = new Hashtable<ActionEnum,Integer>();
						//  TODO: this is the problem line. Its moving later elements on
						// Change this to use a hashtable rather than an ArrayList
						codonCountArray.remove((int) codonIndex);
						codonCountArray.add(codonIndex,codonTable);
						codonModifierTable = new Hashtable<ActionEnum,ArrayList<Double>>();
						codonMod2Array.remove((int) codonIndex);
						codonMod2Array.add(codonIndex,codonModifierTable);
					}
					if (codonTable.containsKey(geneAction)) {
						int currentCount = codonTable.get(geneAction);
						codonTable.put(geneAction, (currentCount+1));
					} else {
						codonTable.put(geneAction, 1);
					}
					// Having incremented the count, we add in the value obtained to the list of all values
					ArrayList<Double> currentModList = codonModifierTable.get(geneAction);
					if (currentModList == null) {
						currentModList = new ArrayList<Double>();
						codonModifierTable.put(geneAction, currentModList);
					}
					currentModList.add(c.getModifier());

				} else synchronized (codonCount) {
					// not seen this codon before, so add it to end of Array
					codonCount++;
					codonHash.put(c.toPrettyString(), codonCount);

					// then insert new codon into table as well
					String sqlString = "INSERT INTO CodonList SET id = " + codonCount + 
					", Description = '" + c.toPrettyString() +"'";
					try {
						st.executeUpdate(sqlString);
					} catch (SQLException e) {
						e.printStackTrace();
						logger.severe(e.toString());
					}

					if (codonCount+1 > codonCountArray.size()) {
						for (int loop = codonCountArray.size(); loop <= codonCount+1; loop++) {
							codonCountArray.add(null);
							codonMod2Array.add(null);
						}
					}
					Hashtable<ActionEnum,Integer> codonTable = new Hashtable<ActionEnum,Integer>();
					codonTable.put(geneAction, 1);
					codonCountArray.add(codonCount, codonTable);
					Hashtable<ActionEnum,ArrayList<Double>> codonModifierTable = new Hashtable<ActionEnum,ArrayList<Double>>();
					ArrayList<Double> tempArray = new ArrayList<Double>();
					tempArray.add(c.getModifier());
					codonModifierTable.put(geneAction,tempArray);
					codonMod2Array.add(codonCount, codonModifierTable);
				}
				previousCodons.add(c);
			}
		}
	}

	@Override
	public void dataExtractFinishedForPeriod(int period) {
		worldPopulation.add(worldPop);
		recordSample(period);
	}

	public void recordSample(int period) {
		/* we want one line per sample period
		 * Header line is Gene.toString
		 * Each line is then the count of that Gene
		 */
		StringBuffer sqlUpdate = null;
		try {
			FileWriter summaryWriter = new FileWriter(outSummary, true);
			if (period == 1){
				summaryWriter.write("Pop, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, AvgXP" + newline);
			}
			StringBuffer line = new StringBuffer();
			sqlUpdate = new StringBuffer();
			line.append(worldPopulation.get(period-1));
			sqlUpdate.append("INSERT INTO " + dbStem + "_Pop SET Period = " + period);
			sqlUpdate.append(", Population = " + worldPopulation.get(period -1));
			for (int m = 1; m <=20; m++) {
				line.append("," + popByLevel[m]);
				sqlUpdate.append(", Level"+m+" = " + popByLevel[m]);
			}
			double denominator = worldPopulation.get(period-1);
			if (denominator == 0) denominator = 1;
			line.append("," + (int)(sumXP / denominator));
			sqlUpdate.append(", AvgXP = " + sumXP/denominator);
			summaryWriter.write(line.toString() + newline);
			summaryWriter.close();
			st.executeUpdate(sqlUpdate.toString());

			sumXP = 0;

			FileWriter geneticWriter = new FileWriter(outGeneList,true);
			for (int n = lastGene; n<geneArray.size(); n++) {
				geneticWriter.write(n + "," + geneArray.get(n).toString()+newline);
				st.executeUpdate("INSERT INTO " + dbStem + "_GeneList " +
						"SET id = " + n +
						", Description = '" +geneArray.get(n).toString() +"'");
			}
			geneticWriter.close();
			lastGene = geneArray.size();

			if (recordGenes) {
				FileWriter fullOutput = new FileWriter(outGeneFull, true);
				if (period==1){
					fullOutput.write("Period, GeneID, Action, Number, Percentage, AvgBase, StdDevBase"+newline);
				}
				for (int n=0; n< geneArray.size(); n++) {
					if (geneCountArray.get(n) > 0) {
						line = new StringBuffer();
						sqlUpdate = new StringBuffer("INSERT INTO " + dbStem + "_GeneExp ");
						line.append(period);
						sqlUpdate.append("SET Period = " + period);
						line.append("," + n);
						sqlUpdate.append(", Gene = " + n);
						line.append("," +geneArray.get(n).getAction().toString());
						sqlUpdate.append(", Action = '" + geneArray.get(n).getAction().toString() +"'");
						line.append("," +geneCountArray.get(n));
						sqlUpdate.append(", Number = " + geneCountArray.get(n));
						double percent = (double) geneCountArray.get(n)/ (double) worldPopulation.get(period-1);
						line.append("," + percent);
						sqlUpdate.append(", Percentage = " + percent);
						double mean = mean(geneMod2Array.get(n));
						double stddev = stddev(geneMod2Array.get(n));
						line.append("," + mean);
						sqlUpdate.append(", AvgBase = " + mean);
						line.append("," + stddev);
						sqlUpdate.append(", StdDevBase = " + stddev);
						fullOutput.write(line.toString() + newline);
						st.executeUpdate(sqlUpdate.toString());
					}
				}
				fullOutput.close();
			}

			FileWriter codonFullFileWriter = new FileWriter(outCodonFull,true);
			if (period ==1) {
				codonFullFileWriter.write("Period, CodonID, Action, Number, Percentage, AvgModifier, StdDevModifier"+newline);
			}

			synchronized (codonCount) {

				for (int loop = codonCountArray.size(); loop <= codonCount+1; loop++) {
					codonCountArray.add(null);
					codonMod2Array.add(null);
				}
				Enumeration<String> keyList = codonHash.keys();
				String codonString = keyList.nextElement();
				for (; keyList.hasMoreElements(); codonString = keyList.nextElement()) {
					int n = codonHash.get(codonString);
					Hashtable<ActionEnum,Integer> codonTable = codonCountArray.get(n);
					Hashtable<ActionEnum,ArrayList<Double>> codonModifierTable = codonMod2Array.get(n);
					if (codonTable != null) {
						for (ActionEnum ae : codonTable.keySet()) {
							line = new StringBuffer();
							sqlUpdate = new StringBuffer("INSERT INTO " + dbStem + "_CodonExp ");
							line.append(period);
							sqlUpdate.append("SET Period = " + period);
							line.append(","+n);
							sqlUpdate.append(", Codon = " + n);
							line.append(","+ae.toString());
							sqlUpdate.append(", Action = '" + ae.toString() +"'");
							line.append(","+codonTable.get(ae));
							sqlUpdate.append(", Number = " + codonTable.get(ae));
							double percent = (double)codonTable.get(ae) / (double) worldPopulation.get(period-1);
							double mean = mean(codonModifierTable.get(ae));
							double stddev = stddev(codonModifierTable.get(ae));
							line.append("," + percent);
							line.append("," + mean);
							line.append("," + stddev);
							sqlUpdate.append(", Percentage = " + percent + ", AvgModifier = " + mean + ", StdDevMod = " + stddev);
							codonFullFileWriter.write(line.toString()+newline);
							st.executeUpdate(sqlUpdate.toString());
						}
					}
				}
			}
			codonFullFileWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
			logger.severe("Error writing GeneRecord: " +e.toString());
		} catch (SQLException e) {
			e.printStackTrace();
			logger.severe("SQL Error updating database: "+e.toString());
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

		if (data.size() < 1) {
			return 0.0;
		}
		double mean = 0;
		final int n = data.size();
		for ( int i=0; i<n; i++ )
		{
			mean += data.get(i);
		}
		mean /= n;
		return mean;
	}
	public static double stddev ( ArrayList<Double> data ) {
		// sd is sqrt of sum of (values-mean) squared divided by n - 1
		final int n = data.size();
		if ( n < 2 )
		{
			return 0.0;
		}

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

	@Override
	public void closeDown() {
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
