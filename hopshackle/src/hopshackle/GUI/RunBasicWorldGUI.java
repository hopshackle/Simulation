package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.basic.*;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
import java.util.Calendar;
import java.util.logging.*;

import javax.swing.*;

public class RunBasicWorldGUI {

	private JFrame frame;
	private JTextField suffixField;
	private JButton startButton;
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
	private static File defaultDirectory = new File(baseDir + "\\Maps");

	public static void main(String[] args) {
		logger = Logger.getLogger("hopshackle");
		FileHandler fh = null;
		try {
			String fileName = SimProperties.getProperty("BaseDirectory", "C:/Simulations/logs") + "/RunWorldGUI_" + 
					String.format("%tY%<tm%<td_%<tH%<tM.log", Calendar.getInstance());
			fh = new FileHandler(fileName);
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new RunBasicWorldGUI();
		logger.removeHandler(fh);
	}

	private RunBasicWorldGUI() {
		createAndShowGUI();
	}

	private void createAndShowGUI() {
		frame = new JFrame("Run Basic World");

		JPanel background = new JPanel(new BorderLayout());
		background.add(startPanel(), BorderLayout.SOUTH);

		frame.getContentPane().add(background);

		//Display the window.
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private JPanel startPanel() {
		JButton selectFileButton = new JButton("File");

		selectedFile = new File(baseDir + "\\Maps\\1_Rooting_Out_A_Mage.map");
		fileLabel = new JLabel(selectedFile.toString());

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
		mainPanel.add(selectFileButton);
		mainPanel.add(fileLabel);
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
				5, 2, //rows, cols
				6, 6,        //initX, initY
				6, 6);       //xPad, yPad 

		return mainPanel;
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
		w.setCalendar(new RealtimeCalendar());
		w.setLocationMap(new HexMap<BasicHex>(selectedFile, BasicHex.getHexFactory()));
		w.setDatabaseAccessUtility(databaseUtility);
		
		int iterationNumber = Integer.valueOf(iterations.getText());
		
		if (loadStateSpace && stateSpaceToLoad.matches(".*\\*\\*")) {
			String amendedStateSpace = stateSpaceToLoad.replace("**", String.format("%d", iterationNumber));
			SimProperties.setProperty("StateSpaceToLoad", amendedStateSpace);
		}
		
		new BasicRunWorld(w, showGUIButton.isSelected(), ((long)endTime) * 60l * 1000l);
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
