package hopshackle.simulation;

public abstract class Policy<I> {
	
	public final String type;
	
	public Policy(String name) {
		this.type = name;
	}
	
	public void apply(I agent){}
	public void apply(I input, Agent agent) {}
	public double getValue(I input, Agent agent) {return 0.0;}
}
