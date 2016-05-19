package hopshackle.GUI;

import hopshackle.simulation.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;


public class DeciderViewer extends JPanel implements Observer {
	
	private Agent c;
	private JTable decisionTable;
	private DeciderTableModel dtm;
	
	public DeciderViewer(Agent c) {
		this.c =c;
		dtm = new DeciderTableModel(c);
		decisionTable = new JTable(dtm);
		decisionTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		
		JScrollPane tablePane = new JScrollPane(decisionTable);
		decisionTable.setPreferredScrollableViewportSize(new Dimension(200, 200));
		
		add(tablePane, BorderLayout.SOUTH);
		decisionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.setBorder(BorderFactory.createEtchedBorder());
		dtm.makeDecision();
		c.addObserver(this);
	}
	
	public void refresh() {
		dtm.makeDecision();
		dtm.fireTableDataChanged();
	}
	
	class DeciderTableModel extends AbstractTableModel {
			
			private String[] columnNames = {
	                "Action Name",
	                "%age chance"};
			
			private Agent a;
			private List<? extends ActionEnum> actionNames;
			private HashMap<ActionEnum, Integer> choices = new HashMap<ActionEnum, Integer>();
		    
		    public DeciderTableModel(Agent c)  {
		    	a = c;
		    	actionNames = a.getDecider().getActions();
		    }

		    public void makeDecision() {
		    	Decider d = a.getDecider();
		    	Genome g = a.getGenome();
		    	if (g == null) return;
		    	actionNames = a.getDecider().getActions();
		    	
		    	ActionEnum choice;
		    	choices.clear();
		    	for (int n = 0; n < 100; n++) {
		    		choice = d.decideWithoutLearning(a, a);
		    		Integer currentValue = choices.get(choice);
		    		if (currentValue == null) currentValue = new Integer(0);
		    		choices.put(choice, currentValue + 1);
		    	}
		    }
		    
		    public int getColumnCount() {
		        return columnNames.length;
		    }

		    public int getRowCount() {
		    	return actionNames.size();
		    }

		    public String getColumnName(int col) {
		        return columnNames[col];
		    }

		    public Object getValueAt(int row, int col) {
		    	switch (col) {
		    	case 0:
		    		return actionNames.get(row);
		    	case 1:
		    		Integer retValue = choices.get(actionNames.get(row));
		    		if (retValue == null) retValue = new Integer(0);
		    		return String.format("%d%%", retValue.intValue());
		    	}
		    	return null;
		    }

		    public Class getColumnClass(int c) {
		        return getValueAt(0, c).getClass();
		    }

		}

	public void update(Observable arg0, Object arg1) {
		refresh();
	}
}
