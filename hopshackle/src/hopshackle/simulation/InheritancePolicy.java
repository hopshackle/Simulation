package hopshackle.simulation;

public interface InheritancePolicy {
	
	public <T extends Agent> void bequeathEstate(T testator);

}
