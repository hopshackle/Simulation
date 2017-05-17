package hopshackle.simulation;

public class MCActionValueDecider<A extends Agent> extends BaseAgentDecider<A> {

	private MonteCarloTree<A> tree;
	private int actorRef;
	
	public MCActionValueDecider(MonteCarloTree<A> tree, StateFactory<A> stateFactory, int actorRef) {
		super(stateFactory);
		this.tree = tree;
		this.actorRef = actorRef;
		localDebug = false;
	}

	@Override
	public double valueOption(ActionEnum<A> option, A decidingAgent) {
		return tree.getActionValue(option.toString(), actorRef);
	}
	@Override
	public double valueOption(ActionEnum<A> option, State<A> state) {
		return tree.getActionValue(option.toString(), actorRef);
	}

	@Override
	public void learnFrom(ExperienceRecord<A> exp, double maxResult) {
		// Do nothing
	}

}
