package hopshackle.simulation;


public class MembershipDetails {

	private long member;
	private long membershipStart;
	private long membershipEnd;
	private long organisation;
	
	public MembershipDetails(Agent member, Organisation<?> org) {
		this.member = member.getUniqueID();
		membershipStart = member.getWorld().getCurrentTime();
		membershipEnd = -1l;
		organisation = org.getUniqueID();
	}

	public void membershipTerminates(long endTime) {
		membershipEnd = endTime;
	}

	public boolean hasTerminated() {
		return membershipEnd != -1l;
	}
	public long getMember() {return member;}
	public long getMembershipStart() {return membershipStart;}
	public long getMembershipEnd() {return membershipEnd;}
	public long getOrganisation() {return organisation;}
}
