package hopshackle.simulation;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class Genome {

	/* A Genome is a collection of Chromosomes
	 * Each Chromosome applies to a given ActionSet (an EnumSet of an Enum that
	 * implements the ActionEnum interface). It also has a Genetic Variable Set (an EnumSet
	 * of an Enum that implements the GeneticVariable interface).
	 * 
	 * Therefore several of the calls to a Genome will specify an Action and an EnumSet. 
	 * 
	 * Things we need to go with a Genome:
	 * 
	 * 		- get Value for an Action in the context of a given Agent
	 * 				This will add the Gene if it doesn't exist
	 * 		- mutate()
	 * 		- create from File
	 * 		- write to File
	 * 		- provide the Prioritised Actions for a given ActionSet that is passed
	 * 	
	 */

	private Hashtable<String, Chromosome> chromosomeTable;
	private String name;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");
	private static Hashtable<String, ArrayList<ActionEnum>> allActionSets = new Hashtable<String, ArrayList<ActionEnum>>();
	private static Hashtable<String, ArrayList<GeneticVariable>> allVariableSets = new Hashtable<String, ArrayList<GeneticVariable>>();

	static { 
		String fileName = SimProperties.getProperty("ActionEnumClassFile", "");
		List<String> actionEnumClasses = HopshackleUtilities.createListFromFile(new File(fileName));
		for (String chromosomeName : actionEnumClasses) {
			List<String> chrName = new ArrayList<String>();
			chrName.add(chromosomeName);
			List<Object> actionEnumList = HopshackleUtilities.loadEnums(chrName);
			ArrayList<ActionEnum> actionsInSet = new ArrayList<ActionEnum>();
			for (Object ae : actionEnumList)
				actionsInSet.add((ActionEnum) ae);

			if (!actionsInSet.isEmpty())
				allActionSets.put(actionsInSet.get(0).getChromosomeDesc(), actionsInSet);
		}

		fileName = SimProperties.getProperty("GeneticVariableClassFile", "");
		List<String> geneticVariableClasses = HopshackleUtilities.createListFromFile(new File(fileName));
		for (String chromosomeName : geneticVariableClasses) {
			List<String> chrName = new ArrayList<String>();
			chrName.add(chromosomeName);
			List<Object> geneticVariableList = HopshackleUtilities.loadEnums(chrName);
			ArrayList<GeneticVariable> variablesInSet = new ArrayList<GeneticVariable>();
			for (Object gv : geneticVariableList)  {
				variablesInSet.add((GeneticVariable)gv);
			}
			if (!variablesInSet.isEmpty())
				allVariableSets.put(variablesInSet.get(0).getDescriptor(), variablesInSet);
		}

	}

	public Genome() {
		chromosomeTable = new Hashtable<String, Chromosome>();
	}
	public static Genome GenomeFactory(Genome base){
		Genome retValue = base.clone();
		retValue.mutate();
		return retValue;
	}

	public Genome(File f) {
		this();

		/* the File will consist of one line per Gene, of the format
		 * chromosomeName:variableSetName:Gene
		 */
		try
		{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String currentChromosome = null;
			String currentVariableSet = null;
			ArrayList<String> chromosomeRecord = new ArrayList<String>();
			String nextGene = br.readLine();
			setName(f.getName());
			while (nextGene!=null)
			{
				String temp[] = nextGene.split(":");
				String chromosomeHeader = temp[0];
				if (currentChromosome == null) {
					currentChromosome = chromosomeHeader;
					currentVariableSet = temp[1];
				}

				if (currentChromosome.equals(chromosomeHeader)) {
					// still on the old chromosome
					chromosomeRecord.add(temp[2]);
				} else {
					// New chromosome has been reached
					Chromosome ch = new Chromosome(chromosomeRecord, 
							allActionSets.get(currentChromosome),
							allVariableSets.get(currentVariableSet));
					chromosomeTable.put(currentChromosome, ch);

					chromosomeRecord = new ArrayList<String>();
					chromosomeRecord.add(temp[2]);
					currentChromosome = chromosomeHeader;
					currentVariableSet = temp[1];
				}
				nextGene = br.readLine();
			}
			Chromosome ch = new Chromosome(chromosomeRecord, 
					allActionSets.get(currentChromosome),
					allVariableSets.get(currentVariableSet));
			chromosomeTable.put(currentChromosome, ch);
			br.close();
		} 
		catch (Exception e)
		{
			System.err.println("File input error "+e.toString());
			logger.severe("Error in reading Genome " + f.toString() + " : " + e.toString());
		}
	}

	public void recordGenome(File f) {
		List<String> geneticRecord = getAllGenesAsString();
		try {
			FileWriter geneticWriter = new FileWriter(f,true);
			for (String s : geneticRecord) {
				geneticWriter.write(s + newline);
			}
			geneticWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.severe("Error writing Genome: " +e.toString());
		}	
	}


	public Genome clone() {
		Genome retValue = new Genome();
		for (Chromosome ch : chromosomeTable.values()) {
			Chromosome clonedCh = ch.clone();
			retValue.chromosomeTable.put(ch.toString(), clonedCh);
		}
		return retValue;
	}

	public Genome crossWith(Genome cross) {
		/* For each Chromosome
		 * 
		 */

		Genome retValue = new Genome();
		for(Chromosome ch : chromosomeTable.values()) {
			Chromosome crossCh = cross.chromosomeTable.get(ch.toString());
			if (crossCh != null) {
				crossCh = crossCh.crossWith(ch);
				retValue.chromosomeTable.put(crossCh.toString(), crossCh);
			} else {
				logger.warning("Chromosome mismatch on " + ch.toString());
				// Don't add Chromosome unless both have it
			}
		}
		return retValue;
	}

	public Chromosome getChromosome(String desc){
		Chromosome retValue = chromosomeTable.get(desc);
		if (retValue == null) return null;
		return retValue.clone();
	}

	public void setChromosome(Chromosome newChr) {
		chromosomeTable.put(newChr.toString(), newChr);
	}

	public void mutate() {
		for (Chromosome ch : chromosomeTable.values()) {
			ch.mutate();
		}
	}

	public double getValue(ActionEnum ae, Agent a, double var, List<GeneticVariable> varSet) {
		double retValue = 0.0;
		Gene g = getGene(ae, varSet);
		if (g != null) {
			retValue = g.getValue(a, var);
		}
		return retValue;
	}
	public double getValue(ActionEnum ae, Agent a, Artefact item, List<GeneticVariable> varSet) {
		double retValue = 0.0;
		Gene g = getGene(ae, varSet);
		if (g != null) {
			retValue = g.getValue(a, item);
		}
		return retValue;
	}
	public double getValue(ActionEnum ae, Agent a, List<GeneticVariable> varSet) {
		return getValue(ae, a, a, varSet);
	}
	public double getValue(ActionEnum ae, Agent a, Agent context, List<GeneticVariable> varSet) {
		double retValue = 0.0;
		Gene g = getGene(ae, varSet);
		if (g != null) {
			retValue = g.getValue(a, context);
		}
		return retValue;
	}
	public Gene getGene(ActionEnum ae, List<GeneticVariable> variables) {
		// first we find out the Chromosome the Gene is on
		// on the basis that there is one Chromosome per ActionSet

		Chromosome ch = chromosomeTable.get(ae.getChromosomeDesc());
		if (ch == null) {
			ArrayList<ActionEnum> actions = allActionSets.get(ae.getChromosomeDesc());
			if (actions != null && variables != null) {
				ch = new Chromosome(actions, variables);
				chromosomeTable.put(ae.getChromosomeDesc(), ch);
			} else {
				logger.warning("Unable to create Chromosome as no record of action or genetic set for " + ae.toString());
			}
		}		
		if (ch != null ) {
			Gene g = ch.getGene(ae);
			if (g == null && variables != null) {
				g = new Gene(ae, variables);
				ch.addGene(g);
			}
			return g;
		}
		return null;
	}
	public List<String> getAllGenesAsString() {
		ArrayList<String> geneticRecord = new ArrayList<String>();
		for (Chromosome ch : chromosomeTable.values()){
			geneticRecord.addAll(ch.toStringArray());
		}
		Collections.sort(geneticRecord);
		return geneticRecord;
	}
	public List<Gene> getAllGenes() {
		// we only want to return currently active Genes
		ArrayList<Gene> geneticRecord = new ArrayList<Gene>();
		for (Chromosome ch : chromosomeTable.values()) 
			for (Gene g : ch.getAllGenes())
				geneticRecord.add(g);

		return geneticRecord;
	}

	public String toString() {
		return "Genome";
	}
	public List<String> allChromosomesToString() {
		ArrayList<String> retValue = new ArrayList<String>();
		for (Chromosome ch : chromosomeTable.values()) {
			retValue.addAll(ch.toStringArray());
		}
		return retValue;
	}
	public int geneticTermCount() {
		int retValue = 0;
		for (Chromosome c : chromosomeTable.values()) {
			retValue+= c.geneticTermCount();
		}
		return retValue;
	}

	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public static FilenameFilter createGenomeFilter(String searchText) {
		return new HopshackleFilter(searchText, "txt");
	}

	public static List<Genome> loadGenomes(File genomeLocation, FilenameFilter filter) {

		List<Genome> retValue = new ArrayList<Genome>();
		if (genomeLocation == null || !genomeLocation.isDirectory()) {
			logger.severe("Error in loading Genomes, specified Directory isn't: " + genomeLocation);
			return new ArrayList<Genome>();
		}

		File files[] = genomeLocation.listFiles(filter);

		for (File f : files) {
			Genome g = new Genome(f);
			retValue.add(g);
		}

		return retValue;
	}
}
