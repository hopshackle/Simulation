package hopshackle.simulation.basic;
import hopshackle.simulation.*;

import java.util.concurrent.atomic.AtomicLong;

public class Marriage implements Persistent {

	private static AgentWriter<Marriage> marriageWriter = new AgentWriter<Marriage>(new MarriageDAO());
	private BasicAgent seniorPartner, juniorPartner;
	private long startDate, dissolutionDate;
	private World world;
	private long uniqueId;
	private static AtomicLong idFountain = new AtomicLong(1);

	public Marriage(BasicAgent partner1, BasicAgent partner2) {
		if (partner1.isMale() == partner2.isMale()) {
			throw new AssertionError("Marriage must be between opposite genders");
		}
		if (partner1.isMale()) {
			seniorPartner = partner1;
			juniorPartner = partner2;
		} else {
			seniorPartner = partner2;
			juniorPartner = partner1;
		}

		partner1.setMarriage(this);
		partner2.setMarriage(this);
		
		world = seniorPartner.getWorld();
		startDate = world.getCurrentTime();
		uniqueId = idFountain.getAndIncrement();
	}

	public BasicAgent getPartnerOf(BasicAgent basicAgent) {
		if (basicAgent == seniorPartner) return juniorPartner;
		if (basicAgent == juniorPartner) return seniorPartner;
		return null;
	}

	public void dissolve() {
		seniorPartner.setMarriage(null);
		juniorPartner.setMarriage(null);
		dissolutionDate = world.getCurrentTime();
		
		marriageWriter.write(this, getWorld().toString());
		
		juniorPartner.updatePlan();
		seniorPartner.updatePlan();
		
		this.seniorPartner = null;
		this.juniorPartner = null;
		this.world = null;
	}

	public BasicAgent getSeniorPartner() {
		return seniorPartner;
	}

	@Override
	public World getWorld() {
		return world;
	}

	public long getStartDate() {
		return startDate;
	}
	public long getDuration() {
		if (dissolutionDate == 0) {
			return world.getCurrentTime() - startDate;
		} else {
			return dissolutionDate - startDate;
		}
	}
	public long getId() {
		return uniqueId;
	}
}
