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

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tembem.android.memomjm.app.data.MemoContract;
import com.tembem.android.memomjm.app.data.MemoContract.MemoEntry;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";

    private static final String FORECAST_SHARE_HASHTAG = " #mjmyamaha";

    private ShareActionProvider mShareActionProvider;
    private String mForecast;
    private Uri mUri;
    ProgressDialog dialog2 = null;
    private Bitmap bitmap;

    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {
            MemoContract.MemoEntry.TABLE_NAME + "." + MemoContract.MemoEntry._ID,
            MemoContract.MemoEntry.COLUMN_RECEIPT_ID,
            MemoContract.MemoEntry.COLUMN_DATE,
            MemoContract.MemoEntry.COLUMN_SHORT_DESC,
            MemoContract.MemoEntry.COLUMN_ENGINE,
            MemoContract.MemoEntry.COLUMN_CHASIS,
            MemoContract.MemoEntry.COLUMN_IMAGE1
    };

    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
    // must change.
    static final int COL_MEMO_ID = 0;
    static final int COL_MEMO_RECEIPT_ID = 1;
    static final int COL_MEMO_DATE = 2;
    static final int COL_MEMO_DESC = 3;
    static final int COL_MEMO_ENGINE = 4;
    static final int COL_MEMO_CHASIS = 5;
    static final int COL_MEMO_IMAGE1 = 6;

    private ImageView mIconView;
    private TextView mDateView;
    private TextView mCustomerView;
    private TextView mEngineView;
    private TextView mChasisView;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        //mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mCustomerView = (TextView) rootView.findViewById(R.id.detail_customer_textview);
        mEngineView = (TextView) rootView.findViewById(R.id.detail_engine_textview);
        mChasisView = (TextView) rootView.findViewById(R.id.detail_chasis_textview);

        Button button1 = (Button)rootView.findViewById(R.id.button_show_image1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PictureActivity.class);
                intent.putExtra(DetailFragment.DETAIL_URI, mUri);
                startActivityForResult(intent, 110);
            }
        });

        ImageView image1 = (ImageView)rootView.findViewById(R.id.thumbnail_image1);
        loadImage("http://192.168.1.3/androiduploadbasic/uploads/101.jpg", image1);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            mUri = data.getParcelableExtra(DetailFragment.DETAIL_URI);
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mForecast != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged(String newLocation) {
        // replace the uri, since the location has changed
//        Uri uri = mUri;
//        if (null != uri) {
//            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
//            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
//            mUri = updatedUri;
//            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
//        }
        getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ( null != mUri ) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            // Read weather condition ID from cursor
            //int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);

            // Use weather art image
            //mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

            // Read date from cursor and update views for day of week and date
            String dateText = data.getString(COL_MEMO_DATE);
            //String dateText = Utility.getFormattedMonthDay(getActivity(), date);
            mDateView.setText(dateText);

            // Read description from cursor and update view
            String description = data.getString(COL_MEMO_DESC);
            mCustomerView.setText(description);

            // For accessibility, add a content description to the icon field
            //mIconView.setContentDescription(description);

            String chasis = data.getString(COL_MEMO_CHASIS);
            mChasisView.setText(chasis);

            String engine = data.getString(COL_MEMO_ENGINE);
            mEngineView.setText(engine);

            // We still need this for the share intent
            mForecast = String.format("%s - %s - %s - %s", dateText, description, chasis, engine);

            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }

    public void loadImage(String url, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        task.execute(url);
    }

    private class BitmapWorkerTask extends AsyncTask<String, String, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog2 = new ProgressDialog(getActivity());
            dialog2.setMessage("Loading Image ....");
            dialog2.show();
        }
        protected Bitmap doInBackground(String... args) {
            try {
                bitmap = BitmapFactory.decodeStream((InputStream)new URL(args[0]).getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }
        protected void onPostExecute(Bitmap image) {
            if(imageViewReference != null && image != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(image);
                }
                dialog2.dismiss();
            } else {
                dialog2.dismiss();
                Toast.makeText(getActivity(), "Image Does Not exist or Network Error", Toast.LENGTH_SHORT).show();
            }
        }
    }
}