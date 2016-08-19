package hopshackle.simulation;

import java.io.*;
import java.util.logging.Logger;

public class EntityLog {

	protected static String logDir = SimProperties.getProperty("LogDirectory", "C:\\Simulation");
	protected static boolean logUsingDate = SimProperties.getProperty("LogUsingDate", "false").equals("true");
	protected static Logger errorLogger = Logger.getLogger("hopshackle.simulation");
	protected File logFile;
	protected boolean logFileOpen;
	public static String newline = System.getProperty("line.separator");
	protected FileWriter logWriter;
	protected World world;
	private long birth;

	public EntityLog(String logFileName, World w) {
		logFile = new File(logDir + File.separator + logFileName + ".txt");
		world = w;
		logFileOpen = false;
	}

	public void setEntityBirth(long birth) {
		this.birth = birth;
	}

	public void log(String message) {
		if (!logFileOpen) {
			try {
				logWriter = new FileWriter(logFile, true);
				logFileOpen = true;
			} catch (IOException e) {
				e.printStackTrace();
				errorLogger.severe("Failed to open logWriter" + e.toString());
				return;
			}
		}
		if (world != null) {
			if (logUsingDate) {
				message = world.getCurrentDate() + ": " + message;
			} else {
				long age = world.getCurrentTime() - birth;
				message = age + ": " + message; 
			}
		}
		try {
			logWriter.write(message+newline);
		} catch (IOException e) {
			errorLogger.severe(e.toString());
			e.printStackTrace();
		}
	}

	public void rename(String newName) {
		close();
		logFile.renameTo(new File(logDir + File.separator + newName + ".txt"));
	}

	public void close() {
		if (logFileOpen)
			try {
				logWriter.close();
				logFileOpen = false;
			} catch (Exception e) {
				errorLogger.severe(e.toString());
				e.printStackTrace();
			}
	}
}
