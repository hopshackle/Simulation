package hopshackle.simulation;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class FastCalendar implements WorldCalendar {

	private AtomicLong time;
	private ArrayList<CalendarTask> scheduledJobs;

	public FastCalendar(long startTime) {
		time = new AtomicLong(startTime);
		scheduledJobs = new ArrayList<>();
	}


	public long getTime() {
		return time.get();
	}

	public synchronized void setScheduledTask(TimerTask task, long delay, long period) {
		CalendarTask newTask = new CalendarTask(task, delay+this.getTime(), period);
		scheduledJobs.add(newTask);
	}

	public synchronized void setScheduledTask(TimerTask task, long delay) {
		CalendarTask newTask = new CalendarTask(task, delay+this.getTime(), -1);
		scheduledJobs.add(newTask);
	}

	public void setTime(long newTime) {
		long oldTime = time.get();
		if (newTime > oldTime) {
			checkForTasks(newTime);
		}
	}

	private synchronized void checkForTasks(long currentTime) {
		Collections.sort(scheduledJobs);
		ArrayList<CalendarTask> toRemove = new ArrayList<CalendarTask>();
		for (CalendarTask t : scheduledJobs) {
			long oldTime = time.get();
			if (t.nextRun <= currentTime) {
				if (t.nextRun > oldTime) {
					time.compareAndSet(oldTime, t.nextRun);
				}
				t.task.run();
				t.lastRun = time.get();
				if (t.period == -1) {
					toRemove.add(t);
				} else {
					t.nextRun = t.nextRun + t.period;
				}
			} else {
				break;
			}
		}
		for (CalendarTask t : toRemove)
			scheduledJobs.remove(t);
		time.compareAndSet(time.get(), currentTime);
	}

	class CalendarTask implements Comparable<CalendarTask>{

		Runnable task;
		long period;
		long lastRun;
		long nextRun;

		CalendarTask(Runnable task, long startTime, long period) {
			this.task = task;
			this.nextRun = startTime;
			this.period = period;
			lastRun = 0l;
		}

		public int compareTo(CalendarTask ct2) {
			return (int) (this.nextRun - ct2.nextRun);
		}
	}

	public synchronized void removeScheduledTask(TimerTask task) {
		if (scheduledJobs.contains(task)) {
			scheduledJobs.remove(task);
		}
	}

	@Override
	public String getDate(long time) {
		int year = (int) (time / 52.0);
		int s = (int) ((time - 52 * year) / 13.0);
		String season = null;
		switch (s) {
			case 0:
				season = "Winter";
				break;
			case 1:
				season = "Spring";
				break;
			case 2:
				season = "Summer";
				break;
			case 3:
				season = "Autumn";
				break;
		}
		return season + " " + String.valueOf(year);
	}

	@Override
	public String getDate() {
		long time = getTime();
		return getDate(time);
	}
}
