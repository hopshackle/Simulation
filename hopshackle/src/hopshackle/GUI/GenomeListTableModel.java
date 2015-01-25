package hopshackle.GUI;

import hopshackle.simulation.Genome;
import hopshackle.simulation.dnd.CharacterClass;

import java.io.*;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class GenomeListTableModel extends AbstractTableModel implements Serializable {

	private String[] columnNames = {"Class", "File name", "Number"};
	private Class[] classNames = {CharacterClass.class, File.class, Integer.class};
	
	private ArrayList<CharacterClass> classList;
	private ArrayList<File> fileList;
	private transient ArrayList<Genome> genomeList;
	private ArrayList<Integer> numberList;
	
	public GenomeListTableModel() {
		classList = new ArrayList<CharacterClass>();
		fileList = new ArrayList<File>();
		numberList = new ArrayList<Integer>();
		genomeList = new ArrayList<Genome>();
	}
	
	public void addEntry(CharacterClass chrClass, File file, int number) {
		if (!fileList.contains(file)) {
			Genome g = new Genome(file);
			classList.add(chrClass);
			fileList.add(file);
			numberList.add(number);
			genomeList.add(g);
		}
		fireTableDataChanged();
	}
	
	public void removeEntry(int index) {
		if (index < fileList.size()){
			classList.remove(index);
			fileList.remove(index);
			numberList.remove(index);
		}
		fireTableDataChanged();
	}
	

	public Class<?> getColumnClass(int arg0) {
		return classNames[arg0];
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int arg0) {
		return columnNames[arg0];
	}

	public int getRowCount() {
		return fileList.size();
	}

	public Object getValueAt(int row, int col) {
    	switch (col) {
    	case 0:
    		return classList.get(row);
    	case 1:
    		return fileList.get(row);
    	case 2:
    		return numberList.get(row);
    	}
    	return null;
	}

	public ArrayList<Genome> getGenomeList() {
		return genomeList;
	}

	public ArrayList<CharacterClass> getClassList() {
		return classList;
	}

	public ArrayList<Integer> getNumberList() {
		return numberList;
	}
	
	private void writeObject(ObjectOutputStream oos) throws IOException {
		// Nothign to do here - the Genome List is transient
		oos.defaultWriteObject();
	}
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		genomeList = new ArrayList<Genome>();
		for (File f : fileList) {
			// For each file, read in the Genome. Don't need to worry about Errors.
			// this simply leaves an empty Genome (which is stil valid)
			Genome g = new Genome(f);
			genomeList.add(g);
		}
	}
}
