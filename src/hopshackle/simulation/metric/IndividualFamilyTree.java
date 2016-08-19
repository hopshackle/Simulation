package hopshackle.simulation.metric;

import hopshackle.simulation.*;

import java.io.File;
import java.sql.*;
import java.util.*;

public class IndividualFamilyTree {

	private long individual;
	private int links;
	private String dbStem;
	private Set<Long> trackedIds;
	private Set<Long> trackedMarriages;
	private HashMap<Long, MarriageList> mapIndividualsToMarriages;
	private boolean descendantsOnly;
	Connection conn = ConnectionFactory.getConnection();

	public IndividualFamilyTree(String db, long id, int steps, boolean descendantsOnly) {
		individual = id;
		links = steps;
		dbStem = db;
		this.descendantsOnly = descendantsOnly;
		String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
		File outputFile = new File(baseDir + "\\"+dbStem+"_"+individual+"_"+links+"_Tree.txt");
		FamilyTreeOutputFile output = new FamilyTreeOutputFile(conn, dbStem, outputFile);

		trackedIds = new TreeSet<Long>();
		trackedMarriages = new TreeSet<Long>();
		mapIndividualsToMarriages = new HashMap<Long, MarriageList>();
		List<Long> newIds = new ArrayList<Long>();
		newIds.add(individual);

		List<Long> newIdsToTrack = new ArrayList<Long>();
		for (int loop = 0; loop < links; loop++) {
			for (long n : newIds) {
				long nextId = n;
				if (!trackedIds.contains(nextId)) {
					trackedIds.add(nextId);
					newIdsToTrack.addAll(getNewIds(nextId));
					trackAllMarriagesForIndividual(nextId);
				}
			}
			newIds = HopshackleUtilities.cloneList(newIdsToTrack);
			newIdsToTrack.clear();
		}

		for (long marriageID : trackedMarriages) {
			output.writeMarriage(marriageID);
		}
		for (long nextId : trackedIds) {
			MarriageList mList = mapIndividualsToMarriages.get(nextId);
			if (mList != null) {
				output.writeIndividual(nextId, mList.parentsMarriage, mList.ownMarriages);
			} else 
				output.writeIndividual(nextId, 0L, new ArrayList<Long>());
		}
		output.closeFile();
	}


	private void trackAllMarriagesForIndividual(long individual) {
		long parentsMarriage = 0;
		ArrayList<Long> ownMarriages = new ArrayList<Long>();
		String dbQuery = "SELECT Father, Mother FROM AllAgents_" + dbStem + " WHERE id = " + individual + ";";
		try {
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(dbQuery);
			if(rs.first()) {
				long father = rs.getLong("Father");
				long mother = rs.getLong("Mother");
				rs.close();
				dbQuery = "SELECT id from AllMarriages_" + dbStem + " WHERE husband = " + father + " AND wife = " + mother + ";";
				rs = s.executeQuery(dbQuery);
				if (rs.first()) {
					parentsMarriage = rs.getLong("id");
				}
			} else {
				// no parents found
			}
			rs.close();

			dbQuery = "SELECT id from AllMarriages_" + dbStem + " WHERE husband = " + individual + " OR wife = " + individual + ";";
			rs = s.executeQuery(dbQuery);
			if (rs.first()) {
				do {
					ownMarriages.add(rs.getLong("id"));
					rs.next();
				} while (!rs.isAfterLast());
			}
			rs.close();
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		MarriageList mList = new MarriageList(individual, parentsMarriage, ownMarriages);
		mapIndividualsToMarriages.put(individual, mList);
		trackedMarriages.add(parentsMarriage);
		trackedMarriages.addAll(ownMarriages);
	}


	private List<Long> getNewIds(long nextId) {
		List<Long> newIds = new ArrayList<Long>();
		String dbQuery = "SELECT Father, Mother, childrenIds FROM AllAgents_" + dbStem + " WHERE id = " + nextId + ";";
		try {
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(dbQuery);
			if(rs.first()) {
				long parent1 = rs.getLong("Father");
				long parent2 = rs.getLong("Mother");
				String childIDs = rs.getString("childrenIds");
				String[] children = childIDs.split(",");

				rs.close();
				s.close();

				if (!descendantsOnly) {
					if (parent1 > 0) newIds.add(parent1);
					if (parent2 > 0) newIds.add(parent2);
				}
				for (String child : children) {
					if (!child.equals(""))
						newIds.add(Long.valueOf(child));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return newIds;

	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2)
			throw new AssertionError("Must have three arguments for dbStem, individualID and number of Links to navigate.");

		boolean descendantsOnly = true;
		String db = args[0];
		long id = Long.valueOf(args[1]);
		int steps = Integer.valueOf(args[2]);
		if (steps < 0) {
			steps = -steps;
			descendantsOnly = false;
		}

		new IndividualFamilyTree(db, id, steps, descendantsOnly);
	}

}

class MarriageList {
	long individualId;
	long parentsMarriage;
	List<Long> ownMarriages;

	public MarriageList(long individual, long parentsMarriage, List<Long> ownMarriages) {
		individualId = individual;
		this.parentsMarriage = parentsMarriage;
		this.ownMarriages = HopshackleUtilities.cloneList(ownMarriages);
	}
}
