package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Calendar;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.table.TableColumnModel;

public class RunDnDWorldGUI {

	private JFrame frame;
	private JTextField suffixField;
	private JButton startButton;
	private JComboBox classList;
	private GenomeListTableModel gltModel;
	private JTable genomeTable;
	private JTextField numberField;
	private JTextField endTimeField;
	private JTextField iterations;
	private JRadioButton showGUIButton;

	private File selectedFile;
	private JLabel fileLabel;
	private static Logger logger;
	private DatabaseAccessUtility databaseUtility;
	boolean loadStateSpace = "true".equals(SimProperties.getProperty("LoadStateSpaceAtStart", "false"));
	String stateSpaceToLoad = SimProperties.getProperty("StateSpaceToLoad", "");

	private static String baseDir = SimProperties.getProperty("BaseDirectory", "C:\\Simulations");
	private static File defaultDirectory = new File(baseDir + "\\Genomes");

	public static void main(String[] args) {
		logger = Logger.getLogger("hopshackle");
		FileHandler fh = null;
		try {
			String fileName = "/Simulations/logs/RunWorldGUI_"  + 
			String.format("%tY%<tm%<td_%<tH%<tM.log", Calendar.getInstance());

			fh = new FileHandler(fileName);
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new RunDnDWorldGUI();
		logger.removeHandler(fh);
	}

	private RunDnDWorldGUI() {
		gltModel = new GenomeListTableModel();
		createAndShowGUI();
	}

	private void createAndShowGUI() {
		frame = new JFrame("Run World");

		JPanel background = new JPanel(new BorderLayout());
		background.add(startPanel(), BorderLayout.SOUTH);
		background.add(genomeListPanel(), BorderLayout.CENTER);
		background.add(displayForGenomeChoice(), BorderLayout.NORTH);

		addMenu();
		frame.getContentPane().add(background);

		//Display the window.
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void addMenu() {
		JMenuBar menubar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");

		frame.setJMenuBar(menubar);
		menubar.add(fileMenu);
		JMenuItem saveSettings = new JMenuItem("Save");
		JMenuItem loadSettings = new JMenuItem("Load");
		fileMenu.add(saveSettings);
		fileMenu.add(loadSettings);

		saveSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JFileChooser fc = new JFileChooser(defaultDirectory);
				fc.setDialogType(JFileChooser.SAVE_DIALOG);
				int returnVal = fc.showSaveDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					selectedFile = fc.getSelectedFile();
					try {
						ObjectOutputStream oos;
						oos = new ObjectOutputStream(new FileOutputStream(selectedFile));
						oos.writeObject(gltModel);
						oos.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		loadSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JFileChooser fc = new JFileChooser(defaultDirectory);
				fc.setDialogType(JFileChooser.OPEN_DIALOG);
				int returnVal = fc.showOpenDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					selectedFile = fc.getSelectedFile();
					Object o = null;
					try {
						ObjectInputStream ois;
						ois = new ObjectInputStream(new FileInputStream(selectedFile));
						o = ois.readObject();
						ois.close();
					} catch (FileNotFoundException e) {
						logger.severe("FileNotFound "+ e.toString());
						e.printStackTrace();
					} catch (IOException e) {
						logger.severe("IOException "+ e.toString());
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						logger.severe("ClassNotFound "+ e.toString());
						o = null;
						e.printStackTrace();
					}
					if (o!=null && o instanceof GenomeListTableModel) {
						gltModel = (GenomeListTableModel) o;
						genomeTable.setModel(gltModel);
						gltModel.fireTableDataChanged();
					}
				}
			}
		});
	}

	private JPanel displayForGenomeChoice() {
		JPanel selectGenomePanel = new JPanel(new BorderLayout());
		JPanel sgSub1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel sgSub2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton selectFileButton = new JButton("File");
		JButton addButton = new JButton("Add");
		JButton removeButton = new JButton("Remove");

		selectedFile = new File(baseDir + "\\Genomes\\StarterFighter.txt");
		fileLabel = new JLabel(selectedFile.toString());
		classList = new JComboBox();
		classList.addItem(CharacterClass.FIGHTER);
		classList.addItem(CharacterClass.CLERIC);
		classList.addItem(CharacterClass.EXPERT);

		JLabel numberLabel = new JLabel("Number: ");
		numberField = new JTextField("1");
		numberField.setColumns(3);

		sgSub1.add(fileLabel);
		sgSub1.add(selectFileButton);
		sgSub1.add(new JPanel());

		selectGenomePanel.add(classList);
		selectGenomePanel.add(numberLabel);
		selectGenomePanel.add(numberField);

		sgSub2.add(classList);
		sgSub2.add(numberLabel);
		sgSub2.add(numberField);
		sgSub2.add(addButton);
		sgSub2.add(new JPanel());
		sgSub2.add(removeButton);

		selectGenomePanel.add(sgSub1, BorderLayout.NORTH);
		selectGenomePanel.add(sgSub2, BorderLayout.SOUTH);

		selectFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JFileChooser fc = new JFileChooser(defaultDirectory);
				fc.setDialogType(JFileChooser.OPEN_DIALOG);
				int returnVal = fc.showOpenDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					selectedFile = fc.getSelectedFile();
					fileLabel.setText(selectedFile.toString());
				}
			}
		});

		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				gltModel.addEntry((CharacterClass)classList.getSelectedItem(), 
						selectedFile, 
						Integer.valueOf(numberField.getText()));
			}
		});

		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int row = genomeTable.getSelectedRow();
				if (row > -1) {
					gltModel.removeEntry(row);
				}
			}
		});

		return selectGenomePanel;
	}

	private JPanel startPanel() {
		startButton = new JButton("Start");

		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				HopshackleState.clear();	// always clear to start. This will be loaded later based on settings.
				startDatabaseThread();
				runWorld();
			}
		});


		suffixField = new JTextField("Default_label_001");
		JLabel suffixLabel = new JLabel("Simulation run: ");
		showGUIButton = new JRadioButton("Show GUI? ");
		showGUIButton.setSelected(true);

		JLabel endTimeLabel = new JLabel("Minutes to run: ");
		endTimeField = new JTextField("60");
		iterations = new JTextField("1");
		JLabel iterLabel = new JLabel("Iterations: ");
		endTimeField.setColumns(5);
		iterations.setColumns(3);

		JPanel mainPanel = new JPanel(new SpringLayout());
		mainPanel.add(suffixLabel);
		mainPanel.add(suffixField);
		mainPanel.add(endTimeLabel);
		mainPanel.add(endTimeField);
		mainPanel.add(iterLabel);
		mainPanel.add(iterations);
		mainPanel.add(showGUIButton);
		mainPanel.add(startButton);
		frame.getContentPane().add(mainPanel);

		SpringUtilities.makeCompactGrid(mainPanel,
				4, 2, //rows, cols
				6, 6,        //initX, initY
				6, 6);       //xPad, yPad 

		return mainPanel;
	}

	private JScrollPane genomeListPanel() {

		genomeTable = new JTable(gltModel);

		TableColumnModel tcm = genomeTable.getColumnModel();

		JScrollPane glPanel = new JScrollPane(genomeTable);
		glPanel.setPreferredSize(new Dimension(500,250));
		glPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		tcm.getColumn(0).setMaxWidth(200);
		tcm.getColumn(1).setPreferredWidth(500);
		tcm.getColumn(2).setMaxWidth(100);
		return glPanel;
	}

	private void startDatabaseThread() {
		if (databaseUtility != null) databaseUtility.addUpdate("EXIT");
		databaseUtility = new DatabaseAccessUtility();

		Thread t = new Thread(databaseUtility);
		t.start();
	}

	void runWorld() {
		if (iterations.getText().trim().equals("0"))
			return;

		int endTime = Integer.valueOf(endTimeField.getText());
		if (endTime < 0) endTime = 60;
		if (endTime > 1000) endTime = 1000;

		World w = new World(new ActionProcessor(suffixField.getText(), false), 
				suffixField.getText() + iterations.getText().trim(), 
				((long)endTime) * 60l * 1000l);
		w.initialiseMarket();
		w.setCalendar(new FastCalendar(0));
		w.setLocationMap(new SquareMap(10, 10));
		w.setDatabaseAccessUtility(databaseUtility);
		
		int iterationNumber = Integer.valueOf(iterations.getText());
		
		if (loadStateSpace && stateSpaceToLoad.matches(".*\\*\\*")) {
			String amendedStateSpace = stateSpaceToLoad.replace("**", String.format("%d", iterationNumber));
			SimProperties.setProperty("StateSpaceToLoad", amendedStateSpace);
		}
		
		new RunWorld(w,	gltModel.getGenomeList(),
				gltModel.getClassList(),
				gltModel.getNumberList(), showGUIButton.isSelected(),
				endTime);
		iterations.setText(String.valueOf(iterationNumber-1));

		w.addListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// start next world
				if(arg0.getActionCommand().equals("Death")) {
					runWorld();
				}
			}
		});	
	}
}
