/*
 * 
 *  Copyright 2009 (C) Ben Moran
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *     
 *  Unless required by applicable law or agreed to in writing, 
 *  software distributed under the License is distributed on an 
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific 
 *  language governing permissions and limitations under the License. 
 */
package net.benmoran.affectsampler;

import java.sql.Timestamp;
import java.text.ParseException;

import net.benmoran.affectsampler.SampleScheduler.PrefsVO;
import android.content.Context;
import android.test.InstrumentationTestCase;

public class SchedulerTest extends InstrumentationTestCase {
	protected PrefsVO mPrefs;
	protected SampleScheduler mScheduler;
    protected Context mContext;
    
	protected void setUp() throws Exception {
		super.setUp();
		mPrefs = TestUtils.makePrefs("01-Jan-09 08:00", "01-Jan-09 22:00", 7);
		mContext = this.getInstrumentation().getContext();
		mScheduler = new SampleScheduler(mContext, mPrefs);
	}

	
	public void testIntervalVO() throws ParseException {
		Interval vo1 = new Interval(TestUtils.parseTimestamp("01-Jan-09 08:00"),
				TestUtils.parseTimestamp("01-Jan-09 10:00"), 0);
		Interval vo2 = new Interval(TestUtils.parseTimestamp("01-Jan-09 08:00"),
				TestUtils.parseTimestamp("01-Jan-09 10:00"), 0);
		Interval vo3 = new Interval(TestUtils.parseTimestamp("01-Jan-09 08:00"),
				TestUtils.parseTimestamp("01-Jan-09 10:00"), 1);
		Interval vo4 = new Interval(TestUtils.parseTimestamp("01-Jan-09 08:01"),
				TestUtils.parseTimestamp("01-Jan-09 10:00"), 0);

		assertEquals(vo1, vo2);
		assertEquals(vo1.hashCode(), vo2.hashCode());
		assertFalse(vo1.equals(vo3));
		assertFalse(vo1.hashCode() == vo3.hashCode());
		assertFalse(vo1.equals(vo4));
		assertFalse(vo1.hashCode() == vo4.hashCode());
	}

	public void testGetNextInterval() throws ParseException {
		Timestamp probeTime = TestUtils.parseTimestamp("01-Jan-09 08:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		assertNotNull(interval);
		assertEquals(TestUtils.parseTimestamp("01-Jan-09 10:00"), interval.startTime);
		assertEquals(TestUtils.parseTimestamp("01-Jan-09 12:00"), interval.endTime);
		assertEquals(1, interval.sequenceID);
	}

	public void testGetNextIntervalYesterday() throws ParseException {
		Timestamp probeTime = TestUtils.parseTimestamp("01-Jan-09 07:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		assertNotNull(interval);
		assertEquals(TestUtils.parseTimestamp("01-Jan-09 08:00"), interval.startTime);
		assertEquals(TestUtils.parseTimestamp("01-Jan-09 10:00"), interval.startTime);

		assertEquals(0, interval.sequenceID);
	}

	public void testGetNextIntervalTomorrow() throws ParseException {
		Timestamp probeTime = TestUtils.parseTimestamp("01-Jan-09 21:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		assertNotNull(interval);
		assertEquals(TestUtils.parseTimestamp("02-Jan-09 08:00"),interval.startTime);	
		assertEquals(TestUtils.parseTimestamp("02-Jan-09 10:00"),interval.endTime);	
		assertEquals(0, interval.sequenceID);
	}


	public void testRandomInInterval() throws ParseException {
		Timestamp probeTime = TestUtils.parseTimestamp("01-Jan-09 21:53");
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
		Timestamp probeTime = TestUtils.parseTimestamp("01-Jan-09 21:53");
		Interval interval = mScheduler.getNextInterval(probeTime);
		Timestamp nextTime = mScheduler.scheduleNextSample(probeTime);
		assertTrue(interval.containsTimestamp(nextTime));		
	}

	public void scheduleCancel() throws ParseException {
		mPrefs.samplesPerDay = 0;
		mScheduler = new SampleScheduler(mContext, mPrefs);
		Timestamp probeTime = TestUtils.parseTimestamp("01-Jan-09 21:53");
		Timestamp nextTime = mScheduler.scheduleNextSample(probeTime);
		assertNull(nextTime);
	}
	
}
