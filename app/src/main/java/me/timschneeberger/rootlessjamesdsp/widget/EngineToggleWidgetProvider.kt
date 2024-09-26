package me.timschneeberger.rootlessjamesdsp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.EngineUtils
import timber.log.Timber

class EngineToggleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Constants.ACTION_ENGINE_STATE_CHANGED,
            Constants.ACTION_SERVICE_STARTED,
            Constants.ACTION_SERVICE_STOPPED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, EngineToggleWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Timber.d("Updating widget with ID: $appWidgetId")

        val views = RemoteViews(context.packageName, R.layout.widget_engine_toggle)

        val iconResource = if (EngineUtils.isEngineEnabled(context)) {
            R.drawable.ic_engine_on
        } else {
            R.drawable.ic_engine_off
        }
        views.setImageViewResource(R.id.widget_engine_icon, iconResource)

        val intent = Intent(context, WidgetActionReceiver::class.java)
        intent.action = Constants.ACTION_WIDGET_CLICK
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_engine_icon, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}