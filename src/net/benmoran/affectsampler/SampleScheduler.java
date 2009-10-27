package net.benmoran.affectsampler;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SampleScheduler {

	public static final String SCHEDULED_SAMPLE = new String(
			"net.benmoran.affectsampler.SCHEDULED_SAMPLE");

	public static final String SCHEDULED_AT = new String(
			"net.benmoran.affectsampler.SCHEDULED_AT");

	public static class PrefsVO {
		public Time startTime;
		public Time endTime;
		public int samplesPerDay;
	}

	public static final int REQUEST_CODE = 0;

	protected PrefsVO mPrefs;
	protected Context mContext;

	private static Random sRandom = new Random();

	//
	// private static Date parseDate(String dateStr) throws ParseException {
	// SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy HH:mm");
	// return formatter.parse(dateStr);
	// }

	private static Time parseTime(String dateStr) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("HHmm");
		Date date = formatter.parse(dateStr);
		return new Time(date.getTime());
	}

	static protected PrefsVO readPrefs(Context context) {
		// TODO: TEST
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		PrefsVO prefs = new PrefsVO();
		try {
			prefs.startTime = parseTime(settings
					.getString("Start time", "0800"));
		} catch (ParseException e) {
			prefs.startTime = new Time(8, 0, 0);
		}
		try {
			prefs.endTime = parseTime(settings.getString("End time", "2000"));
		} catch (ParseException e) {
			prefs.endTime = new Time(20, 0, 0);
		}

		prefs.samplesPerDay = Integer.parseInt(settings.getString("Frequency",
				"4"));
		return prefs;
	}

	public SampleScheduler(Context context) {
		this(context, readPrefs(context));
	}

	public SampleScheduler(Context context, PrefsVO prefs) {
		mContext = context;
		mPrefs = prefs;
	}

	//
	// public ArrayList<Interval> getIntervals() {
	// ArrayList<Interval> intervals = new ArrayList<Interval>();
	//
	// long interval = (mPrefs.endTime.getTime() - mPrefs.startTime.getTime())
	// / mPrefs.samplesPerDay;
	// for (int i = 0; i < mPrefs.samplesPerDay; ++i) {
	// long l = mPrefs.startTime.getTime() + interval * i;
	// intervals.add(new Interval(new Timestamp(l), new Timestamp(l
	// + interval), i));
	// }
	// return intervals;
	// }

	private Calendar timeOnDay(Time time, Timestamp day) {
		Calendar timeCal = Calendar.getInstance();
		timeCal.setTime(time);
		Calendar dayCal = Calendar.getInstance();
		dayCal.setTime(day);
		int hours = timeCal.get(Calendar.HOUR_OF_DAY);
		int mins = timeCal.get(Calendar.MINUTE);
		int secs = timeCal.get(Calendar.SECOND);

		dayCal.set(Calendar.HOUR_OF_DAY, hours);
		dayCal.set(Calendar.MINUTE, mins);
		dayCal.set(Calendar.SECOND, secs);
		return dayCal;
	}
	
	public Interval getNextInterval(Timestamp probeTime) {
		Calendar start = timeOnDay(mPrefs.startTime, probeTime);
		Calendar end = timeOnDay(mPrefs.endTime, probeTime);		
		
		long startMs = start.getTimeInMillis();
		long endMs = end.getTimeInMillis();
		long duration = (endMs - startMs) / mPrefs.samplesPerDay;

		if (probeTime.getTime() >= (endMs - duration)) {
			start.add(Calendar.DATE, 1);
			end.add(Calendar.DATE, 1);
			startMs = start.getTimeInMillis();
			endMs = end.getTimeInMillis();

		}

		int seq = (int) Math.max((probeTime.getTime() - startMs) / duration, -1);
		seq++;
		Timestamp startTime = new Timestamp(startMs + duration * (seq));
		Timestamp endTime = new Timestamp(startMs + duration * (seq+1));		
		return new Interval(startTime, endTime, seq);
	}

	private boolean noAlarms() {
		return (mPrefs.samplesPerDay == 0);
	}

	public Timestamp getNextSampleTime(Timestamp probeTime) {
		if (noAlarms())
			return null;
		Interval interval = getNextInterval(probeTime);
		return new Timestamp(randomLong(interval.startTime.getTime(),
				interval.endTime.getTime()));
	}

	public static long randomLong(long min, long max) {
		long range = max - min;
		long val = Math.abs(sRandom.nextLong());
		val = val % range;
		return min + val;
	}

	public Timestamp scheduleNextSample() {
		return scheduleNextSample(new Timestamp(System.currentTimeMillis()));
	}

	/**
	 * Schedules alarm for getNextTime, clearing any previous alarms (or cancels
	 * any previous if freq=0 in prefsObj).
	 * 
	 * @return the time that was scheduled, or null if cancelled.
	 **/
	public Timestamp scheduleNextSample(Timestamp probeTime) {

		// TODO: Test this
		Intent intent = new Intent(SCHEDULED_SAMPLE);
		Timestamp schedule = getNextSampleTime(probeTime);
		intent.putExtra(SCHEDULED_AT, noAlarms() ? -1 : schedule.getTime());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,
				REQUEST_CODE, intent, 0);

		if (noAlarms()) {
			cancelAlarm(pendingIntent);
			return null;
		} else {
			return setAlarm(pendingIntent, schedule);
		}
	}

	private Timestamp setAlarm(PendingIntent pendingIntent, Timestamp schedule) {
		AlarmManager alarmManager = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);

		alarmManager.set(AlarmManager.RTC_WAKEUP, schedule.getTime(),
				pendingIntent);
		Toast.makeText(mContext, "Alarm set " + schedule.toString(),
				Toast.LENGTH_LONG).show();
		return schedule;
	}

	private void cancelAlarm(PendingIntent pendingIntent) {
		AlarmManager alarmManager = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		Toast.makeText(mContext, "Alarm cancelled", Toast.LENGTH_LONG).show();
	}
}
