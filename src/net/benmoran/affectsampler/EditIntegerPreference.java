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

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class EditIntegerPreference extends EditTextPreference {
	public EditIntegerPreference(Context context) {
		super(context);
	}

	public EditIntegerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditIntegerPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public String getText() {
		return String.valueOf(getSharedPreferences().getInt(getKey(), 0));
	}

	@Override
	public void setText(String text) {
		getSharedPreferences().edit().putInt(getKey(), Integer.parseInt(text))
				.commit();
	}
}