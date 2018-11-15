package hopshackle.simulation;

import java.io.*;
import java.util.logging.Logger;

public class EntityLog {

	public static String logDir = SimProperties.getProperty("LogDirectory", "C:\\Simulation");
	protected static boolean logUsingDate = SimProperties.getProperty("LogUsingDate", "false").equals("true");
	public static String newline = System.getProperty("line.separator");
	protected static Logger errorLogger = Logger.getLogger("hopshackle.simulation");
	protected File logFile;
	protected boolean logFileOpen;
	protected FileWriter logWriter;
	protected WorldCalendar calendar;
	private long birth;

	public EntityLog(String logFileName, WorldCalendar cal) {
		logFile = new File(logDir + File.separator + logFileName + ".txt");
		calendar = cal;
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
		if (calendar != null) {
			if (logUsingDate) {
				message = calendar.getDate() + ": " + message;
			} else {
				long age = calendar.getTime() - birth;
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

	public void flush() {
		if (logFileOpen) {
			try {
				logWriter.flush();
			} catch (Exception e) {
				errorLogger.severe(e.toString());
				e.printStackTrace();
			}
		}
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
