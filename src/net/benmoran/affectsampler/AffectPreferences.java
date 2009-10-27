package net.benmoran.affectsampler;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class AffectPreferences extends PreferenceActivity {
	private static final String TAG = "AffectPreferences";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			addPreferencesFromResource(R.xml.preferences);
		} catch (ClassCastException e) {
			Log.e(TAG, "Could not load preferences", e);
		}
	}

	@Override
	protected void onStop() {
		SampleScheduler sched = new SampleScheduler(this);
		sched.scheduleNextSample();
		super.onStop();
	}

}
