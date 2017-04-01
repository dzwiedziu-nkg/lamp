package pl.nkg.mm.switcher;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


public class LightSwitchWidget extends AppWidgetProvider {

    public static String EXTRA_WIDGET_ID = "widget_id";
    public static boolean state = false;


    static protected PendingIntent getPendingSelfIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, LightSwitchReceiver.class);
        intent.putExtra(EXTRA_WIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.light_switch_widget);
        views.setOnClickPendingIntent(R.id.widget_image, getPendingSelfIntent(context, appWidgetId));
        views.setImageViewResource(R.id.widget_image, state ? R.drawable.ic_light_on : R.drawable.ic_light_off);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            LightSwitchReceiver.sendCommand(context, LightSwitchWidget.state ? 0 : 1, appWidgetId);
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
