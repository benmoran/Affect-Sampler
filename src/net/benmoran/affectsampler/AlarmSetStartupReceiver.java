package net.benmoran.affectsampler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmSetStartupReceiver extends BroadcastReceiver {
    static final String TAG = "AlarmSetStartupReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// just make sure we are getting the right intent (better safe than
		// sorry)
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			SampleScheduler sched = new SampleScheduler(context);
			sched.scheduleNextSample();
		} else {
			Log.e(TAG, "Received unexpected intent " + intent.toString());
		}

	}

}
