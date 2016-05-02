/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.tembem.android.memomjm.app.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class MemoProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private MemoDbHelper mOpenHelper;

    static final int MEMO = 100;
    static final int MEMO_WITH_RECEIPT_ID = 101;

    private static final SQLiteQueryBuilder sMemoByReceiptIdQueryBuilder;

    static {
        sMemoByReceiptIdQueryBuilder = new SQLiteQueryBuilder();
        sMemoByReceiptIdQueryBuilder.setTables(MemoContract.MemoEntry.TABLE_NAME);
    }

    private static final String sReceiptSelection = MemoContract.MemoEntry.COLUMN_RECEIPT_ID + " = ? ";

    // Cari memo dengan receipt_id (waktu klik list item memo-memo)
    private Cursor getMemoByReceiptId(Uri uri, String[] projection, String sortOrder) {
        String receiptId = MemoContract.MemoEntry.getReceiptIdFromUri(uri);

        String[] selectionArgs;
        String selection;

        selection = sReceiptSelection;
        selectionArgs = new String[]{ receiptId };

        return sMemoByReceiptIdQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the WEATHER, WEATHER_WITH_LOCATION, WEATHER_WITH_LOCATION_AND_DATE,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MemoContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, MemoContract.PATH_MEMO, MEMO);
        matcher.addURI(authority, MemoContract.PATH_MEMO + "/*", MEMO_WITH_RECEIPT_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MemoDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case MEMO:
                return MemoContract.MemoEntry.CONTENT_TYPE;
            case MEMO_WITH_RECEIPT_ID:
                return MemoContract.MemoEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {

            // "memo"
            case MEMO: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MemoContract.MemoEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            // "memo/*"
            case MEMO_WITH_RECEIPT_ID: {
                retCursor = getMemoByReceiptId(uri, projection, sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case MEMO: {
                //normalizeDate(values);
                long _id = db.insert(MemoContract.MemoEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = MemoContract.MemoEntry.buildMemoUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case MEMO:
                rowsDeleted = db.delete(
                        MemoContract.MemoEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

//    private void normalizeDate(ContentValues values) {
//        // normalize the date value
//        if (values.containsKey(MemoContract.MemoEntry.COLUMN_DATE)) {
//            long dateValue = values.getAsLong(MemoContract.MemoEntry.COLUMN_DATE);
//            values.put(MemoContract.MemoEntry.COLUMN_DATE, MemoContract.normalizeDate(dateValue));
//        }
//    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case MEMO:
                //normalizeDate(values);
                rowsUpdated = db.update(MemoContract.MemoEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MEMO:
                db.beginTransaction();

                db.delete(MemoContract.MemoEntry.TABLE_NAME, null, null);

                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        //normalizeDate(value);
                        long _id = db.insert(MemoContract.MemoEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
                //throw new UnsupportedOperationException("Unknown bulk insert: " + uri);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}