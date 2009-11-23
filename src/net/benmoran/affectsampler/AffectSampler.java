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

import java.sql.Timestamp;

import net.benmoran.affectsampler.charts.IChart;
import net.benmoran.affectsampler.charts.TimeSeriesChart;
import net.benmoran.provider.AffectSampleStore.AffectSamples;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AffectSampler extends Activity implements SyncCaller {
	public static final int MENU_SETTINGS = 0;
	public static final int MENU_QUIT = 1;

	private IChart mChart;
	private Button btnSave;
	private Button btnCancel;
	protected Timestamp mScheduled;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mChart = new TimeSeriesChart();

		btnSave = (Button) findViewById(R.id.ButtonSave);
		btnSave.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				saveSample();
			}
		});

		btnCancel = (Button) findViewById(R.id.ButtonCancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				scheduleAndQuit();
			}
		});

		mScheduled = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			long scheduled = extras.getLong(SampleScheduler.SCHEDULED_AT, -1);
			mScheduled = (scheduled > 0) ? new Timestamp(scheduled) : null;
		}
	}

	private void saveSample() {
		HorizontalSlider hzEmotion = (HorizontalSlider) findViewById(R.id.slideEmotion);
		HorizontalSlider hzIntensity = (HorizontalSlider) findViewById(R.id.slideIntensity);
		TextView txtComment = (TextView) findViewById(R.id.textComment);

		ContentValues values = new ContentValues();
		values.put(AffectSamples.EMOTION, hzEmotion.getValue());
		values.put(AffectSamples.INTENSITY, hzIntensity.getValue());
		values.put(AffectSamples.COMMENT, txtComment.getText().toString());
		if (mScheduled != null) {
			values.put(AffectSamples.SCHEDULED_DATE, mScheduled.getTime());
		}
		getContentResolver().insert(AffectSamples.CONTENT_URI, values);
		scheduleAndQuit();
	}

	private void scheduleAndQuit() {
		SampleScheduler sched = new SampleScheduler(this);
		sched.scheduleNextSample();

		finish();
	}

	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.chart_menu_item:
			Uri samples = AffectSamples.CONTENT_URI;
			String[] projection = new String[] { AffectSamples.CREATED_DATE,
					AffectSamples.EMOTION, AffectSamples.INTENSITY };

			Cursor cursor = managedQuery(samples, projection, null, null,
					AffectSamples.CREATED_DATE + " ASC");

			Intent intent = mChart.execute(this, cursor);
			startActivity(intent);
			return (true);

		case R.id.sync_menu_item:
			doSync();
			return (true);

		case R.id.settings_menu_item:
			startActivity(new Intent(this, AffectPreferences.class));
			return (true);

		case R.id.quit_menu_item:
			finish();
			return (true);
		}

		return (super.onOptionsItemSelected(item));
	}

	private void doSync() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		String user = settings.getString(getString(R.string.username), null);
		String pass = settings.getString(getString(R.string.password), null);
		SyncParams params = new SyncParams(user, pass, null, getContentResolver());
		new SyncTask(this).execute(params);
	}

	public void onError(Exception e) {
		Toast.makeText(this, "Sync failed: " + e.toString(),
				Toast.LENGTH_LONG).show();
	}

	public void onSuccess(int syncCount) {
		Toast.makeText(this, "Sync succeeded: " + Integer.toString(syncCount) + " items",
				Toast.LENGTH_LONG).show();
	}

}