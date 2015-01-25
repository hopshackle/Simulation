package hopshackle.GUI;

import hopshackle.simulation.*;

import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;

import javax.swing.*;

import net.sourceforge.chart2d.*;

public class PopulationGraph extends JPanel{

	private Connection conn;
	private String suffix;
	private LBChart2D populationChart;
	private String[] legendLabels = {"FTR", "CLR", "EXP"};
	private int maxMinutes;

	/*
	 * Works on the assumption that the World will be set for 
	 */
	public PopulationGraph(World w, long lifespan) {
		suffix = w.toString();
		conn = ConnectionFactory.getConnection();
		maxMinutes = (int) (lifespan / 60000);
		setUpDisplay();
	}

	private void setUpDisplay() {
		JButton refreshButton = new JButton("Refresh Graph");
		refreshButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshGraph();
			}
		});
		this.add(refreshButton);
	}
	
	private void refreshGraph() {
		if (populationChart != null)
			this.remove(populationChart);
		Dataset dataset = new Dataset(legendLabels.length, maxMinutes, 1);
		updateDataset(dataset);
		populationChart = GraphHelper.makeStandardGraph("Population", "Population", dataset, legendLabels);

		this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.add(populationChart);
		repaint();
	}

	private void updateDataset(Dataset dataset) {
		int set = 0;
		for (String classCode : legendLabels) {
			String tableName = classCode + "_" + suffix + "_pop";
			ArrayList<Integer> population = getPopulationHistory(tableName);
			for (int loop = 0; loop < maxMinutes && population.size() > loop; loop++) 
				dataset.add(set, loop, 0, population.get(loop));

			set++;
		}
	}

	private ArrayList<Integer> getPopulationHistory(String tableName) {
		String sqlQuery = "SELECT period, population FROM " + tableName + ";";
		ArrayList<Integer> retValue = new ArrayList<Integer>(maxMinutes);

		Statement st;
		try {
			st = conn.createStatement();
			ResultSet results = st.executeQuery(sqlQuery);

			if (results.first()) 
				for ( ; !results.isAfterLast(); results.next()) {
					int period = results.getInt("period");
					int population = results.getInt("population");
					retValue.add(period-1, population);
				}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return retValue;
	}

}
