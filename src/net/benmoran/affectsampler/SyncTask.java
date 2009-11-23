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

import net.benmoran.affectsampler.datastore.AppEngineClient;
import net.benmoran.affectsampler.datastore.AppEngineClientImpl;

import org.json.JSONException;

import android.os.AsyncTask;
import android.util.Log;

public class SyncTask extends AsyncTask<SyncParams, Void, SyncResult> {

	private static final String TAG = "SyncTask";
	private final SyncCaller mCaller;

	public SyncTask() {
		mCaller = null;
	}

	public SyncTask(SyncCaller caller) {
		mCaller = caller;
	}

	@Override
	protected SyncResult doInBackground(SyncParams... params) {
		Log.i(TAG, "run()");

		int syncCount;
		SyncParams param = params[0];
		try {
			// TODO Allow Mock client for testing
			AppEngineClient client = new AppEngineClientImpl(param.username,
					param.password, param.URI);
			Synchronizer sync = new Synchronizer(client);
			AffectSerializer ser = new AffectSerializer(param.contentResolver);
			syncCount = sync.sync(ser.toJSONArray());
		} catch (SyncException e) {
			return new SyncResult(e);
		} catch (JSONException e) {
			return new SyncResult(e);
		}
		return new SyncResult(syncCount);
	}

	@Override
	protected void onPostExecute(SyncResult result) {
		super.onPostExecute(result);
		if (mCaller != null) {
			if (result.isOK()) {
				mCaller.onSuccess(result.getCount());
			} else {
				mCaller.onError(result.getException());
			}
		}
	}
}
