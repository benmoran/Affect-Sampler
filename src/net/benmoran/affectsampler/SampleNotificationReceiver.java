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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SampleNotificationReceiver extends BroadcastReceiver {
    static final String TAG = "SampleNotificationReceiver";

    static final int SAMPLE_NOTIFICATION = 0;
    
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (SampleScheduler.SCHEDULED_SAMPLE.equals(intent.getAction())) {
			// Set a notification that a sample is due
			long scheduledAt = intent.getLongExtra(SampleScheduler.SCHEDULED_AT, -1);
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

			Notification notification = new Notification(R.drawable.nuvoladrama, "Affect sampler", System.currentTimeMillis());
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.defaults |= Notification.DEFAULT_SOUND;
			long[] vibrate = {0,100,200,300,200,100};
			notification.vibrate = vibrate;
			
			Intent notificationIntent = new Intent(context, AffectSampler.class);
			notificationIntent.putExtra(SampleScheduler.SCHEDULED_AT, scheduledAt);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

			//notification.sound = Uri.parse("file:///sdcard/notification/ringer.mp3");
			CharSequence contentTitle = "Affect sampler";  // expanded message title
			CharSequence contentText = "How are you feeling?";      // expanded message text

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

			notificationManager.notify(SAMPLE_NOTIFICATION, notification);
			// which will open AffectSampler, with the scheduledAt passed through
			
		} else {
			Log.e(TAG, "Received unexpected intent " + intent.toString());
		}

	}

}
