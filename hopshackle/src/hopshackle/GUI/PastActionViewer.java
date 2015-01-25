package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.Action;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;


public class PastActionViewer extends JPanel implements Observer {
	
	private Agent c;
	private JTable pastActionTable;
	private PastActionTableModel dtm;
	
	public PastActionViewer(Agent c) {
		this.c =c;
		dtm = new PastActionTableModel(c);
		pastActionTable = new JTable(dtm);
		pastActionTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		
		JScrollPane tablePane = new JScrollPane(pastActionTable);
		pastActionTable.setPreferredScrollableViewportSize(new Dimension (200, 200));
		
		add(tablePane, BorderLayout.SOUTH);
		pastActionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.setBorder(BorderFactory.createEtchedBorder());
		c.addObserver(this);
	}
	
	public void refresh() {
		dtm.fireTableDataChanged();
	}
	
	class PastActionTableModel extends AbstractTableModel {
			
			private String[] columnNames = {
	                "Action Name",
	                "Action Number",
	                "Total %age"};
			
			private List<Action> actionNames;
		    
		    public PastActionTableModel(Agent c)  {
		    	actionNames = c.getExecutedActions();
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
		    	row = actionNames.size() - row - 1;
		    	switch (col) {
		    	case 0:
		    		return actionNames.get(row).toString();
		    	case 1:
		    		return row;
		    	case 2: 
		    		return 0.0;
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
