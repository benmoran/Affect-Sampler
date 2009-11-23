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
package net.benmoran.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class AffectSampleStore {

	public static final String AUTHORITY = "net.benmoran.provider.AffectSampleStore";

	// This class cannot be instantiated
	private AffectSampleStore() {
	}

	/**
	 * Notes table
	 */
	public static final class AffectSamples implements BaseColumns {
		// This class cannot be instantiated
		private AffectSamples() {
		}

		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/samples");

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.benmoran.affectsamples";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
		 * note.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.benmoran.affectsample";

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "created DESC";

		/**
		 * The emotion of the sample, 0 for misery, 1.0 for bliss
		 * <P>
		 * Type: Double
		 * </P>
		 */
		public static final String EMOTION = "emotion";

		/**
		 * The intensity of the sample, 0 for comatose, 1.0 for frenzy
		 * <P>
		 * Type: Double
		 * </P>
		 */
		public static final String INTENSITY = "intensity";

		/**
		 * Comment on the sample
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String COMMENT = "comment";


		/**
		 * The timestamp for when the note was scheduled
		 * <P>
		 * Type: INTEGER (long from System.currentTimeMillis())
		 * </P>
		 */
		public static final String SCHEDULED_DATE = "scheduled";
		
		/**
		 * The timestamp for when the note was created
		 * <P>
		 * Type: INTEGER (long from System.currentTimeMillis())
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

	}
}
