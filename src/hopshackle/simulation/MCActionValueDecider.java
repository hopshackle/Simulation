package hopshackle.simulation;

import java.util.List;

public class MCActionValueDecider<A extends Agent> extends BaseDecider<A> {

	private MonteCarloTree<A> tree;
	private double actionTemperature = getPropertyAsDouble("MonteCarloActionValueDeciderTemperature", "0.00");
	private int actorRef;
	
	public MCActionValueDecider(MonteCarloTree<A> tree, StateFactory<A> stateFactory, int actorRef) {
		super(stateFactory);
		this.tree = tree;
		this.actorRef = actorRef;
		localDebug = false;
	}
	
	@Override
	public void injectProperties(DeciderProperties dp) {
		super.injectProperties(dp);
		actionTemperature = getPropertyAsDouble("MonteCarloActionValueDeciderTemperature", "0.00");
	};
	
	
	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return tree.getActionValue(option.toString(), actorRef);
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
