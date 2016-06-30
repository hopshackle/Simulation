package hopshackle.simulation;

import java.util.*;
import java.util.logging.Logger;

public class Gene {

	private static boolean trimCodons;
	private static double newCodon, swapCodon, duplicateCodon, removeCodon, modChange, shift;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");

	private List<Codon> codons;
	private double base;
	private ActionEnum associatedAction;
	private List<GeneticVariable> genVarSet;
	private boolean isMutable, mutabilityInitialised;

	static {
		modChange = SimProperties.getPropertyAsDouble("MaxModifierChange", "0.05");
		shift = SimProperties.getPropertyAsDouble("ModifierShift", "0.003");
		newCodon = SimProperties.getPropertyAsDouble("NewCodon", "0.005");
		swapCodon = SimProperties.getPropertyAsDouble("SwapCodon", "0.01");
		trimCodons = SimProperties.getProperty("CodonRemovalProportionalToNumber", "false").equals("true");
		removeCodon = SimProperties.getPropertyAsDouble("RemoveCodon", "0.0025");
		duplicateCodon = SimProperties.getPropertyAsDouble("DuplicateCodon", "0.00125");
	}

	public Gene(ActionEnum ae, List<GeneticVariable> gvs) {
		associatedAction = ae;
		genVarSet = gvs;
		codons = new ArrayList<Codon>();
		base = Math.random() -.5;
	}

	public Gene(String s, List<GeneticVariable> geneVarSet, List<ActionEnum> actionSet) {
		genVarSet = geneVarSet;
		codons = new ArrayList<Codon>();
		// Instantiate from a String of the format:
		// ActionName|Codon|Codon

		StringTokenizer st = new StringTokenizer(s, "|");
		String actionName = st.nextToken();
		associatedAction = null;
		for (ActionEnum ae : actionSet) {
			if (ae.toString().equals(actionName)) {
				associatedAction = ae;
			}
		}
		if (associatedAction == null) {
			logger.severe("Action " + actionName + " not found in ActionSet");
			associatedAction = (ActionEnum) actionSet.toArray()[0];
		}
		String baseStr = st.nextToken();
		base = Double.valueOf(baseStr);

		while (st.hasMoreTokens())
		{
			String nextCodon = st.nextToken();
			codons.add(new Codon(nextCodon, genVarSet));
		}
	}


	public Gene clone() {
		Gene retValue = new Gene(associatedAction, genVarSet);
		List<Codon> clonedCodons = new ArrayList<Codon>();
		for (Codon c : codons) {
			clonedCodons.add(c.clone());
		}
		retValue.codons = clonedCodons;
		retValue.base = this.base;
		return retValue;
	}

	public boolean mutate() {		
		if (!isMutable())
			return false;

		double t = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		base = base +  (Math.random() -.5)*modChange * t;
		base *= (1.0 - shift * t); // tend to move towards zero
		ArrayList<Codon> remList = new ArrayList<Codon>();
		ArrayList<Codon> addList = new ArrayList<Codon>();
		int noOfCodons = codons.size();
		double chanceToRemove = removeCodon;
		if (trimCodons) chanceToRemove *= noOfCodons;
		for (Codon c : codons) {

			if (Math.random() < (chanceToRemove * t)) {
				// remove the term
				remList.add(c);
			} else {
				if (Math.random() <duplicateCodon * t) {
					//duplicate the term
					addList.add(c.clone());
				}
				c.mutate();
			}
		}
		for (Codon c : remList) {
			codons.remove(c);
		}
		for (Codon c : addList) {
			codons.add(c);
		}
		for (int loop = 1; loop < codons.size()-1; loop++) {
			// See if any should be swapped
			if (Math.random()<swapCodon * t) {
				int swapIndex = loop -1;
				if (Math.random() > 0.5) 
					swapIndex = loop+1;
				Codon temp1Codon = codons.get(swapIndex);
				Codon temp2Codon = codons.get(loop);
				codons.set(loop, temp1Codon);
				codons.set(swapIndex, temp2Codon);
			}
		}
		if (Math.random() < newCodon * t) {
			codons.add(new Codon(genVarSet));
		}
		return true;
	} 

	public boolean isMutable() {
		if (!mutabilityInitialised) {
			String defaultMutability = SimProperties.getProperty("GeneticMutation", "true");
			String overrideMutability = SimProperties.getProperty("GeneticMutation." + associatedAction.toString(), defaultMutability);
			isMutable = (overrideMutability.equals("true"));
			mutabilityInitialised = true;
		}
		return isMutable;
	}

	public void setGeneticVariables(List<GeneticVariable> gvSet) {
		genVarSet = gvSet;
		for (Codon c : codons) {
			c.setGeneticVariables(gvSet);
		}
	}

	public String toString() {
		StringBuffer tempString = new StringBuffer();
		tempString.append(associatedAction.toString());
		tempString.append(String.format("|%.2f", base));

		for (Codon c : codons) {
			tempString.append("|" + c.toString());
		}
		return tempString.toString();
	}

	public Double getValue(Agent a) {
		return getValue(a, a);
	}
	public Double getValue(Agent a1, Agent a2) {
		double retValue = base;
		for (Codon c : codons) {
			retValue += c.getValue(a1, a2);
		}
		return retValue;
	}
	public Double getValue(Agent a, Artefact item) {
		double retValue = base;
		for (Codon c : codons) {
			retValue += c.getValue(a, item);
		}
		return retValue;
	}

	public ActionEnum getAction() {
		return associatedAction;
	}

	public boolean isSimilar(Gene g2) {
		// we first need to sort the codon arrays of both genes
		// and then run through to see if they're similar 
		if (!(associatedAction.equals(g2.associatedAction))) return false;
		List<Codon> set1 =  HopshackleUtilities.cloneList(codons);
		List<Codon> set2 =  HopshackleUtilities.cloneList(g2.codons);
		Codon testCodon1 = new Codon("0.10",genVarSet);
		Codon testCodon2 = new Codon("-0.10",genVarSet);
		List<Codon> nullCodons = new ArrayList<Codon>();
		for (Codon c : set1) {
			if (c.isSimilar(testCodon1)) nullCodons.add(c);
			if (c.isSimilar(testCodon2)) nullCodons.add(c);
		}
		for (Codon c : nullCodons) set1.remove(c);
		nullCodons.clear();
		for (Codon c : set2) {
			if (c.isSimilar(testCodon1)) nullCodons.add(c);
			if (c.isSimilar(testCodon2)) nullCodons.add(c);
		}
		for (Codon c : nullCodons) set2.remove(c);

		Collections.sort(set1);
		Collections.sort(set2);
		int n = 0;
		if (set1.size() == 0 && set2.size() ==0 ) return true;
		if (set1.size() != set2.size()) {
			return false;
		} else {
			do {
				if (!(set1.get(n).isSimilar(set2.get(n)))) {
					return false;
				}
				n++;
			} while (set1.size() > n);
		}
		return true;
	}

	public List<Codon> getCodons() {
		ArrayList<Codon> retValue = new ArrayList<Codon>();
		// remove empty codons as well
		for (Codon c : codons) {
			if (!c.isEmpty()) retValue.add(c);
		}
		return retValue;
	}

	public double getBase() {
		return base;
	}
	public int geneticTermCount() {
		int retValue = 0;
		for (Codon c : codons) {
			retValue += c.geneticTermCount();
		}
		return retValue;
	}

	public Gene crossWith(Gene crossSplitGene) {
		return crossWith(crossSplitGene, Math.random());
	}

	public Gene crossWith(Gene crossSplitGene, double splitDBL) {
		Gene retValue = this.clone();
		int thisCodons = codons.size();
		int thisStartSize = thisCodons;
		int crossCodons = crossSplitGene.codons.size();
		int crossStartSize = crossCodons;

		thisCodons = (int) Math.round((double)thisCodons * splitDBL);
		crossCodons = crossCodons - (int)Math.round((double)crossCodons * (1.0-splitDBL));
		// the numbers to pull over from each

		for (int loop = thisStartSize-1; loop >= thisCodons ; loop--)
			retValue.codons.remove(loop);
		// so remove the later codons from this
		// and add the later codons from cross

		for (int loop = crossCodons; loop < crossStartSize; loop++ )
			retValue.codons.add(crossSplitGene.codons.get(loop));

		return retValue;
	}
}
