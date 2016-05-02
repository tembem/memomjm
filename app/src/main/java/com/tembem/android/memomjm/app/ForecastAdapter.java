package com.tembem.android.memomjm.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    //private static final int VIEW_TYPE_COUNT = 2;

    // Flag to determine if we want to use a separate view for "today".
    //private boolean mUseTodayLayout = true;

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView engineView;
        public final TextView chasisView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            engineView = (TextView) view.findViewById(R.id.list_item_engine_textview);
            chasisView = (TextView) view.findViewById(R.id.list_item_chasis_textview);
        }
    }

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        //int viewType = getItemViewType(cursor.getPosition());
        int layoutId = R.layout.list_item_forecast;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

//        int viewType = getItemViewType(cursor.getPosition());
//        switch (viewType) {
//            case VIEW_TYPE_TODAY: {
//                // Get weather icon
//                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(
//                        cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID)));
//                break;
//            }
//            case VIEW_TYPE_FUTURE_DAY: {
//                // Get weather icon
//                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(
//                        cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID)));
//                break;
//            }
//        }

        viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition());

        // Read date from cursor
        String dateInString = cursor.getString(ForecastFragment.COL_MEMO_DATE);
        // Find TextView and set formatted date on it
        viewHolder.dateView.setText(dateInString);

        // Read weather forecast from cursor
        String description = cursor.getString(ForecastFragment.COL_MEMO_DESC);
        // Find TextView and set weather forecast on it
        viewHolder.descriptionView.setText(description);

        // For accessibility, add a content description to the icon field
        viewHolder.iconView.setContentDescription(description);

        int receiptId = cursor.getInt(ForecastFragment.COL_MEMO_RECEIPT_ID);

        String engine = cursor.getString(ForecastFragment.COL_MEMO_ENGINE);
        viewHolder.engineView.setText(engine);

        String chasis = cursor.getString(ForecastFragment.COL_MEMO_CHASIS);
        viewHolder.chasisView.setText(chasis);

        //String image1 = cursor.getString(ForecastFragment.COL_MEMO_IMAGE1);
        //viewHolder.engineViewView.setText(image1);
    }
}