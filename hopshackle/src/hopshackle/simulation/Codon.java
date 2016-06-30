package hopshackle.simulation;

import java.io.InvalidObjectException;
import java.util.*;
import java.util.logging.Logger;

public class Codon implements Comparable {
	private List<GeneticVariable> genVarSet;
	private List<GeneticTerm> codon;
	private double modifier;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");
	private static double newTerm, removeTerm, modChange, shift;
	
	/* What do we want to be able to do with a codon?
	 * 		- mutate()
	 * 		- set the Genetic Variable list to use
	 * 		- return a value based on the context of a given Agent
	 * 		- serialise it via a String representation
	 * 		- instantiate it from a String representation
	 */
	
	static {
		modChange = SimProperties.getPropertyAsDouble("MaxModifierChange", "0.05");
		shift = SimProperties.getPropertyAsDouble("ModifierShift", "0.003");
		newTerm = SimProperties.getPropertyAsDouble("NewGeneticTerm", "0.005");
		removeTerm = SimProperties.getPropertyAsDouble("RemoveGeneticTerm", "0.0025");
	}
	
	public Codon(List<GeneticVariable> gvs) {
		codon = new ArrayList<GeneticTerm>();
		modifier = (Math.random() -.5);
		genVarSet = gvs;
		addGeneticTerm();
	}
	
	public Codon(String s, List<GeneticVariable> gvs) {
		// Instantiate from a String of the format:
		// Modifer>GeneticTerm>GeneticTerm
		// in this case do not add a random Genetic Term
		
		codon = new ArrayList<GeneticTerm>();
		modifier = (Math.random() -.5);
		genVarSet = gvs;
		StringTokenizer st = new StringTokenizer(s, ">");
		String modStr = st.nextToken();
		modifier = Double.valueOf(modStr);
		while (st.hasMoreTokens())
		{
			String nextTerm = st.nextToken();
			try {
				codon.add(new GeneticTerm(nextTerm, genVarSet));
			} catch (InvalidObjectException e) {
				// Genetic Term invalid. 
				// we don't add it, and proceed onwards
			}
		}
	}

	public Codon clone() {
		Codon retValue = new Codon(genVarSet);
		List<GeneticTerm> clonedCodon = new ArrayList<GeneticTerm>();
		for (GeneticTerm gt : codon) {
			clonedCodon.add(gt);
			// GeneticTerms are invariant - so no need to clone them
		}
		retValue.codon = clonedCodon;
		retValue.modifier = this.modifier;
		return retValue;
	}
	public void mutate() {
		double t = SimProperties.getPropertyAsDouble("Temperature", "1.0");
		modifier = modifier +  (Math.random() -.5) * modChange * t;
		modifier *= (1.0 - shift * t); // tend to move towards zero
		List<GeneticTerm> remList = new ArrayList<GeneticTerm>();
		int noOfGeneticTerms = codon.size();
		for (GeneticTerm gt : codon) {
			if (Math.random() < removeTerm * noOfGeneticTerms * t) {
				// remove the term
				remList.add(gt);
			}
		}
		for (GeneticTerm gt : remList) {
			codon.remove(gt);
		}
		if (Math.random() < (newTerm * t) && genVarSet != null) {
			addGeneticTerm();
		}
	} 

	public void setGeneticVariables(List<GeneticVariable> gvSet) {
		genVarSet = gvSet;
	}

	public String toString() {
		StringBuffer tempString = new StringBuffer();
		tempString.append(String.format("%.2f", modifier));
		
		for (GeneticTerm gt : codon) {
			tempString.append(">" + gt.toString());
		}
		return tempString.toString();
	}
	public String toPrettyString() {
		// firstly sort Genetic Terms
		List<GeneticTerm> set1 =  HopshackleUtilities.cloneList(codon);
		Collections.sort(set1);
		StringBuffer tempString = new StringBuffer();
		if (modifier < 0.00) {
			tempString.append("MINUS ");
		} else {	
			tempString.append("PLUS  ");
		}

		for (GeneticTerm gt : set1) {
			tempString.append(">" + gt.toString());
		}
		return tempString.toString();
	}

	public double getValue(Agent a1, Agent a2) {
		double retValue = modifier;
		for (GeneticTerm gt : codon) {
			retValue *= gt.getValue(a1, a2);
		}
		if (codon.isEmpty()) retValue = 0.0;
		return retValue;
	}
	public double getValue(Agent a, Artefact item) {
		double retValue = modifier;
		for (GeneticTerm gt : codon) {
			retValue *= gt.getValue(a, item);
		}
		if (codon.isEmpty()) retValue = 0.0;
		return retValue;
	}

	public boolean isEmpty() {
		return codon.isEmpty();
	}
	
	void addGeneticTerm() {
		GeneticVariable genVar = (GeneticVariable) genVarSet.toArray()[Dice.roll(1, genVarSet.size())-1];
		GeneticTerm newTerm = new GeneticTerm(genVar);
		//Now check to see if there is the inverse of this term already present
		// if so - remove the inverse instead
		GeneticTerm inverseTerm = newTerm.invert();
		GeneticTerm termToRemove = null;
		for (GeneticTerm gt : codon) {
			if (gt.isSimilar(inverseTerm)) termToRemove = gt;
		}
		if (termToRemove == null) {
			codon.add(newTerm);
		}
		else {
			codon.remove(termToRemove);
		}
	}

	public boolean isSimilar (Object arg0) {
		if (!(arg0 instanceof Codon)) {
			return false;
		}
		//firstly sort the codons
		List<GeneticTerm> set1 =  HopshackleUtilities.cloneList(codon);
		List<GeneticTerm> set2 =  HopshackleUtilities.cloneList(((Codon)arg0).codon);
		// then remove any Genetic Terms which have no variables
		Collections.sort(set1);
		Collections.sort(set2);
		int n = 0;
		if (modifier >0.00 && ((Codon)arg0).modifier < 0.00) return false;
		if (modifier <0.00 && ((Codon)arg0).modifier > 0.00) return false;
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

	public int compareTo(Object arg0) {
		if (!(arg0 instanceof Codon)) {
			return 0;
		}
		Codon c2 = (Codon) arg0;
		List<GeneticTerm> set1 = HopshackleUtilities.cloneList(codon);
		List<GeneticTerm> set2 = HopshackleUtilities.cloneList(c2.codon);
		Collections.sort(set1);
		Collections.sort(set2);
		int n = 0;
		if (set1.size() == 0 && set2.size() ==0 ) return (int) (1000*(c2.modifier - modifier));
		if (set1.size() != set2.size()) {
			return set2.size() - set1.size();
		} else {
			do {
				if (!(set1.get(n).isSimilar(set2.get(n)))) {
					return set1.get(n).compareTo(set2.get(n));
				}
				n++;
			} while (set1.size() > n);
		}
		return (int) (1000*(c2.modifier - modifier));
	}

	public double getModifier() {
		return modifier;
	}
	public int geneticTermCount() {
		return codon.size();
	}


}
