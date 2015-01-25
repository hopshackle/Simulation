package hopshackle.GUI;

import hopshackle.simulation.dnd.Character;

import java.awt.Dimension;
import java.util.*;

import javax.swing.*;

public class GenomePanel extends JScrollPane {
	private Character c;
	public static String newline = System.getProperty("line.separator");
	
	public GenomePanel(Character person){
		c = person;
		
		JTextArea genomeView = new JTextArea();
		
		List<String> genomeStr = c.getGenome().getAllGenesAsString();
		for (String s : genomeStr) {
			genomeView.append(s + newline);
		}
		setViewportView(genomeView);
		this.setPreferredSize(new Dimension(200, 300));
	}
}
