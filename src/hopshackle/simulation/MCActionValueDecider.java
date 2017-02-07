package hopshackle.simulation;

import java.util.List;

public class MCActionValueDecider<A extends Agent> extends BaseDecider<A> {

	private MonteCarloTree<A> tree;
	private double actionTemperature = SimProperties.getPropertyAsDouble("MonteCarloActionValueDeciderTemperature", "0.00");
	
	public MCActionValueDecider(MonteCarloTree<A> tree, StateFactory<A> stateFactory) {
		super(stateFactory);
		this.tree = tree;
		localDebug = false;
	}
	
	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return tree.getActionValue(option.toString());
	}
	
	@Override
	protected ActionEnum<A> selectOption(List<ActionEnum<A>> optionList, A decidingAgent) {
		if (actionTemperature > 0.0001)
			return selectOptionUsingBoltzmann(optionList, decidingAgent);
		return super.selectOption(optionList, decidingAgent);		// choose greedily
	}
	
	protected List<Double> getNormalisedBoltzmannValuesPerOption(List<ActionEnum<A>> optionList, A decidingAgent){
		// we just need to override the temperature to use the AVDecider specific one
		return getNormalisedBoltzmannValuesPerOption(optionList, decidingAgent, actionTemperature);
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		// Do nothing
	}

}
