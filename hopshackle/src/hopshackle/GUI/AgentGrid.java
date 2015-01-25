package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.swing.*;

public class AgentGrid extends JTable {
	
	private CharacterTableModel agentModel;
	private final Location world;
	private Party pFilter;

	public AgentGrid(Location l, CharacterTableModel tm, Party p) {
		super(tm);
		world = l;
		agentModel = tm;
		pFilter = p;
		this.addMouseListener(new AgentModelListener(this));
		java.util.Timer tableUpdate = new java.util.Timer(true);
		TimerTask task = new TimerTask() {
			public void run() {	
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							ArrayList<Agent> arrAg = new ArrayList<Agent>();
							agentModel.clearTable();
							List<Agent> tempArrAg = world.getAgents();
							for (Agent a : tempArrAg) {
							if (a instanceof Character) {
								if (pFilter != null) {
									if (pFilter.equals(((Character)a).getParty()))  {
										arrAg.add(a);
									}
								} else {
									arrAg.add(a);
								}
							}
						}
						Collections.sort(arrAg, new Comparator<Agent>() {
							public int compare(Agent a1, Agent a2) {
								Character c1 = (Character) a1;
								Character c2 = (Character) a2;
								return (int)(c2.getXp() - c1.getXp());
							}
						});
						if (arrAg.size() > 100) {
							ArrayList<Agent> arrTemp = new ArrayList<Agent>();
							for (int n = 0; n<100; n++) {
								arrTemp.add(arrAg.get(n));
							}
							arrAg = arrTemp;
						}
						int n = 0;
						for (Agent a : arrAg) {
							Character c = (Character) a;
							agentModel.addCharacter(c);
							n++;
						}
						agentModel.fireTableDataChanged();
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}	// End of Runnable
			}
		};
		tableUpdate.scheduleAtFixedRate(task, (long)100, (long)2000);
	}


	class AgentModelListener implements MouseListener {

		private JTable table;
		private CharacterTableModel agentModel;
		AgentModelListener(JTable t) {
			table = t;
			agentModel = (CharacterTableModel) table.getModel();
		}
		public void mouseClicked(MouseEvent e) {
			int rowSelected = table.getSelectedRow();
			if (rowSelected == -1) return;
			Agent c = agentModel.getCharacterAt(rowSelected);
			new AgentViewer(c);
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		
	}
}

