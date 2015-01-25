package hopshackle.GUI;

import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class PartyTableModel extends AbstractTableModel {
	
	private String[] columnNames = {"Name",
			"Leader",
            "Number",
            "Fighters",
            "Clerics",
            "Wound",
            "Avg Level",
            "Std Dev",
            "Location",
            "Reputation",
            "Age"};

    private ArrayList<Party> data;
    
    public PartyTableModel()  {
    	data = new ArrayList<Party>();
    }
    
    public void addParty(Party p) {
    	data.add(p);
    }
    public Party getPartyAt(int row) {
    	return data.get(row);
    }
    
    public void removeParty(Party p) {
    	data.remove(p);
    }
    public void clearTable() {
		data = new ArrayList<Party>();
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
    	Party p = data.get(row);
    	switch (col) {
    	case 0:
    		return p.toString();
    	case 1:
    		Character leader = p.getLeader();
    		if (leader == null)
    			return "Null";
    		return leader.toString();
    	case 2:
    		return p.getMembers().size();
    	case 3:
    		return getNumberInClass(CharacterClass.FIGHTER, p);
    	case 4:
    		return getNumberInClass(CharacterClass.CLERIC, p);
    	case 5: 
    		return String.format("%.2f", p.getWound());
    	case 6:
    		return String.format("%.2f", p.getLevel());
    	case 7:
    		return String.format("%.2f",p.getLevelStdDev());
    	case 8:
    		if (p.getLocation() != null)
    			return p.getLocation().toString();
    		return "Null";
    	case 9:
    		return p.getReputation();
    	case 10:
    		return p.getAge()/1000;
    	}
    	return null;
    }

    public Class getColumnClass(int c) {
    	switch (c) {
    	case 0:
    	case 1:
    	case 5:
    	case 6:
    	case 7:
    	case 8:
    		return String.class;
    	default:
    		return Integer.class;
    	}
    }

	public int getNumberInClass(CharacterClass chrClass, DnDAgent group) {
		int classCount = 0;
		for (DnDAgent d : group.getMembers()) {
			Character c = (Character) d;
			if (c.getChrClass() == chrClass)
				classCount += 1;
		}
		return classCount;
	}
}
