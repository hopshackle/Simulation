package hopshackle.simulation;

public class ERCAllocationPolicyByMaster<A extends Agent> implements ExperienceRecordCollector.ERCAllocationPolicy<A> {

    private ExperienceRecordCollector<A> erc;

    public ERCAllocationPolicyByMaster(ExperienceRecordCollector<A> erc) {
        this.erc = erc;
    }

    @Override
    public void apply(A agent) {
        if (agent.getGame() != null) {
            A master = (A) agent.getGame().getMasterOf(agent);
            A ref = erc.getReferenceFor(master);
            erc.registerAgentWithReference(agent, ref);
        }
    }
}
