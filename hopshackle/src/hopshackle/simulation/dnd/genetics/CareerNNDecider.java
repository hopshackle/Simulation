package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;

import java.util.*;

/** 
 * CareerNNDecider works on the basis that one Neural Network is maintained for each character class.
 * Each is used to determine the expected XP a new character will achieve in a lifetime.

 * When the character dies, a Death event is picked up by the Teacher, which then trains the relevant Network with
 * the actual observed result.
 * 
 * @author James Goodman
 * @version 1	
 *
 */
public class CareerNNDecider extends NeuralDecider {

	public CareerNNDecider()  {
		super(null, null);
		List<ActionEnum> actions = new ArrayList<ActionEnum>(EnumSet.allOf(CareerActionsI.class));
		List<GeneticVariable> variables = new ArrayList<GeneticVariable>(EnumSet.allOf(CareerGeneticEnum.class));

		this.setActions(actions);
		this.setVariables(variables);
		this.setName("CAREER");
	}

	/* This converts a Neural Decider into a CareerNeuralDecider
	 * 
	 */
	public static CareerNNDecider CareerNNDeciderFactory(NeuralDecider nd) {

		CareerNNDecider retValue = new CareerNNDecider();
		if (nd.getActions().size() != retValue.getActions().size()) {
			logger.severe("Incorrect action input");
			return null;
		}
		if (nd.getVariables().size() != retValue.getVariables().size()) {
			logger.severe("Incorrect variable input");
			return null;
		}
		retValue.updateBrain(nd);
		return retValue;
	}
	
	@Override
	public Decider crossWith(Decider otherDecider) {
		NeuralDecider nd = (NeuralDecider) super.crossWith(otherDecider);
		CareerNNDecider retValue = CareerNNDecider.CareerNNDeciderFactory(nd);
		retValue.setTeacher(this.getTeacher());
		return retValue;
	}

}
