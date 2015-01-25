package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.genetics.TradeGeneticEnum;

import java.util.*;

import javax.swing.table.AbstractTableModel;

public class MarketTableModel extends AbstractTableModel {
	
	private String[] columnNames = {"Item",
			"Price [1]",
			"Price [10]",
			"Price [100]",
			"Price [300]",
            "Volume [1]",
            "Volume [10]",
            "Volume [100]",
            "Volume [300]",
            "Offer Liquidity",
            "Bid Liquidity"};

	private ArrayList<Artefact> sortOrder;
    private Hashtable<Artefact, ArrayList<Double>> itemList;
    private Market m;
    private int refreshCount = 0;
    
    public MarketTableModel(Market m)  {
    	this.m = m;
    	itemList = new Hashtable<Artefact, ArrayList<Double>>();
    }

    public void refreshTable() {
    	refreshCount++;
    	List<Artefact> temp = m.getItems();
    	for (Artefact a : temp)
    		if (!itemList.containsKey(a)) {
    			ArrayList<Double> t = new ArrayList<Double>();
    			for (int n = 0; n<4; n++)
    				t.add(0.0);
    			itemList.put(a, t);
    		}

    	sortOrder = new ArrayList<Artefact>();
    	for (Artefact a : itemList.keySet())
    		sortOrder.add(a);
    	
    	Collections.sort(sortOrder, new Comparator<Artefact>() {
				public int compare(Artefact a1, Artefact a2) {
					// Based on volume sold (descending order of volume)
					double difference = itemList.get(a2).get(2) - itemList.get(a1).get(2);
					if (difference < 0.00) return -1;
					return 1;
				}
    	});


    	if (refreshCount%10 == 0) {
    		for (Artefact a : itemList.keySet()) {
    			ArrayList<Double> t = itemList.get(a);
    			int maxSequence = m.getSequenceSize(a);
    			t.set(0, m.getAveragePrice(a, Math.min(100, maxSequence)));
    			t.set(2, (double)m.getSalesVolume(a, Math.min(100, maxSequence))/Math.min(100, maxSequence));
    		}
    	}
    	if (refreshCount%30 == 0) {
    		for (Artefact a : itemList.keySet()) {
    			ArrayList<Double> t = itemList.get(a);
    			int maxSequence = m.getSequenceSize(a);
    			t.set(1, m.getAveragePrice(a, Math.min(300, maxSequence)));
    			t.set(3, (double)m.getSalesVolume(a, Math.min(300, maxSequence))/Math.min(300, maxSequence));
    		}
    	}

    }

    public int getColumnCount() {
    	return columnNames.length;
    }

    public int getRowCount() {
        return itemList.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
    	Artefact a = getArtefactAt(row);
    	int temp;
    	switch (col) {
    	case 0:
    		return a.toString();
    	case 1:
    		return String.format("%.2f", m.getAveragePrice(a, 1));
    	case 2:
    		return String.format("%.1f", m.getAveragePrice(a, 10));
    	case 3:
    		return String.format("%.1f", itemList.get(a).get(0));
    	case 4:
    		return String.format("%.1f", itemList.get(a).get(1));
    	case 5: 
    		return String.format("%d", m.getSalesVolume(a, 1));
    	case 6:
    		temp = Math.min(10, m.getSequenceSize(a));
    		return String.format("%.1f", m.getSalesVolume(a, temp)/(double)temp);
    	case 7:
    		return String.format("%.1f", itemList.get(a).get(2));
    	case 8:
    		return String.format("%.1f", itemList.get(a).get(3));
    	case 9:
    		return String.format("%d%%", (int)(100*TradeGeneticEnum.OFFER_LIQUIDITY.getValueAtMarket(m, a)));
    	case 10:
    		return String.format("%d%%", (int)(100*TradeGeneticEnum.BID_LIQUIDITY.getValueAtMarket(m, a)));
    	}
    	return null;
    }
    
    public Market getMarket() {return m;}

    public Class getColumnClass(int c) {
    	switch (c) {
    	default:
    		return String.class;
    	}
    }

	public Artefact getArtefactAt(int rowSelected) {
		return sortOrder.get(rowSelected);
	}
}
