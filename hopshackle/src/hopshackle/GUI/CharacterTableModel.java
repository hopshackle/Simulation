package hopshackle.GUI;

import hopshackle.simulation.Agent;
import hopshackle.simulation.dnd.Character;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class CharacterTableModel extends AbstractTableModel {
	
	private String[] columnNames = {"Name",
            "Race",
            "Class",
            "Level",
            "XP",
            "hp",
            "Max hp",
            "AC",
            "Str",
            "Dex",
            "Con",
            "Int",
            "Wis",
            "Cha",
            "Square",
            "Generation",
            "Party",
            "Leader",
            "Gold",
            "Reputation",
            "Age"};

    private ArrayList<Character> data;
    
    public CharacterTableModel()  {
    	data = new ArrayList<Character>();
    }
    
    public void addCharacter(Character c) {
    	data.add(c);
    }
    public Agent getCharacterAt(int row) {
    	return data.get(row);
    }
    
    public void removeCharacter(Agent c) {
    	data.remove(c);
    }
    public void clearTable() {
		data = new ArrayList<Character>();
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
    	if (data.size() == 0) return null;
    	Character c = data.get(row);
    	if (c == null) {
    		switch (col) {
    		case 17:
    			return false;
    		case 0:
    		case 1:
    		case 2:
    		case 14:
    		case 16:
    			return "NULL";
    		default:
    			return 0;
    		}
    	}
    	switch (col) {
    	case 0:
    		return c.getName();
    	case 1:
    		return c.getRace();
    	case 2:
    		return c.getChrClass();
    	case 3:
    		return c.getLevel();
    	case 4:
    		return c.getXp();
    	case 5:
    		return c.getHp();
    	case 6:
    		return c.getMaxHp();
    	case 7:
    		return c.getAC();
    	case 8:
    		return c.getStrength().getValue();
    	case 9:
    		return c.getDexterity().getValue();
    	case 10:
    		return c.getConstitution().getValue();
    	case 11:
    		return c.getIntelligence().getValue();
    	case 12:
    		return c.getWisdom().getValue();
    	case 13:
    		return c.getCharisma().getValue();
    	case 14:
    		if (c.getLocation() != null) {
    			return c.getLocation().toString();
    		} else return "Unknown";
    	case 15:
    		return c.getGeneration();
    	case 16:
    		if (c.getParty() == null) return null;
    		String retResult = c.getParty().toString();
    		return Integer.valueOf(retResult);
    	case 17:
    		if (c.getParty() != null) {
    			Character leader = c.getParty().getLeader();
    			if (leader != null && leader.equals(c)) 
    				return true;
    		}
    		return false;
    	case 18:
    		return (int)c.getGold();
    	case 19:
    		return c.getReputation();
    	case 20:
    		return c.getAge()/1000;
    	}
    	return null;
    }

    public Class getColumnClass(int c) {
		switch (c) {
		case 17:
			return Boolean.class;
		case 0:
		case 1:
		case 2:
		case 14:
			return String.class;
		default:
			return Integer.class;
		}
    }

}