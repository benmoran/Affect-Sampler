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

import java.net.URI;

import net.benmoran.affectsampler.datastore.AppEngineClient;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Synchronizer {

	private static final int MISSING_LATEST = -1;
	private static final String TAG = "Synchronizer";
	
	private AppEngineClient mClient;

	public Synchronizer(AppEngineClient client)
			throws SyncException {
		
		mClient = client;
	}

	public int sync(JSONArray array) throws SyncException {
		int synced = 0;
		for (int i = 0; i < array.length(); ++i) {
			try {
				if (syncObject(array.getJSONObject(i))) {
					synced++;
				}
			} catch (JSONException e) {
				throw new SyncException(e);
			}
		}
		return synced;
	}

	private boolean syncObject(JSONObject object) throws SyncException {
		Log.i(TAG, "Sending: " + object.toString());
		HttpResponse response = mClient.postJSON(getCreateSampleUri(), object.toString());
		Log.i(TAG, response.getStatusLine().toString());
		switch (response.getStatusLine().getStatusCode()) {
		case 201:
			return true;
		case 302:
			return false;
		default:
			throw new SyncException("Unexpected response to sync: " + response.getStatusLine().toString());
		}

	}

	private URI getCreateSampleUri() {
		return URI.create("/Sample/");
	}


	public long getLatest() {
		return MISSING_LATEST;
	}

}
