package net.benmoran.affectsampler;

import java.sql.Timestamp;

import net.benmoran.affectsampler.charts.IChart;
import net.benmoran.affectsampler.charts.TimeSeriesChart;
import net.benmoran.provider.AffectSampleStore.AffectSamples;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AffectSampler extends Activity {
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
		// TODO: Make this class store a 'scheduled time' that's null if opened
		// manually
		// or a timestamp if coming from an alarm - store that with the sample
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
		// Schedule the next sample
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
			String[] projection = new String[] { AffectSamples.CREATED_DATE, AffectSamples.EMOTION,
					AffectSamples.INTENSITY };

			Cursor cursor = managedQuery(samples, projection, null, null,
					AffectSamples.CREATED_DATE + " ASC");

			Intent intent = mChart.execute(this, cursor);
			startActivity(intent);
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

}