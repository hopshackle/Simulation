package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class CharacterViewer extends JTabbedPane implements Observer {

	// A simple Panel that contains the details of a Character
	private Character c;
	JLabel strLabel, dexLabel, conLabel, intLabel, wisLabel, chaLabel, goldLabel;
	JTextField strField, dexField, conField, intField, wisField, chaField, goldField;
	
	JLabel levelLabel, hpLabel, maxHpLabel, ACLabel, CRLabel, ageLabel, locLabel,
			maxAgeLabel, nameLabel, idLabel, xpLabel, raceLabel, chrClassLabel,
			generationLabel, armourLabel, shieldLabel, weaponLabel;
	JLabel mpLabel, maxMpLabel;
	JTextField xpField, hpField, maxHpField, CRField, ageField, 
			maxAgeField, mpField, maxMpField, childrenField, repField;
	
	JPanel attributePanel, namePanel, combatPanel, locationPanel, mainPanel;
	
	JButton partyButton;
	InventoryPanel inventoryTab;
	GenomePanel genomeTab;
	
	public CharacterViewer(Character c) {
		this.c =c;
		setUpDisplay();
		c.addObserver(this);
	}
	
	private void setUpDisplay() {

		// Set up variable Text areas first
		
		xpField = new JTextField(8);
		ageField = new JTextField(8);
		hpField = new JTextField(4);
		mpField = new JTextField(4);
		maxHpField = new JTextField(4);
		maxHpField.setEditable(false);
		maxMpField = new JTextField(4);
		maxMpField.setEditable(false);
		CRField = new JTextField(3);
		locLabel = new JLabel();
		levelLabel = new JLabel();
		strField = new JTextField(3);
		dexField = new JTextField(3);
		conField = new JTextField(3);
		intField = new JTextField(3);
		wisField = new JTextField(3);
		chaField = new JTextField(3);
		armourLabel = new JLabel();
		shieldLabel =new JLabel();
		weaponLabel = new JLabel();
		ACLabel = new JLabel();
		goldField = new JTextField(6);
		childrenField = new JTextField(3);
		repField = new JTextField(4);
		
		
		ActionListener al = new CharacterViewerListener();
		xpField.addActionListener(al);
		hpField.addActionListener(al);
		CRField.addActionListener(al);
		ageField.addActionListener(al);
		strField.addActionListener(al);
		goldField.addActionListener(al);
		
		mainPanel = new JPanel(new BorderLayout(3,3));
		this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.addTab("Main", mainPanel);
		
		strLabel = new JLabel("STR: ");
		strLabel.setLabelFor(strField);
		dexLabel = new JLabel("DEX: ");
		dexLabel.setLabelFor(dexField);
		conLabel = new JLabel("CON: ");
		conLabel.setLabelFor(conField);
		intLabel = new JLabel("INT: ");
		intLabel.setLabelFor(intField);
		wisLabel = new JLabel("WIS: ");
		wisLabel.setLabelFor(wisField);
		chaLabel = new JLabel("CHA: ");
		chaLabel.setLabelFor(chaField);
		
		attributePanel = new JPanel(new SpringLayout());
		attributePanel.add(strLabel);
		attributePanel.add(strField);
		attributePanel.add(dexLabel);
		attributePanel.add(dexField);
		attributePanel.add(conLabel);
		attributePanel.add(conField);
		attributePanel.add(intLabel);
		attributePanel.add(intField);
		attributePanel.add(wisLabel);
		attributePanel.add(wisField);
		attributePanel.add(chaLabel);
		attributePanel.add(chaField);
		
        SpringUtilities.makeCompactGrid(attributePanel,
                6, 2, //rows, cols
                6, 6,        //initX, initY
                6, 6);       //xPad, yPad

        attributePanel.setBorder(BorderFactory.createEtchedBorder());
		mainPanel.add(attributePanel, BorderLayout.WEST);
        
		xpLabel = new JLabel("XP: ");
		goldLabel = new JLabel("Gold: ");
		
		maxAgeLabel = new JLabel("Max Age: ");
		
		namePanel = new JPanel(new SpringLayout());
		
        namePanel.add(new JLabel("Name: "));
        namePanel.add(new JLabel(c.getName()));
        namePanel.add(new JLabel("ID: "));
        namePanel.add(new JLabel(String.valueOf(c.getUniqueID())));
        namePanel.add(new JLabel("Generation: "));
        namePanel.add(new JLabel(String.valueOf(c.getGeneration())));
        namePanel.add(new JLabel("Race: "));
        namePanel.add(new JLabel(c.getRace().toString()));
        namePanel.add(new JLabel("Class: "));
        namePanel.add(new JLabel(c.getChrClass().toString()));
        namePanel.add(new JLabel("Level: "));
        namePanel.add(levelLabel);
        namePanel.add(maxAgeLabel);
        namePanel.add(new JLabel(String.valueOf(c.getMaxAge())));
        namePanel.add(new JLabel("Age: "));
        namePanel.add(ageField);
        namePanel.add(xpLabel);
        namePanel.add(xpField);
        namePanel.add(goldLabel);
        namePanel.add(goldField);
        namePanel.add(new JLabel("Children: "));
        namePanel.add(childrenField);
        namePanel.add(new JLabel("Reputation: "));
        namePanel.add(repField);
		
        SpringUtilities.makeCompactGrid(namePanel,
                6, 4, //rows, cols
                6, 6,        //initX, initY
                6, 6);       //xPad, yPad
        
        namePanel.setBorder(BorderFactory.createEtchedBorder());
        mainPanel.add(namePanel, BorderLayout.NORTH);
        
		hpLabel = new JLabel("HP: ");
		maxHpLabel = new JLabel("Max HP: ");
		mpLabel = new JLabel("MP: ");
		maxMpLabel = new JLabel("Max MP: ");
		
		combatPanel = new JPanel(new SpringLayout());
		combatPanel.add(maxHpLabel);
		combatPanel.add(maxHpField);
		combatPanel.add(hpLabel);
		combatPanel.add(hpField);
		combatPanel.add(maxMpLabel);
		combatPanel.add(maxMpField);
		combatPanel.add(mpLabel);
		combatPanel.add(mpField);
		combatPanel.add(ACLabel);
		combatPanel.add(new JLabel());
		combatPanel.add(weaponLabel);
		combatPanel.add(new JLabel());
		combatPanel.add(armourLabel);
		combatPanel.add(new JLabel());
		combatPanel.add(shieldLabel);
		combatPanel.add(new JLabel());
		
        SpringUtilities.makeCompactGrid(combatPanel,
                8, 2, //rows, cols
                6, 6,        //initX, initY
                6, 6);       //xPad, yPad
        
        combatPanel.setBorder(BorderFactory.createEtchedBorder());
		mainPanel.add(combatPanel, BorderLayout.EAST);
        
		CRLabel = new JLabel("CR of Location: ");

		locationPanel = new JPanel();
		locationPanel.add(CRLabel);
		locationPanel.add(CRField);
		locationPanel.add(locLabel);
		locationPanel.add(new JPanel());
		partyButton = new JButton("Party");
		locationPanel.add(partyButton);
		locationPanel.add(new JPanel());
		
		partyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// open window for the party
				Party p = c.getParty();
				if (p!=null)
					new AgentViewer(p);
			}
		});
		
        locationPanel.setBorder(BorderFactory.createEtchedBorder());
		mainPanel.add(locationPanel, BorderLayout.SOUTH);
		
		inventoryTab = new InventoryPanel(c);
		genomeTab = new GenomePanel(c);
		
		this.addTab("Inventory", inventoryTab);
		this.addTab("Genome", genomeTab);
		refresh();
	}
	
	public void refresh() {

		if (c.getLocation() != null) {
			locLabel.setText(c.getLocation().toString());
		} else {
			locLabel.setText("Limbo");
		}
		xpField.setText(String.valueOf(c.getXp()));
		goldField.setText(String.format("%.2f", c.getGold()));
		levelLabel.setText(String.valueOf(c.getLevel()));
		ageField.setText(String.valueOf(c.getAge()));
		hpField.setText(String.valueOf(c.getHp()));
		maxHpField.setText(String.valueOf(c.getMaxHp()));
		mpField.setText(String.valueOf(c.getMp()));
		maxMpField.setText(String.valueOf(c.getMaxMp()));
		if (c.getLocation() != null && c.getLocation() instanceof Square) {
			Square s = (Square)c.getLocation();
			CRField.setText(String.valueOf(s.getX() + s.getY()));
		} else {CRField.setText("NA");}
		
		strField.setText(c.getStrength().toString());
		dexField.setText(c.getDexterity().toString());
		conField.setText(c.getConstitution().toString());
		intField.setText(c.getIntelligence().toString());
		wisField.setText(c.getWisdom().toString());
		chaField.setText(c.getCharisma().toString());
		
		childrenField.setText(String.valueOf(c.getNumberOfChildren()));
		repField.setText(String.valueOf(c.getReputation()));
		
		if (c.getParty() != null && !c.getParty().isDead()) {
			partyButton.setEnabled(true);
		} else {
			partyButton.setEnabled(false);
		}
		armourLabel.setText("NONE");
		shieldLabel.setText("NONE");
		weaponLabel.setText("NONE");
		if (c.getArmour() != null) armourLabel.setText(c.getArmour().toString());
		if (c.getShield() != null) shieldLabel.setText(c.getShield().toString());
		if (c.getWeapon() != null) weaponLabel.setText(c.getWeapon().toString());
		ACLabel.setText("AC: " + c.getAC());
		
		if (c.isDead()) {
			namePanel.setBackground(Color.LIGHT_GRAY);
			ageField.setText(ageField.getText() + " Dead");
			partyButton.setEnabled(false);
		}

	}

	public void update(Observable arg0, Object arg1) {
		// we can assume that arg0 is c.
		if (c.isDead()) {
			c.deleteObserver(this);
		}
		refresh();
		repaint();
		inventoryTab.refresh();
	}

	public Agent getCharacter() {
		return c;
	}
	
	class CharacterViewerListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source.equals(xpField)) {
				long newXP = Long.valueOf(xpField.getText());
				c.addXp(newXP - c.getXp());
			} else if (source.equals(hpField)) {
				int newHP = Integer.valueOf(hpField.getText());
				c.addHp(newHP - c.getHp(), false);
			}  else if (source.equals(ageField)) {
				int newAge = Integer.valueOf(ageField.getText());
				c.addAge((newAge - c.getAge()));
			} else if (source.equals(strField)) {
				Attribute newStr = new Attribute(Integer.valueOf(strField.getText()));
				c.setStrength(newStr);
				c.addHp(0, false);
			} else if (source.equals(goldField)) {
				double change = Double.valueOf(goldField.getText())-c.getGold();
				c.addGold(change);
			}
			
		}
		
		
	}
	
}
