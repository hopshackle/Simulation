package hopshackle.simulation;

import java.util.TimerTask;

public interface WorldCalendar {

	public long getTime();
	
	public void setTime(long newTime);
	
	public void setScheduledTask(TimerTask task, long delay, long period);
	
	public void setScheduledTask(TimerTask task, long delay);

	public void removeScheduledTask(TimerTask task);

	public String getDate();
}
