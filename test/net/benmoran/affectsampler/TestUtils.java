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

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.benmoran.affectsampler.SampleScheduler.PrefsVO;

public class TestUtils {
	public static PrefsVO makePrefs(String start, String end, int samplesPerDay)
			throws ParseException {
		PrefsVO prefs = new PrefsVO();
		prefs.startTime = parseTime("01-Jan-09 08:00");
		prefs.endTime = parseTime("01-Jan-09 22:00");
		prefs.samplesPerDay = samplesPerDay;
		return prefs;
	}

	public static Date parseDate(String dateStr) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy HH:mm");
		return formatter.parse(dateStr);
	}

	public static Time parseTime(String dateStr) throws ParseException {
		return new Time(parseDate(dateStr).getTime());
	}


	public static Timestamp parseTimestamp(String dateStr) throws ParseException {
		return new Timestamp(parseDate(dateStr).getTime());
	}

}
