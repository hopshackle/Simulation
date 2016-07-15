package hopshackle.simulation;

import java.io.*;
import java.util.*;

import org.encog.neural.networks.BasicNetwork;

public class SimpleStateNeuralDecider<A extends Agent> extends NeuralDecider<A, SimpleState<A>> {

	public SimpleStateNeuralDecider(List<? extends ActionEnum<A>> actions, List<GeneticVariable<A, SimpleState<A>>> variables) {
		super(actions, variables);
	}

	@Override
	public SimpleState<A> getCurrentState(A decidingAgent) {
		return new SimpleState<A>(decidingAgent, variableSet);
	}
	


	@SuppressWarnings("unchecked")
	public static <A extends Agent> SimpleStateNeuralDecider<A> createNeuralDecider(File saveFile) {
		SimpleStateNeuralDecider<A> retValue = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile));

			ArrayList<ActionEnum<A>> actionSet = (ArrayList<ActionEnum<A>>) ois.readObject();
			ArrayList<GeneticVariable<A, SimpleState<A>>> variableSet = (ArrayList<GeneticVariable<A, SimpleState<A>>>) ois.readObject();
			retValue = new SimpleStateNeuralDecider<A>(actionSet, variableSet);

			BasicNetwork[] actionNN = new BasicNetwork[actionSet.size()];
			for (int n=0; n<actionSet.size(); n++)
				actionNN[n] = (BasicNetwork) ois.readObject();

			ois.close();
			retValue.setBrain(actionNN);
			String name = saveFile.getName();
			String end = ".brain";
			name = name.substring(0, name.indexOf(end));
			retValue.setName(name);

		} catch (Exception e) {
			logger.severe("Error reading combat brain: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		}

		return retValue;
	}

	@Override
	/* 
	 * 	For Neural Deciders, we can cross them with another
	 *  as long as the action and variable Sets are identical
	 *  (or at least have the same number of items across the two Deciders)
	 */
	public Decider<A, SimpleState<A>> crossWith(Decider<A, SimpleState<A>> otherDecider) {
		if (!(otherDecider instanceof NeuralDecider))
			return super.crossWith(otherDecider);
		if (this.variableSet.size() != otherDecider.getVariables().size())
			return super.crossWith(otherDecider);
		if (this.actionSet.size() != otherDecider.getActions().size())
			return super.crossWith(otherDecider);
		if (this == otherDecider) return this;

		SimpleStateNeuralDecider<A> retValue = new SimpleStateNeuralDecider<A>(actionSet, variableSet);
		BasicNetwork[] newBrain = new BasicNetwork[actionSet.size()];
		for (int n = 0; n < actionSet.size(); n++) {
			// 50:50 chance for each action which Network we take
			if (Math.random() < 0.5) {
				newBrain[n] = this.brain.get(actionSet.get(n).toString());
			} else {
				newBrain[n] = ((SimpleStateNeuralDecider<A>)otherDecider).brain.get(actionSet.get(n).toString());
			}
		}

		retValue.setBrain(newBrain);
		retValue.setName(toString());
		return retValue;
	}

}
