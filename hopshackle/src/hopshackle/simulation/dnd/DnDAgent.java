package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

import java.util.ArrayList;

public abstract class DnDAgent extends Agent {
	
	private boolean stationary;
	
	public DnDAgent(World world) {
		super(world);
	}
	
	public abstract void surviveEncounter(int CR);
	
	public abstract void rest(int seconds);
	
	public void setStationary(boolean state) {stationary = state;}
	
	public boolean isStationary() {return stationary;}
	
	public abstract double getLevel();
	
	public abstract double getLevelStdDev();
	
	public abstract int getSize();
	
	public abstract ArrayList<Character> getMembers();
	
	public abstract double getWound();
	
	public abstract int getReputation();
	
	}
