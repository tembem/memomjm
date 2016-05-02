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
package com.tembem.android.memomjm.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

import com.tembem.android.memomjm.app.data.MemoContract;
import com.tembem.android.memomjm.app.data.MemoContract.MemoEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

public class FetchMemoTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchMemoTask.class.getSimpleName();

    private final Context mContext;

    public FetchMemoTask(Context context) {
        mContext = context;
    }

    private boolean DEBUG = true;

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getMemoDataFromJson(String forecastJsonStr,
                                            String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_DATE = "date";

        final String OWM_LIST = "list";

        final String OWM_RECEIPT_ID = "receipt_id";
        final String OWM_CUSTOMER = "customer";
        final String OWM_ENGINE = "engine";
        final String OWM_CHASIS = "chasis";
        final String OWM_IMAGE1 = "image1";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray memoArray = forecastJson.getJSONArray(OWM_LIST);

                        // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(memoArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for(int i = 0; i < memoArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;

                String description;
                String engine;
                String chasis;
                String image1;
                int receiptId;
                String date;

                // Get the JSON object representing the day
                JSONObject dayForecast = memoArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);

                date = dayForecast.getString(OWM_DATE);
                description = dayForecast.getString(OWM_CUSTOMER);
                engine = dayForecast.getString(OWM_ENGINE);
                chasis = dayForecast.getString(OWM_CHASIS);
                image1 = dayForecast.getString(OWM_IMAGE1);
                receiptId = dayForecast.getInt(OWM_RECEIPT_ID);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(MemoEntry.COLUMN_RECEIPT_ID, receiptId);
                weatherValues.put(MemoEntry.COLUMN_DATE, date);
                weatherValues.put(MemoEntry.COLUMN_ENGINE, engine);
                weatherValues.put(MemoEntry.COLUMN_CHASIS, chasis);
                weatherValues.put(MemoEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(MemoEntry.COLUMN_IMAGE1, image1);

                cVVector.add(weatherValues);
            }

            // Add to database
            int inserted = 0;
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = mContext.getContentResolver().bulkInsert(MemoEntry.CONTENT_URI, cvArray);
            }

            Log.d(LOG_TAG, "FetchMemoTask Complete. " + inserted + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(String... params) {

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (params.length == 0) {
            return null;
        }
        String query = params[0];

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String receiptIdParam = "123";

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            final String FORECAST_BASE_URL =
                    Utility.MJM_API_URL + "memo?";

            final String RECEIPT_PARAM = "q";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(RECEIPT_PARAM, query)
                    .build();

            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, "URI: " + builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
            getMemoDataFromJson(forecastJsonStr, query);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }
}