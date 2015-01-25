package hopshackle.simulation;

import java.util.*;
import java.util.logging.Logger;

public class Chromosome {
	
	/* What do we want to be able to do with a chromosome?
	 * 		- mutate()
	 * 		- return a value based on the context of a given Agent and Action
	 * 		- serialise it via a String representation
	 * 		- instantiate it from a String representation
	 * 		- return a prioritised list of the Actions
	 * 		- set a Gene 
	 * 		- get a Gene
	 * 		
	 */
	
	private Hashtable<ActionEnum, Gene> genes;
	private String chromosomeName;
	private String geneticVarName;
	private List<ActionEnum> actionSet;
	private List<GeneticVariable> genVarSet;

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");

	public Chromosome(List<ActionEnum> actions, List<GeneticVariable> genVars) {
		chromosomeName = actions.get(0).getChromosomeDesc();
		actionSet = actions;
		genVarSet = genVars;
		geneticVarName = genVars.get(0).getDescriptor();
		genes = new Hashtable<ActionEnum, Gene>();
	}
	public Chromosome(List<String> record, List<ActionEnum> actions, List<GeneticVariable> genVars) {
		this(actions, genVars);
		// Instantiate from an Array of Strings, with each line of the Array
		// being a single Gene

		for (String s : record) {
			ActionEnum key;
			Gene newGene = new Gene(s, genVarSet, actionSet);
			key = newGene.getAction();
			genes.put(key, newGene);
		}
	}

	public Chromosome clone() {
		Chromosome retValue = new Chromosome(actionSet, genVarSet);
		Hashtable<ActionEnum, Gene> clonedGenes = new Hashtable<ActionEnum, Gene>();
		for (Gene g : genes.values()) {
			clonedGenes.put(g.getAction(), g.clone());
		}
		retValue.genes = clonedGenes;
		return retValue;
	}

	public void mutate() {
		for (Gene g : genes.values()) {
			g.mutate();
		}
	} 
	
	public Chromosome crossWith(Chromosome cross) {
		// the plan here is to take a cross Point - and take all Genes from one Chromosome
		// before this point, and all genes from the other after this point
		
		/* SplitDBL is the precise split point
		 * then splitPoint it the (int) version to say how many full Genes come from the 
		 * first Chromosome.
		 * This leaves the %age of the gene to be taken across.
		 */
		if (cross == null) return this.clone();
		
		ActionEnum key;
		Chromosome retValue = new Chromosome(actionSet, genVarSet);
		
		Chromosome first = this;
		Chromosome second = cross;
		if (Math.random() > 0.5) {
			first = cross;
			second = this;
		}
		
		double splitDBL = Math.random() * first.genes.size();
		int splitPoint = (int)splitDBL;
		splitDBL = splitDBL - splitPoint;
		
		Enumeration<ActionEnum> tempKeys = first.genes.keys();
		for (int n = 0; n< splitPoint; n++) {
			key = tempKeys.nextElement();
			retValue.genes.put(key, first.genes.get(key).clone());
		}
		// We now have the split Gene (possibly)
		// this check is so that we don't always take the base+priority from thisGene
		// and the codons from crossGene (which would not be what we want)

		key = tempKeys.nextElement();
		Gene splitGene = null;
		Gene crossSplitGene = second.genes.get(key);
		Gene thisSplitGene = first.genes.get(key);
		if (crossSplitGene == null) crossSplitGene = thisSplitGene;
		if (thisSplitGene == null) thisSplitGene = crossSplitGene;
		
		long fromCross = Math.round(crossSplitGene.getCodons().size() * (1.0-splitDBL));
		long fromThis = Math.round(thisSplitGene.getCodons().size() * splitDBL);

//		System.out.println(String.format(("Gene1: %s, Gene2: %s, SplitDBL: %.2f, from1: %d, from2: %d"),
//								thisSplitGene.toString(), crossSplitGene.toString(), splitDBL, fromThis, fromCross));
		
		if (fromThis > 0 && fromCross > 0) {
			splitGene = thisSplitGene.crossWith(crossSplitGene, splitDBL);
			retValue.genes.put(key, splitGene); 
			// i.e. only split if you're taking at least one from each gene
		} else	
			// too small to split. We use the second 
			retValue.genes.put(key, crossSplitGene.clone());

		// end of split gene - on to the ones we take directly from second

		for (; tempKeys.hasMoreElements(); ) {
			key = tempKeys.nextElement();
			Gene newGene = second.genes.get(key);
			if (newGene == null) newGene = first.genes.get(key);
			newGene = newGene.clone();
			retValue.genes.put(key, newGene);
		}
		return retValue;
	}

	public void setGeneticVariables(ArrayList<GeneticVariable> genVars) {
		genVarSet = genVars;
		for (Gene g : genes.values()) {
			g.setGeneticVariables(genVars);
		}
	}
	public List<GeneticVariable> getVariables() {
		return genVarSet;
	}

	public String toString() {
		return chromosomeName;
	}
	public List<String> toStringArray() {
		List<String> retValue = new ArrayList<String>();
		for (Gene g : genes.values()) {
			retValue.add(chromosomeName + ":" + geneticVarName +":" + g.toString());
		}
		Collections.sort(retValue);
		return retValue;
	}

	public Double getValue(ActionEnum ae, Agent a) {
		Gene g = genes.get(ae);
		if (g==null) {
			g = new Gene(ae, genVarSet);
			genes.put(ae, g);
		}
		return g.getValue(a);
	}
	public Double getValue(ActionEnum ae, Agent a1, Agent a2) {
		Gene g = genes.get(ae);
		if (g==null) {
			g = new Gene(ae, genVarSet);
			genes.put(ae, g);
		}
		return g.getValue(a1, a2);
	}
	public Double getValue(ActionEnum ae, Agent a, Artefact item) {
		Gene g = genes.get(ae);
		if (g==null) {
			g = new Gene(ae, genVarSet);
			genes.put(ae, g);
		}
		return g.getValue(a, item);
	}
	
	public void addGene(Gene g) {
		ActionEnum ae = g.getAction();
		if (actionSet.contains(ae)) {
			genes.put(ae, g);
		} else {
			logger.severe("Cannot insert Gene as its action is not in ActionSet: " + ae.toString());
		}
	}
	public Gene getGene(ActionEnum ae) {
		if (genes.containsKey(ae)) {
			return genes.get(ae).clone();
		}
		return null;
	}

	public int geneticTermCount() {
		int retValue = 0;
		for (Gene g : genes.values()) {
			retValue += g.geneticTermCount();
		}
		return retValue;
	}
	
	public Collection<Gene> getAllGenes() {
		return genes.values();
	}
}
