package hopshackle.GUI;

import hopshackle.simulation.dnd.Party;

import java.util.*;

import javax.swing.*;

public class PartyViewer extends JScrollPane implements Observer {

	// A simple Panel that contains a grid of the Party Members
	private Party p;
	private AgentGrid partyMembers;
	
	JPanel attributePanel, namePanel, combatPanel, locationPanel, mainPanel;
	
	public PartyViewer(Party p) {
		this.p = p;
		setUpDisplay();
		p.addObserver(this);
	}
	
	private void setUpDisplay() {

		// Set up variable Text areas first

		partyMembers = new AgentGrid(p.getLocation().getParentLocation(), 
						new CharacterTableModel(),
						p);
	
		setViewportView(partyMembers);
		
	}

	public void update(Observable arg0, Object arg1) {
		// we can assume that arg0 is p.
		if (p.isDead()) {
			p.deleteObserver(this);
		}
		repaint();
	}
}
