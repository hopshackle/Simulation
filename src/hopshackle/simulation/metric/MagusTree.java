package hopshackle.simulation.metric;

import hopshackle.simulation.*;

import java.io.*;
import java.sql.*;
import java.util.*;

public class MagusTree {

	private FileWriter output;
	private String dbStem;
	private static String newline = System.getProperty("line.separator");
	private Connection conn;
	protected static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	private int lastRecordedYear;

	public MagusTree(Connection conn, String dbStem, File outputFile) {
		try {
			output = new FileWriter(outputFile);
			this.dbStem = dbStem;
			this.conn = conn;

			output.write("digraph G {" + newline);

			lastRecordedYear = getLastRecordedYear();
			for (int i = 600; i <= lastRecordedYear; i+=25) {
				output.write("Y" +i + ";" + newline);
				if (i != 600)
					output.write("Y" + (i-25) + " -> Y" + i + ";" + newline);
			}

			output.write("	node [shape=record]; " + newline);
			getData();

			closeFile();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int getLastRecordedYear() {
		int retValue = 0;
		String dbQuery = "SELECT max(currentYear) as lastRecordedYear FROM Magi_" + dbStem;
		Statement s1;
		try {
			s1 = conn.createStatement();
			ResultSet rs = s1.executeQuery(dbQuery);
			if (rs.first()) {
				retValue = rs.getInt(1);
			}
			rs.close();
			s1.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retValue;
	}

	public void getData() {	
		String dbQuery = "SELECT m.id, m.parens, m.apprenticeIDs, m.name, m.covenant, m.tribunal, m.currentYear, m.yearsInTwilight, m.age, m.apparentAge " +
				"FROM Magi_"  + dbStem + " as m, magibylastyear " +
				"where m.id = magibylastYear.id and m.currentYear = magibylastyear.lastyear " +
				"order by id;";
		Set<String> parensRelationships = new HashSet<String>();
		Statement s1;
		try {
			s1 = conn.createStatement();
			ResultSet rs = s1.executeQuery(dbQuery);
			rs.first();
			do {
				long id = rs.getLong("m.id");
				int lastYear = rs.getInt("m.currentYear");
				String name = rs.getString("m.name");
				String apprenti = rs.getString("m.apprenticeIds");
				int covenant = rs.getInt("m.covenant");
				String tribunal = rs.getString("m.tribunal");
				long parens = rs.getLong("m.parens");
				int age = rs.getInt("m.age");
				int apparentAge = rs.getInt("m.apparentAge");
				int inTwilight = rs.getInt("m.yearsInTwilight");

				String[] temp = apprenti.split(",");
				boolean isDead = lastYear < lastRecordedYear;
				String bold = "";
				String attributes = "";
				if (!isDead) {
					bold = ",style=bold";
					attributes = "\\nAge: " + age + " (" + apparentAge + ")";
					if (inTwilight > 0)
						attributes = attributes + " " + inTwilight + " years in Twilight";
				}

				String covenantName = "";
				if (covenant > 0)
					covenantName = "\\n" + getCovenantName(covenant);
				
				output.write(id + "[label=\"" + name + attributes + covenantName + "\\n" + tribunal + "\"" + bold + "];" + newline);
				int yearRank = (lastYear - age) / 25;
				yearRank *= 25;
				output.write("{ rank=same ; Y" + yearRank + " ; " + id + "; }" + newline);
				if (parens > 0) {
					String relationshipStr = String.format("%4d", parens) + String.format("%4d", id);
					if (!parensRelationships.contains(relationshipStr)) {
						output.write(parens + " -> " + id + ";" + newline);
						parensRelationships.add(relationshipStr);
					}
				}

				if (apprenti.length() > 0) {
					for (String apprentice : temp) {
						long apprenticeId = Long.valueOf(apprentice);
						output.write(id + " -> " + apprenticeId + ";" + newline);
						String relationshipStr = String.format("%4d", id) + String.format("%4d", apprenticeId);
						parensRelationships.add(relationshipStr);
					}
				}
			} while (rs.next());

			rs.close();
		} catch (SQLException e) {
			System.out.println(dbQuery);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getCovenantName(int covenant) {
		String retValue = "";
		String dbQuery = "SELECT DISTINCT(name) FROM Covenants_" + dbStem + " WHERE id = " + covenant + ";";
		Statement s1;
		try {
			s1 = conn.createStatement();
			ResultSet rs = s1.executeQuery(dbQuery);
			if (rs.first()) {
				retValue = rs.getString(1);
			}
			rs.close();
			s1.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retValue;
	}

	public void closeFile() {
		try {
			output.write("}" + newline);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		new MagusTree(ConnectionFactory.getConnection(), "AM1", new File(baseDir + "\\Magi.txt"));
	}

}
