package hopshackle.simulation;


import java.util.*;

public class RealtimeCalendar implements WorldCalendar {
	
	private long startTime;
	
	public RealtimeCalendar() {
		// very easy
		// just a wrapper for System Time and usual TimerTask functionality
		startTime = System.currentTimeMillis();
	}
	
	public long getTime() {
		return System.currentTimeMillis() - startTime;
	}

	public void setScheduledTask(TimerTask task, long delay, long period) {
		Timer newTimer = new Timer(true);
		newTimer.scheduleAtFixedRate(task, delay, period);
	}

	public void setScheduledTask(TimerTask task, long delay) {
		Timer newTimer = new Timer(true);
		newTimer.schedule(task, delay);
	}

	public void setTime(long newTime) {
		// do nothing
	}

	public void removeScheduledTask(TimerTask task) {
		task.cancel();
	}

	@Override
	public String getDate() {
		return String.valueOf(getTime());
	}

	@Override
	public String getDate(long time) {
		return String.valueOf(time);
	}
}
