/**
 * Adapted Ben Moran 2009
 * 
 * Original Copyright (C) 2009 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.benmoran.affectsampler.charts;

import java.util.ArrayList;
import java.util.List;

import net.benmoran.provider.AffectSampleStore.AffectSamples;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;

/**
 * Time series affect chart.
 */
public class TimeSeriesChart extends AbstractChart {
	/**
	 * Returns the chart name.
	 * 
	 * @return the chart name
	 */
	public String getName() {
		return "Affect Sampler";
	}

	/**
	 * Returns the chart description.
	 * 
	 * @return the chart description
	 */
	public String getDesc() {
		return "Changing mood over time";
	}

	/**
	 * Executes the chart demo.
	 * 
	 * @param context
	 *            the context
	 * @param cursor
	 *            the data cursor
	 * @return the built intent
	 */
	public Intent execute(Context context, Cursor cursor) {

		String[] titles = new String[] { AffectSamples.EMOTION,
				AffectSamples.INTENSITY };
		long startTime, endTime;
		int emIndex = cursor.getColumnIndex(AffectSamples.EMOTION);
		int inIndex = cursor.getColumnIndex(AffectSamples.INTENSITY);
		int cdIndex = cursor.getColumnIndex(AffectSamples.CREATED_DATE);

		List<double[]> values = new ArrayList<double[]>();

		int length = cursor.getCount();
		int MAX_VALUES=100;
		int startPos = Math.max(length - MAX_VALUES, 0);
		length = length - startPos;
		values.add(new double[length]);
		values.add(new double[length]);
		cursor.moveToPosition(startPos);
		endTime = startTime = cursor.getLong(cdIndex);
		int i = 0;
		for (cursor.moveToPosition(startPos); !cursor.isLast(); cursor.moveToNext()) {
			values.get(0)[i] = cursor.getDouble(emIndex);
			values.get(1)[i] = cursor.getDouble(inIndex);
			endTime = cursor.getLong(cdIndex);
			++i;
		}
		int[] colors = new int[] { Color.BLUE, Color.CYAN };
		PointStyle[] styles = new PointStyle[] { PointStyle.POINT,
				PointStyle.POINT };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		// renderer.setOrientation(Orientation.VERTICAL);
		//TODO: get better X axis labels - more meaningful in time
		setChartSettings(renderer, "Emotion over time",
				"Time", "Emotion/intensity", 0, length, 0.0, 1.0, Color.GRAY,
				Color.LTGRAY);
		renderer.setXLabels(12);
		renderer.setYLabels(10);
		renderer.setDisplayChartValues(false);
		length = renderer.getSeriesRendererCount();
		for (i = 0; i < length; i++) {
			XYSeriesRenderer seriesRenderer = (XYSeriesRenderer) renderer
					.getSeriesRendererAt(i);
//			seriesRenderer.setFillBelowLine(i == length - 1);
//			seriesRenderer.setFillBelowLineColor(colors[i]);
		}
		return ChartFactory.getLineChartIntent(context, buildBarDataset(titles,
				values), renderer);
	}

}
