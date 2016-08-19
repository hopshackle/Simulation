package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class BasicAgentRetriever implements AgentRetriever<BasicAgent> {

	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	private Connection con;

	public BasicAgentRetriever() {
		openConnection();
	}
	
	public BasicAgentRetriever(Connection con) {
		this.con = con;
	}
	
	public void closeConnection() {
		if (con != null)
			try {
				con.close();
			} catch (SQLException e) {
				logger.severe(e.toString());
				e.printStackTrace();
			}
	}

	@Override
	public BasicAgent getAgent(long uniqueID, String tableSuffix, World world) {

		BasicAgent retValue = null;
		try {
			String agentTable = "AllAgents_" + tableSuffix;
			Statement st = con.createStatement();

			ResultSet rs;
			rs = st.executeQuery("SELECT * FROM " + agentTable + " WHERE id = " + uniqueID + ";");
			if (!rs.first()) return null;
			long birth = rs.getLong("birth");
			long age = rs.getLong("age");
			long death = birth + age;
			int generation = rs.getInt("generation");
			long father = rs.getLong("father");
			long mother = rs.getLong("mother");
			String childrenIds = rs.getString("childrenIds");
			String[] childIdArray = childrenIds.split(",");
			List<Long> childrenList = new ArrayList<Long>();
			for (String childID : childIdArray) {
				try {
				childrenList.add(Long.valueOf(childID));
				} catch (NumberFormatException e) {
					// just move on
				}
			}
			
			retValue = new BasicAgent(world, uniqueID, father, mother, childrenList);
			retValue.setGeneration(generation);
			retValue.setBirth(birth);
			retValue.setDeath(death);

			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.severe(e.toString());
		}
		
		return retValue;
	}

	@Override
	public void openConnection() {
		closeConnection();
		con = ConnectionFactory.getConnection();
	}

}
