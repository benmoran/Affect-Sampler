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

import net.benmoran.provider.AffectSampleStore.AffectSamples;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class AffectSerializer {

	ContentResolver mContentResolver;

	public AffectSerializer(ContentResolver contentResolver) {
		mContentResolver = contentResolver;
	}

	public String toJSONString() throws JSONException {
		return toJSONArray().toString();
	}
	
	public JSONArray toJSONArray() throws JSONException {	
		Uri samples = AffectSamples.CONTENT_URI;
		String[] projection = new String[] { AffectSamples.COMMENT,
				AffectSamples.SCHEDULED_DATE, AffectSamples.CREATED_DATE,
				AffectSamples.EMOTION, AffectSamples.INTENSITY };

		Cursor cursor = mContentResolver.query(samples,
				projection, null, null, AffectSamples.CREATED_DATE + " ASC");
		JSONArray arr = new JSONArray();
		int emIndex = cursor.getColumnIndex(AffectSamples.EMOTION);
		int inIndex = cursor.getColumnIndex(AffectSamples.INTENSITY);
		int cdIndex = cursor.getColumnIndex(AffectSamples.CREATED_DATE);
		int sdIndex = cursor.getColumnIndex(AffectSamples.SCHEDULED_DATE);
		int coIndex = cursor.getColumnIndex(AffectSamples.COMMENT);

		for (cursor.moveToPosition(0); !cursor.isLast(); cursor.moveToNext()) {
			JSONObject o = new JSONObject();
			o.put(AffectSamples.EMOTION, cursor.getDouble(emIndex));
			o.put(AffectSamples.INTENSITY, cursor.getDouble(inIndex));
			if (cursor.getLong(sdIndex) > 0) {
				o.put(AffectSamples.SCHEDULED_DATE, cursor.getLong(sdIndex));
			} else {
				o.put(AffectSamples.SCHEDULED_DATE, null);
			}

			o.put(AffectSamples.CREATED_DATE, cursor.getLong(cdIndex));
			if (cursor.getString(coIndex) != null) {
				o.put(AffectSamples.COMMENT, cursor.getString(coIndex));
			} else {
				o.put(AffectSamples.COMMENT, null);
			}
			arr.put(o);
		}
		cursor.close();
		return arr;
	}
}
