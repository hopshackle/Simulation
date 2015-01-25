/**
 * 
 */
package hopshackle.simulation;

import java.io.InvalidObjectException;
import java.util.*;

class GeneticTerm implements Comparable {
	/**
	 * 
	 */
	private GeneticVariable gv;
	private String operator;
	
	/* Each holds a single genetic Variable, and the operator to be used
	 * when applying this to the total
	 */
	
	GeneticTerm(GeneticVariable genVar) {
		gv = genVar;
		if (gv.unitaryRange() || Math.random() < 0.5) {
			operator = "*";
		} else {
			operator = "/";
		}
	}
	
	GeneticTerm (GeneticVariable genVar, String op) {
		operator = "*";
		if (op.equals("/")) operator = "/";
		// default to multiplication - but divide allowed
		
		gv = genVar;
	}
	
	GeneticTerm invert() {
		String invertedOp = "*";
		if (operator.equals("*")) invertedOp = "/";
		return new GeneticTerm(gv, invertedOp);
	}
	
	GeneticTerm(String s, List<GeneticVariable> es) throws InvalidObjectException {
		// Instantiate a GeneticTerm from a String of Format:
		// Operator Variable
		StringTokenizer st = new StringTokenizer(s, " ");
		while (st.hasMoreTokens())
		{
			operator = st.nextToken();
			if (!operator.equals("*") && !operator.equals("/")) {
				Codon.logger.severe("Operator invalid: " + operator);
				operator = "*";
			}
			
			String nextTerm = st.nextToken();
			for (Object o : es) {
				GeneticVariable findGV = (GeneticVariable) o;
				if (findGV.toString().equals(nextTerm)) {
					gv = findGV;
					break;
				}
			}
			if (gv == null) {
				Codon.logger.severe("No match found for Genetic Term " + nextTerm);
				gv = (GeneticVariable) es.toArray()[0];
				throw new InvalidObjectException("No Match found for Genetic Term " + nextTerm);
			} 
		}
	}
	
	public String toString() {
		return operator + " " + gv.toString();
	}
	
	public double getValue(Agent a, double var){
		double retValue =  gv.getValue(a, var);
		if (operator.equals("/")) {
			if (retValue == 0.0) retValue = 0.01;
			retValue = 1.0 / retValue;
		} 
		return retValue;
	}
	public double getValue(Agent a1, Agent a2){
		double retValue =  gv.getValue(a1, a2);
		if (operator.equals("/")) {
			if (retValue == 0.0) retValue = 0.01;
			retValue = 1.0 / retValue;
		} 
		return retValue;
	}
	public double getValue(Agent a, Artefact item){
		double retValue =  gv.getValue(a, item);
		if (operator.equals("/")) {
			if (retValue == 0.0) retValue = 0.01;
			retValue = 1.0 / retValue;
		} 
		return retValue;
	}

	public boolean isSimilar(GeneticTerm gt) {
		return (gv.toString()+operator).equals(gt.gv.toString()+gt.operator);
	}
	
	public int compareTo(Object arg0) {
		if (!(arg0 instanceof GeneticTerm)) {
		return 0;
		}
		GeneticTerm gt = (GeneticTerm) arg0;
		int retValue = (gv.getDescriptor()+operator).compareTo(gt.gv.getDescriptor()+gt.operator);

		return retValue;
	}
}