package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

import java.awt.event.*;
import java.util.*;
/*
 *  This is for DND specific maintenance
 *  
 *  It must be the child location of a World
 */
public class DNDWorld extends Location {

	private HashMap<CharacterClass, Double> chrClassPercentage;
	private static HashMap<World, DNDWorld> universalMap = new HashMap<World, DNDWorld>();

	private DNDWorld(World world) {
		super(world);
		this.maintenance();
	}

	public synchronized double getPercentagePop(CharacterClass chrClass) {
		if (chrClassPercentage.containsKey(chrClass)) 
			return chrClassPercentage.get(chrClass);
		else 
			return 0.0;

	}

	public synchronized void maintenance() {
		chrClassPercentage = new HashMap<CharacterClass, Double>();

		for (CharacterClass cc : EnumSet.allOf(CharacterClass.class)) 
			chrClassPercentage.put(cc, getPercentage(cc));
	}

	private double getPercentage(CharacterClass chrClass){
		World w = (World)this.getParentLocation();

		List<Agent> allAgents = w.getAgents();

		int count =0;
		for (Agent a : allAgents) {
			if (a instanceof Character)
				if (((Character)a).getChrClass()==chrClass)
					count++;
		}

		if (allAgents.size() == 0) return 0.0;
		return ((double)count) / ((double)allAgents.size());
	}

	public static DNDWorld getDNDWorld(World world) {
		if (universalMap.containsKey(world))
			return universalMap.get(world);

		DNDWorld retValue = new DNDWorld(world);
		universalMap.put(world, retValue);
		world.addListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("Death")) {
					universalMap.remove((World)arg0.getSource());
				} else 
					logger.info("Unknown event type: " + arg0.getActionCommand());
			}
		});
		
		return retValue;
	}
}
