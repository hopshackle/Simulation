package hopshackle.simulation.metric;

import java.io.*;
import java.sql.*;
import java.util.List;

public class FamilyTreeOutputFile {

	private FileWriter output;
	private String dbStem;
	private static String newline = System.getProperty("line.separator");
	private Connection conn;

	public FamilyTreeOutputFile(Connection conn, String dbStem, File outputFile) {
		try {
			output = new FileWriter(outputFile);
			this.dbStem = dbStem;
			this.conn = conn;

			output.write("digraph G {" + newline);
			output.write("	node [shape=record]; " + newline);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeMarriage (long id) {
		String dbQuery = "SELECT id, husband, wife FROM AllMarriages_" + dbStem + " WHERE id = " + id + ";";
		Statement s1;
		try {
			s1 = conn.createStatement();
			ResultSet rs = s1.executeQuery(dbQuery);

			if (rs.first()) {
				Long male = rs.getLong("husband");
				Long female = rs.getLong("wife");
				rs.close();
				String husband = getName(male, s1);
				String wife = getName(female, s1);
				output.write("family" + id + "[label=\"<male> " + husband + " |<m> |<female> " + wife + " \"];" + newline);
				//	family0[label="<male> Markus [3426] |<m> |<female> Anne [3855] "];
			}
		} catch (SQLException e) {
			System.out.println(dbQuery);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getName(Long uniqueId, Statement s) throws SQLException {
		return getIndividualData(uniqueId, s, "name");
	}
	private String getGender(Long uniqueId, Statement s) throws SQLException {
		return getIndividualData(uniqueId, s, "gender");
	}

	private String getIndividualData(Long uniqueId, Statement s, String columnName) throws SQLException {
		String name;
		String dbQuery = "SELECT " + columnName + " from AllAgents_" + dbStem + " WHERE id = " + uniqueId + ";";
		ResultSet rs2 = s.executeQuery(dbQuery);
		if (rs2.first()) {
			name = rs2.getString(columnName);
		} else {
			name = "Unknown";
		}
		rs2.close();
		return name;
	}

	public void writeIndividual(long id, long parentsMarriage, List<Long> ownMarriages) {
		int numberOfMarriages = ownMarriages.size();
		Statement s1;
		try {
			s1 = conn.createStatement();
			String name = getName(id, s1);
			String gender = getGender(id, s1); // M or F
			if (gender.equals("M")) {
				gender = "male";
			} else {
				gender = "female";
			}
			if (parentsMarriage > 0) {
				if (numberOfMarriages > 0) {
					output.write("\"family" + parentsMarriage + "\":m -> \"family" + ownMarriages.get(0) + "\":" + gender + "; " + newline);
					// "family0":m -> "family2":male;
				} else {
					output.write("\"family" + parentsMarriage + "\":m -> \"" + name + " \"; " + newline);
					// "family1":m -> "Hans-Dieter [3791]"
				}
			} 
			if (numberOfMarriages > 1) {
				long previousMarriage = ownMarriages.get(0);
				for (long marriageId : ownMarriages) {
					if (marriageId != previousMarriage) {
						output.write("\"family" + previousMarriage + "\":" + gender + " -> \"family" + marriageId + "\":" + gender + " [color=blue]; " + newline);
						// "family2":female -> "family3":female [color=blue];
						previousMarriage = marriageId;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeFile() {
		try {
			output.write("}" + newline);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
