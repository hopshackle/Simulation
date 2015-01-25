package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.awt.event.*;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;


public class AgentViewer extends JFrame {

	private Agent agent;
	private JTabbedPane decisionDetails;
	private JPanel pastActions, futureActions;
	private JComponent agentDetails;
	private JSplitPane mainPane;
	
	public AgentViewer(Agent a){
		agent = a;
		this.setTitle(a.toString());
		agentDetails = new JPanel();
		if (a instanceof Character) {
			agentDetails = new CharacterViewer((Character) a);
		}
		if (a instanceof Party) {
			agentDetails = new PartyViewer((Party) a);
		}

		decisionDetails = new JTabbedPane();
		pastActions = new PastActionViewer((Agent) a);
		futureActions = new DeciderViewer((Agent) a);
		pastActions.setName("Past choices");
		futureActions.setName("Current choice");
		decisionDetails.add(futureActions);
		decisionDetails.add(pastActions);
		
		mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPane.setTopComponent(agentDetails);
		mainPane.setBottomComponent(decisionDetails);
		this.getContentPane().add(mainPane);
		
		JMenuBar menubar = new JMenuBar();
		JMenu saveMenu = new JMenu("Save");
		
		setJMenuBar(menubar);
		menubar.add(saveMenu);
		JMenuItem saveGenome = new JMenuItem("Genome");
		saveMenu.add(saveGenome);
		
		saveGenome.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setDialogType(JFileChooser.SAVE_DIALOG);
	            int returnVal = fc.showSaveDialog(AgentViewer.this);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fc.getSelectedFile();
	                Genome g = agent.getGenome();
	                g.recordGenome(file);
	            }
			}	
		});
		
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.pack();
		this.setVisible(true);
	}
	
class GenomeTableModel extends AbstractTableModel {
		
		private String[] columnNames = {
                "GeneName",
                "%age chance"};
		
		private Agent a;
		private List<? extends ActionEnum> geneArray;
	    
	    public GenomeTableModel(Agent agent)  {
	    	a = agent;
	    	geneArray = a.getDecider().getActions();
	    }

	    public int getColumnCount() {
	        return columnNames.length;
	    }

	    public int getRowCount() {
	    	return geneArray.size();
	    }

	    public String getColumnName(int col) {
	        return columnNames[col];
	    }

	    public Object getValueAt(int row, int col) {
	    	switch (col) {
	    	case 0:
	    		return geneArray.get(row).toString();
	    	case 1:
	    		ActionEnum option = geneArray.get(row);
	    		double chance =  a.getDecider().valueOption(option, a, a);

	    		if (chance<0.0) chance = 0.0;
	    		if (chance>1.0)	chance =1.0;
	    		chance *= 100;
	    		return String.format("%2.0f%%", chance);
	    	}
	    	return null;
	    }

	    public Class getColumnClass(int c) {
	        return getValueAt(0, c).getClass();
	    }

	}
	
	
}
