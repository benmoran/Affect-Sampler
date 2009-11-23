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

import android.content.Context;
import android.content.Intent;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

// TODO: The SyncService isn't yet used, that will be for 
// the background automatic sync service
public class SyncServiceTestCase extends ServiceTestCase<SyncService> {
	protected PrefsVO mPrefs;
	protected Context mContext;
	protected JSONArray mJSONArray;

	public SyncServiceTestCase(Class<SyncService> serviceClass) {
		super(serviceClass);
	}

	public SyncServiceTestCase() {
		super(SyncService.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		mPrefs = TestUtils.makePrefs("01-Jan-09 08:00", "01-Jan-09 22:00", 7);
		mContext = getContext();
		TestDataBuilder tdBuilder = new TestDataBuilder(mContext
				.getContentResolver());

		mJSONArray = tdBuilder.getJSONArray();
	}

	/**
	 * The name 'test preconditions' is a convention to signal that if this test
	 * doesn't pass, the test case was not set up properly and it might explain
	 * any and all failures in other tests. This is not guaranteed to run before
	 * other tests, as junit uses reflection to find the tests.
	 */
	@SmallTest
	public void testPreconditions() {
	}

	/**
	 * Test basic startup/shutdown of Service
	 */
	@SmallTest
	public void testStartable() {
		Intent startIntent = new Intent();
		startIntent.setClass(getContext(), SyncService.class);
		startService(startIntent);
	}

//	/**
//	 * Test binding to service
//	 */
//	@MediumTest
//	public void testBindable() {
//		Intent startIntent = new Intent();
//		startIntent.setClass(getContext(), SyncService.class);
//		IBinder service = bindService(startIntent);
//	}

}
