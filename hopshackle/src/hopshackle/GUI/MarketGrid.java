package hopshackle.GUI;

import hopshackle.simulation.Artefact;

import java.awt.event.*;
import java.util.TimerTask;

import javax.swing.*;

public class MarketGrid extends JTable {
	
	private MarketTableModel marketModel;
	
	public MarketGrid(MarketTableModel tm) {
		super(tm);
		marketModel = tm;

		this.addMouseListener(new MarketModelListener(this));
		java.util.Timer tableUpdate = new java.util.Timer(true);
		TimerTask task = new TimerTask() 
		{ public void run() {		
			marketModel.refreshTable();
			marketModel.fireTableDataChanged();}
		};
		tableUpdate.scheduleAtFixedRate(task, (long)100, (long)2000);
	}
	

	class MarketModelListener implements MouseListener {

		private JTable table;
		private MarketTableModel marketModel;
		MarketModelListener(JTable t) {
			table = t;
			marketModel = (MarketTableModel) table.getModel();
		}
		public void mouseClicked(MouseEvent e) {
			int rowSelected = table.getSelectedRow();
			if (rowSelected == -1) return;
			Artefact a = marketModel.getArtefactAt(rowSelected);
			MarketViewer mView = new MarketViewer(a, marketModel.getMarket());
			
			JFrame mainWindow = new JFrame();
			mainWindow.getContentPane().add(mView);
			mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			mainWindow.pack();
			mainWindow.setSize(500, 200);
			mainWindow.setVisible(true);
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		
	}
}

