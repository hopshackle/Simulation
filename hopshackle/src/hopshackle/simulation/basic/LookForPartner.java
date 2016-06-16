package hopshackle.simulation.basic;

public class LookForPartner extends BasicAction {

	Marry marriageAction;
	
	public LookForPartner(BasicAgent ba) {
		super(BasicActions.LOOK_FOR_PARTNER, ba, 500, true);
	}
	
	@Override
	public void doStuff() {
		BasicAgent ba = (BasicAgent) actor;
		PartnerFinder advertMarry = new PartnerFinder(ba, new MarriagePartnerScoringFunction(ba));
		if (advertMarry.getPartner() != null && !ba.getActionPlan().contains(BasicActions.MARRY)) {
			marriageAction = new Marry(ba, advertMarry.getPartner());
			marriageAction.addToAllPlans();
		} else {
			ba.log("Fails to find a partner.");
		}
	}
	
	@Override
	public String toString() {
		return "LOOK_FOR_PARTNER";
	}
	
}
