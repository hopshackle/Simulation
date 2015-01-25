package hopshackle.GUI;

import hopshackle.simulation.Artefact;
import hopshackle.simulation.dnd.Character;

import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class InventoryPanel extends JScrollPane {

	// A Pane to show a characters inventory
	// Consists of a JTable on a Scroll Pane
	private Character c;
	private Hashtable<Artefact, Integer> inventory;
	JTable inventoryJTable;
	
	public InventoryPanel(Character person){
		super();
		c = person;
		
		refreshInventory();

		// We now have a hashtable containing all items
		
		inventoryJTable = new JTable(new ArtefactTableModel(inventory));
		setViewportView(inventoryJTable);
	}
	
	public void refresh() {
		refreshInventory();
		ArtefactTableModel tableModel = (ArtefactTableModel)inventoryJTable.getModel();
		tableModel.updateInventory(inventory);
	}
	public void refreshInventory() {
		inventory = new Hashtable<Artefact, Integer>();
		List<Artefact> itemList = c.getInventory();
		Integer temp;
		for (Artefact item : itemList) {
			temp = inventory.get(item);
			if (temp == null) {
				inventory.put(item, 1);
			} else {
				inventory.put(item, temp+1);
			}
		}
	}
	
	public class ArtefactTableModel extends AbstractTableModel {

		private Hashtable<Artefact,Integer> itemTable;
		private Object[] itemList;
		private String[] columnNames = {"Item",
	            "Number"};
		
		public ArtefactTableModel(Hashtable<Artefact, Integer> itemTable) {
			super();
			this.itemTable = itemTable;
			itemList = itemTable.keySet().toArray();
		}
		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return itemTable.keySet().size();
		}

		public Object getValueAt(int row, int column) {
			switch (column) {
			case 0:
				return (Artefact) itemList[row];
			case 1:
				return itemTable.get((Artefact)itemList[row]);
			}
			return null;
		}

	    public String getColumnName(int col) {
	        return columnNames[col];
	    }
	    
	    public void updateInventory(Hashtable<Artefact,Integer> newInventory) {
	    	itemTable = newInventory;
			itemList = itemTable.keySet().toArray();
			fireTableDataChanged();
	    }
	}
}
