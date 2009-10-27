package net.benmoran.affectsampler;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.benmoran.affectsampler.SampleScheduler.PrefsVO;
import android.content.Context;
import android.test.InstrumentationTestCase;

public class SchedulerTest extends InstrumentationTestCase {
	protected PrefsVO mPrefs;
	protected SampleScheduler mScheduler;
    protected Context mContext;
    
	protected void setUp() throws Exception {
		super.setUp();
		mPrefs = makePrefs("01-Jan-09 08:00", "01-Jan-09 22:00", 7);
		mContext = this.getInstrumentation().getContext();
		mScheduler = new SampleScheduler(mContext, mPrefs);
	}

	private PrefsVO makePrefs(String start, String end, int samplesPerDay)
			throws ParseException {
		PrefsVO prefs = new PrefsVO();
		prefs.startTime = parseTime("01-Jan-09 08:00");
		prefs.endTime = parseTime("01-Jan-09 22:00");
		prefs.samplesPerDay = samplesPerDay;
		return prefs;
	}

	private Date parseDate(String dateStr) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy HH:mm");
		return formatter.parse(dateStr);
	}

	private Time parseTime(String dateStr) throws ParseException {
		return new Time(parseDate(dateStr).getTime());
	}

	private Timestamp parseTimestamp(String dateStr) throws ParseException {
		return new Timestamp(parseDate(dateStr).getTime());
	}

	public void testIntervalVO() throws ParseException {
		Interval vo1 = new Interval(parseTimestamp("01-Jan-09 08:00"),
				parseTimestamp("01-Jan-09 10:00"), 0);
		Interval vo2 = new Interval(parseTimestamp("01-Jan-09 08:00"),
				parseTimestamp("01-Jan-09 10:00"), 0);
		Interval vo3 = new Interval(parseTimestamp("01-Jan-09 08:00"),
				parseTimestamp("01-Jan-09 10:00"), 1);
		Interval vo4 = new Interval(parseTimestamp("01-Jan-09 08:01"),
				parseTimestamp("01-Jan-09 10:00"), 0);

		assertEquals(vo1, vo2);
		assertEquals(vo1.hashCode(), vo2.hashCode());
		assertFalse(vo1.equals(vo3));
		assertFalse(vo1.hashCode() == vo3.hashCode());
		assertFalse(vo1.equals(vo4));
		assertFalse(vo1.hashCode() == vo4.hashCode());
	}

//	/**
//	 * 
//	 * Test method for {@link
//	 * net.benmoran.affectsampler.SampleScheduler.getIntervals())}.
//	 * 
//	 * @throws ParseException
//	 */
//	public void testIntervals() throws ParseException {
//		ArrayList<Interval> response = mScheduler.getIntervals();
//		assertEquals(response.size(), mPrefs.samplesPerDay);
//		Interval answers[] = {
//				new Interval(parseTimestamp("01-Jan-09 08:00"),
//						parseTimestamp("01-Jan-09 10:00"), 0),
//				new Interval(parseTimestamp("01-Jan-09 10:00"),
//						parseTimestamp("01-Jan-09 12:00"), 1),
//				new Interval(parseTimestamp("01-Jan-09 12:00"),
//						parseTimestamp("01-Jan-09 14:00"), 2),
//				new Interval(parseTimestamp("01-Jan-09 14:00"),
//						parseTimestamp("01-Jan-09 16:00"), 3),
//				new Interval(parseTimestamp("01-Jan-09 16:00"),
//						parseTimestamp("01-Jan-09 18:00"), 4),
//				new Interval(parseTimestamp("01-Jan-09 18:00"),
//						parseTimestamp("01-Jan-09 20:00"), 5),
//				new Interval(parseTimestamp("01-Jan-09 20:00"),
//						parseTimestamp("01-Jan-09 22:00"), 6) };
//
//		for (int i = 0; i < response.size(); ++i) {
//			Interval interval = response.get(i);
//			assertEquals(interval, answers[i]);
//		}
//	}

	public void testGetNextInterval() throws ParseException {
		Timestamp probeTime = parseTimestamp("01-Jan-09 08:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		assertNotNull(interval);
		assertEquals(parseTimestamp("01-Jan-09 10:00"), interval.startTime);
		assertEquals(parseTimestamp("01-Jan-09 12:00"), interval.endTime);
		assertEquals(1, interval.sequenceID);
	}

	public void testGetNextIntervalYesterday() throws ParseException {
		Timestamp probeTime = parseTimestamp("01-Jan-09 07:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		assertNotNull(interval);
		assertEquals(parseTimestamp("01-Jan-09 08:00"), interval.startTime);
		assertEquals(parseTimestamp("01-Jan-09 10:00"), interval.startTime);

		assertEquals(0, interval.sequenceID);
	}

	public void testGetNextIntervalTomorrow() throws ParseException {
		Timestamp probeTime = parseTimestamp("01-Jan-09 21:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		assertNotNull(interval);
		assertEquals(parseTimestamp("02-Jan-09 08:00"),interval.startTime);	
		assertEquals(parseTimestamp("02-Jan-09 10:00"),interval.endTime);	
		assertEquals(0, interval.sequenceID);
	}


	public void testRandomInInterval() throws ParseException {
		Timestamp probeTime = parseTimestamp("01-Jan-09 21:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		int TESTS=10;
		Timestamp lastTime = null;
		for (int i=0; i<TESTS; ++i) {
			Timestamp nextTime = mScheduler.getNextSampleTime(probeTime);
			assertTrue(interval.containsTimestamp(nextTime));
			if (lastTime != null) {
			   assertFalse(nextTime.equals(lastTime));
			}
			lastTime = nextTime;
		}
	}

	public void scheduleInterval() throws ParseException {
		Timestamp probeTime = parseTimestamp("01-Jan-09 21:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		Timestamp nextTime = mScheduler.scheduleNextSample(probeTime);
		assertTrue(interval.containsTimestamp(nextTime));		
	}

	public void scheduleCancel() throws ParseException {
		mPrefs.samplesPerDay = 0;
		mScheduler = new SampleScheduler(mContext, mPrefs);
		Timestamp probeTime = parseTimestamp("01-Jan-09 21:53");
		Timestamp nextTime = mScheduler.scheduleNextSample(probeTime);
		assertNull(nextTime);
	}
	
}
