package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.Party;

import java.awt.event.*;
import java.util.*;

import javax.swing.JTable;

public class PartyGrid extends JTable {
	
	private PartyTableModel agentModel;
	private final Location world;
	
	public PartyGrid(Location l, PartyTableModel tm) {
		super(tm);
		world = l;
		agentModel = tm;
		this.addMouseListener(new AgentModelListener(this));
		java.util.Timer tableUpdate = new java.util.Timer(true);
		TimerTask task = new TimerTask() 
		{ @SuppressWarnings("unchecked")
		public void run() {		
			ArrayList<Agent> arrAg = new ArrayList<Agent>();
			agentModel.clearTable();
			List<Agent> tempArrAg = world.getAgents();
			for (Agent a : tempArrAg) {
				if (a instanceof Party) arrAg.add(a);
			}
			Collections.sort(arrAg, new Comparator<Agent>() {
				public int compare(Agent a1, Agent a2) {
					Party c1 = (Party) a1;
					Party c2 = (Party) a2;
					return (int)(c2.getAge() - c1.getAge());
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
				Party p = (Party) a;
				agentModel.addParty(p);
				n++;
			}
			agentModel.fireTableDataChanged();}
		};
		tableUpdate.scheduleAtFixedRate(task, (long)100, (long)2000);
	}
	
	 class AgentModelListener implements MouseListener {

		private JTable table;
		private PartyTableModel agentModel;
		AgentModelListener(JTable t) {
			table = t;
			agentModel = (PartyTableModel) table.getModel();
		}
		public void mouseClicked(MouseEvent e) {
			int rowSelected = table.getSelectedRow();
			if (rowSelected == -1) return;
			Agent c = agentModel.getPartyAt(rowSelected);
			new AgentViewer(c);
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		
	}
}

