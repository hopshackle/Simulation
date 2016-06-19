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
	String defaultMap = SimProperties.getProperty("BasicDefaultMap", "1_Rooting_Out_A_Mage.map");

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
		if (args.length > 0 && args[0].equals("-noGUI")) {
			new RunBasicWorldGUI(false);
		} else {
			new RunBasicWorldGUI(true);
		}
		logger.removeHandler(fh);
	}

	private RunBasicWorldGUI(boolean showGUI) {
		selectedFile = new File(baseDir + "\\Maps\\" + defaultMap);
		if (showGUI) {
			createAndShowGUI();
		} else {
			int iterationNumber = SimProperties.getPropertyAsInteger("BasicIterations", "30");
			int endTime = SimProperties.getPropertyAsInteger("BasicEndTime", "60");
			boolean realTime = SimProperties.getProperty("BasicRealTime", "false").equals("true");
			String runName = SimProperties.getProperty("BasicRunName", "default");
			startDatabaseThread();
			runWorld(runName, iterationNumber, endTime, realTime, false);
		}
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
				int endTime = Integer.valueOf(endTimeField.getText());
				if (endTime < 0) endTime = 60;
				if (endTime > 1000) endTime = 1000;
				String runName = suffixField.getText();
				String runNumber = iterations.getText().trim();
				int iterationNumber = Integer.valueOf(runNumber);
				startDatabaseThread();
				runWorld(runName, iterationNumber, endTime, true, showGUIButton.isSelected());
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
		HopshackleState.clear();
		if (databaseUtility != null) databaseUtility.addUpdate("EXIT");
		databaseUtility = new DatabaseAccessUtility();

		Thread t = new Thread(databaseUtility);
		t.start();
	}

	void runWorld(String runName, int iterationNumber, int endTime, boolean realTime, boolean showGUI) {
		if (iterationNumber == 0)
			return;

		World w = new World(new ActionProcessor(runName, realTime), 
				runName + "_" + iterationNumber,
				((long)endTime) * 60l * 1000l);
		w.initialiseMarket();
		if (realTime) {
			w.setCalendar(new RealtimeCalendar());
		} else {
			w.setCalendar(new FastCalendar(0l));
		}
		w.setLocationMap(new HexMap<BasicHex>(selectedFile, BasicHex.getHexFactory()));
		w.setDatabaseAccessUtility(databaseUtility);
		
		if (loadStateSpace && stateSpaceToLoad.matches(".*\\*\\*")) {
			String amendedStateSpace = stateSpaceToLoad.replace("**", String.format("%d", iterationNumber));
			SimProperties.setProperty("StateSpaceToLoad", amendedStateSpace);
		}
		
		new BasicRunWorld(w, showGUI, ((long)endTime) * 60l * 1000l);

		w.addListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// start next world
				if(arg0.getActionCommand().equals("Death")) {
					Agent.clearAndResetCacheBuffer(500);
					runWorld(runName, iterationNumber - 1, endTime, realTime, showGUI);
				}
			}
		});	
	}
}
