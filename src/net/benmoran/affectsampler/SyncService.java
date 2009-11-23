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

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class SyncService extends Service {
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("BackgroundService", "onCreate()");
		Thread thr = new Thread(null, new RunThread(), "SyncService");
		thr.start();
	}

	public class SyncBinder extends Binder {
		SyncService getService() {
			return SyncService.this;
		}
	}

	class RunThread implements Runnable {
		public void run() {
			Log.i("SyncService", "run()");

			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());

			String user = settings
					.getString(getString(R.string.username), null);
			String pass = settings
					.getString(getString(R.string.password), null);
			SyncParams params = new SyncParams(user, pass, null, getContentResolver());
			new SyncTask().execute(params);

//			try {
//				// TODO Allow Mock client for testing
//				AppEngineClient client = new AppEngineClientImpl(user, pass, null);
//				Synchronizer sync = new Synchronizer(client);
//				AffectSerializer ser = new AffectSerializer(
//						getContentResolver());
//				sync.sync(ser.toJSONArray());
//				// TODO: Use synchronizer to get only latest
//			} catch (SyncException e) {
//				// TODO Report error to user
//				e.printStackTrace();
//			} catch (JSONException e) {
//				// TODO Report error to user
//				e.printStackTrace();
//			}
			SyncService.this.stopSelf();
		}
	}

	@Override
	public void onDestroy() {
		Log.i("SyncService", "onDestroy()");
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i("SyncService", "onStart()");
		super.onStart(intent, startId);
	}

    @Override
    public IBinder onBind(Intent intent) {
    	Log.i("SyncService", "onBind()");
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new SyncBinder();


}
