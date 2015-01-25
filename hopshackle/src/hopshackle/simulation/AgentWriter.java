package hopshackle.simulation;

import java.awt.event.*;
import java.util.ArrayList;

public class AgentWriter<T extends Persistent> {

	private String lastSuffix;
	private StringBuffer buffer;
	private int numberInBuffer, bufferLimit;
	private ArrayList<World> worlds;
	private AgentDAO<T> agentDAO;

	public AgentWriter(AgentDAO<T> agentDAO) {
		this.agentDAO = agentDAO;
		numberInBuffer = 0;
		bufferLimit = 10;
		worlds = new ArrayList<World>();
	}

	public void write(T agent, String tableSuffix) {

		if (lastSuffix == null || !lastSuffix.equals(tableSuffix)) {
			lastSuffix = tableSuffix;
			updateWorldListeners(agent.getWorld());
			writeBuffer(agent.getWorld());

			String sqlDelete = agentDAO.getTableDeletionSQL(tableSuffix);
			agent.getWorld().updateDatabase(sqlDelete);
			String sqlQuery = agentDAO.getTableCreationSQL(tableSuffix);
			agent.getWorld().updateDatabase(sqlQuery);
		}

		addToBuffer(agent);
	}

	private void updateWorldListeners(World world) {
		if (!worlds.contains(world)) {
			world.addListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (arg0.getActionCommand().equals("Death")) {
						World dyingWorld = (World)arg0.getSource();
						worlds.remove(dyingWorld); // to prevent heap dumps if DAO keeps historic worlds from being garbage collected
						if (lastSuffix.equals(dyingWorld.toString())) {
							writeBuffer(dyingWorld);
							// only if this is the world we are currently writing characters for
						}
					}
				}
			});
		}
	}


	private void addToBuffer(T agent) {
		if (!buffer.substring(buffer.length()-6).equals("VALUES"))
			buffer.append(",");
		
		buffer.append(agentDAO.getValuesForAgent(agent));
		numberInBuffer++;
		if (numberInBuffer >= bufferLimit) 
			writeBuffer(agent.getWorld());
	}

	public void writeBuffer(World w) {
		// write if not null
		if (numberInBuffer > 0) {
			w.updateDatabase(buffer.toString());
		}

		// initialise new buffer
		buffer = new StringBuffer(agentDAO.getTableUpdateSQL(lastSuffix));

		numberInBuffer = 0;
	}

	public void setBufferLimit(int newLimit) {
		bufferLimit = newLimit;
	}
	public int getBufferLimit() {
		return bufferLimit;
	}

}
