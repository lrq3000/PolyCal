package org.lrq3000.polycalx;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

public class PolyCalXWidgetProvider extends AppWidgetProvider {
    public static final String RELOAD_EVENTS = "org.lrq3000.polycalx.RELOAD_EVENTS";
    public static final String CHANGE_SOURCE = "org.lrq3000.polycalx.CHANGE_SOURCE";
    public static final String LAUNCH_CALENDAR = "org.lrq3000.polycalx.LAUNCH_CALENDAR";
    public static final String EVENT_ID = "org.lrq3000.polycalx.EVENT_ID";
    public static final String EVENT_BEGIN = "org.lrq3000.polycalx.EVENT_BEGIN";
    private static final String TAG = "PolyCalXWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        boolean calendar_permissions = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "CheckCalendarPermission() = " + calendar_permissions );

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int widgetId = appWidgetIds[i];
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget);

            Intent view_intent = null;
            if (calendar_permissions && ! CheckScreenshotMode(context, widgetId)) {
                Log.d(TAG, "wID " + widgetId + " is in Calendar mode");
                view_intent = new Intent(context, CalendarRemoteViewsService.class);
                view_intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                view_intent.setData(Uri.parse(view_intent.toUri(Intent.URI_INTENT_SCHEME)));
                LogIntent("onUpdate(calendar) view_intent", view_intent);

                // Launch Calendar when clicked
                Intent launch_calendar_intent = new Intent(context, PolyCalXWidgetProvider.class);
                launch_calendar_intent.setAction(PolyCalXWidgetProvider.LAUNCH_CALENDAR);
                launch_calendar_intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
                LogIntent("onUpdate(calendar) launch_calendar_intent", launch_calendar_intent);

                PendingIntent launchCalPendingIntent = PendingIntent.getBroadcast(context, 0, launch_calendar_intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                remoteViews.setPendingIntentTemplate(R.id.listview, launchCalPendingIntent);

            } else {
                Log.d(TAG, "wID " + widgetId + " is in Screenshot mode");
                view_intent = new Intent(context, ScreenshotRemoteViewsService.class);
                view_intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                LogIntent("onUpdate(screenshot) view_intent", view_intent);

                //  Launch Setting when clicked
                Intent settings_intent = new Intent(context, SettingsActivity.class);
                settings_intent.setData(Uri.parse("wid://" + widgetId)); // Ensures uniqueness when creating PendingIntent
                settings_intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                settings_intent.putExtra("from", "Layout.UserClick");
                LogIntent("onUpdate(screenshot) settings_intent", settings_intent);
                Log.d(TAG, "created settings_intent for wID=" + widgetId);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, settings_intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
                remoteViews.setPendingIntentTemplate(R.id.listview, pendingIntent);
            }

            remoteViews.setRemoteAdapter(R.id.listview, view_intent);
            remoteViews.setEmptyView(R.id.listview, R.id.empty_view);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        Log.d(TAG, "End of OnUpdate()");
    }

    private boolean CheckScreenshotMode(Context context, int widget_id) {
        String pref_file_name = String.format("org.lrq3000.polycalx.prefs_for_widget_%d", widget_id);
        Log.d(TAG, "Checking screenshot_mode in preference file " + pref_file_name);
        return context.getSharedPreferences(pref_file_name, 0).getBoolean("screenshot_mode", true);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        Log.d(TAG, "onReceive() -> " + intent.toString() );
        if( intent.getAction() == RELOAD_EVENTS){
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] allWidgetIDs = appWidgetManager.getAppWidgetIds( new ComponentName(context, this.getClass()) );
            appWidgetManager.notifyAppWidgetViewDataChanged(allWidgetIDs, R.id.listview);
        }
        if( intent.getAction() == CHANGE_SOURCE){
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] allWidgetIDs = appWidgetManager.getAppWidgetIds( new ComponentName(context, this.getClass()) );
            onUpdate(context, appWidgetManager, allWidgetIDs);
        }
        if( intent.getAction() == LAUNCH_CALENDAR){
            //int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            long event_begin = intent.getLongExtra(EVENT_BEGIN, 0);

            Log.d(TAG, "Launching calendar at timestamp " + event_begin);

            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            ContentUris.appendId(builder, event_begin);
            Intent eventbegin_intent = new Intent(Intent.ACTION_VIEW);
            eventbegin_intent.setData(builder.build());
            eventbegin_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(eventbegin_intent);
/*
            // Open a specific event, rather than time
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event_id);
            Intent event_intent = new Intent(Intent.ACTION_VIEW);
            event_intent.setData(uri);
            event_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(event_intent);
 */
        }

        //LogIntent("onReceive()", intent);

        super.onReceive(context, intent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds){
        for(int i=0; i<appWidgetIds.length; ++i) {
            Log.d(TAG, "Removed wID " + appWidgetIds[i] + ". Deleting preferences file.");
            String pref_file_name = String.format("org.lrq3000.polycalx.prefs_for_widget_%d", appWidgetIds[i]);
            context.deleteSharedPreferences(pref_file_name);
        }
        super.onDisabled(context);
    }


    public void LogIntent(String extra_tag, Intent intent){
        Log.d(TAG, extra_tag + " -> " + intent.toString() );
        for (String key : intent.getExtras().keySet())
            Log.d(TAG, "(extra) " + key + " = " + intent.getExtras().get(key));
    }

}