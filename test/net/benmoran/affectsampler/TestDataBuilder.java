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

import java.text.ParseException;
import java.util.Date;

import net.benmoran.provider.AffectSampleStore.AffectSamples;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;

public class TestDataBuilder {
	private ContentResolver mContentResolver;
	private JSONArray mJSONArray;

	public TestDataBuilder(ContentResolver contentResolver) {
		mContentResolver = contentResolver;
		mJSONArray = new JSONArray();
		try {
			addSample(0.2, 0.3, null, TestUtils.parseDate("01-Jan-10 11:12"));
			addSample(0.4, 0.3, "Testing", TestUtils.parseDate("01-Jan-10 11:12"));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public void addSample(double emotion, double intensity, String comment,
			Date date) throws JSONException {

		ContentValues values = new ContentValues();
		values.put(AffectSamples.EMOTION, emotion);
		values.put(AffectSamples.INTENSITY, intensity);
		values.put(AffectSamples.COMMENT, comment);
		values.put(AffectSamples.SCHEDULED_DATE, date.getTime());
		values.put(AffectSamples.CREATED_DATE, date.getTime());
		mContentResolver.insert(AffectSamples.CONTENT_URI, values);
		mJSONArray.put(new JSONObject().put(AffectSamples.EMOTION, emotion)
				.put(AffectSamples.INTENSITY, intensity).put(
						AffectSamples.COMMENT, comment).put(
						AffectSamples.SCHEDULED_DATE, date.getTime()).put(
						AffectSamples.CREATED_DATE, date.getTime()));
	}
	public JSONArray getJSONArray() {
		return mJSONArray;
	}
}
