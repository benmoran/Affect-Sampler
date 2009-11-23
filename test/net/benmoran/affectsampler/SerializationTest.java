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

import net.benmoran.affectsampler.SampleScheduler.PrefsVO;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.test.InstrumentationTestCase;

public class SerializationTest extends InstrumentationTestCase {
	protected PrefsVO mPrefs;
	protected Context mContext;
	protected JSONArray mJSONArray;

	protected void setUp() throws Exception {
		super.setUp();
		mPrefs = TestUtils.makePrefs("01-Jan-09 08:00", "01-Jan-09 22:00", 7);
		mContext = this.getInstrumentation().getContext();
		TestDataBuilder tdBuilder = new TestDataBuilder(mContext.getContentResolver());
		mJSONArray = tdBuilder.getJSONArray();
	}

	public void testSerialization() throws JSONException {
		AffectSerializer af = new AffectSerializer(mContext.getContentResolver());
		assertEquals(mJSONArray.toString(), af.toJSONString());
	}
}
