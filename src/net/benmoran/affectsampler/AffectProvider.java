/*
 * Copyright (C) 2007 The Android Open Source Project
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

package net.benmoran.affectsampler;


import java.util.HashMap;

import net.benmoran.provider.AffectSampleStore;
import net.benmoran.provider.AffectSampleStore.AffectSamples;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to a database of affect samples. Each sample has
 * a timestamp, the two affect axes (positivity and intensity),
 * and optional notes.  
 */
public class AffectProvider extends ContentProvider {

    private static final String TAG = "AffectProvider";

    private static final String DATABASE_NAME = "affect.db";
    private static final int DATABASE_VERSION = 2;
    private static final String SAMPLES_TABLE_NAME = "samples";

    private static HashMap<String, String> sSamplesProjectionMap;

    private static final int SAMPLES = 1;
    private static final int SAMPLE_ID = 2;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + SAMPLES_TABLE_NAME + " ("
                    + AffectSamples._ID + " INTEGER PRIMARY KEY,"
                    + AffectSamples.EMOTION + " double,"
                    + AffectSamples.INTENSITY + " double,"
                    + AffectSamples.COMMENT + " TEXT,"
                    + AffectSamples.SCHEDULED_DATE + " INTEGER,"
                    + AffectSamples.CREATED_DATE + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SAMPLES_TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case SAMPLES:
            qb.setTables(SAMPLES_TABLE_NAME);
            qb.setProjectionMap(sSamplesProjectionMap);
            break;

        case SAMPLE_ID:
            qb.setTables(SAMPLES_TABLE_NAME);
            qb.setProjectionMap(sSamplesProjectionMap);
            qb.appendWhere(AffectSamples._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = AffectSampleStore.AffectSamples.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case SAMPLES:
            return AffectSamples.CONTENT_TYPE;

        case SAMPLE_ID:
            return AffectSamples.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != SAMPLES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (values.containsKey(AffectSampleStore.AffectSamples.CREATED_DATE) == false) {
            values.put(AffectSampleStore.AffectSamples.CREATED_DATE, now);
        }

        // Don't set SCHEDULED_DATE if it is null - that means user manually started the sample
        
        if (values.containsKey(AffectSampleStore.AffectSamples.COMMENT) == false) {
            values.put(AffectSampleStore.AffectSamples.COMMENT, "");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(SAMPLES_TABLE_NAME, AffectSamples.COMMENT, values);
        if (rowId > 0) {
            Uri sampleUri = ContentUris.withAppendedId(AffectSampleStore.AffectSamples.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(sampleUri, null);
            return sampleUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case SAMPLES:
            count = db.delete(SAMPLES_TABLE_NAME, where, whereArgs);
            break;

        case SAMPLE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(SAMPLES_TABLE_NAME, AffectSamples._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case SAMPLES:
            count = db.update(SAMPLES_TABLE_NAME, values, where, whereArgs);
            break;

        case SAMPLE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(SAMPLES_TABLE_NAME, values, AffectSamples._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AffectSampleStore.AUTHORITY, "samples", SAMPLES);
        sUriMatcher.addURI(AffectSampleStore.AUTHORITY, "samples/#", SAMPLE_ID);

        sSamplesProjectionMap = new HashMap<String, String>();
        sSamplesProjectionMap.put(AffectSamples._ID, AffectSamples._ID);
        sSamplesProjectionMap.put(AffectSamples.EMOTION, AffectSamples.EMOTION);
        sSamplesProjectionMap.put(AffectSamples.INTENSITY, AffectSamples.INTENSITY);
        sSamplesProjectionMap.put(AffectSamples.COMMENT, AffectSamples.COMMENT);
        sSamplesProjectionMap.put(AffectSamples.SCHEDULED_DATE, AffectSamples.SCHEDULED_DATE);
        sSamplesProjectionMap.put(AffectSamples.CREATED_DATE, AffectSamples.CREATED_DATE);
    }
}
