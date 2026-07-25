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
            Constants.ACTION_SERVICE_STARTED,
            Constants.ACTION_SERVICE_STOPPED -> {
                Timber.d("Widget received service state change: ${intent.action}")

                // Cancel loading timeout since service responded
                WidgetActionReceiver.cancelLoadingTimeout()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, EngineToggleWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                // Update widgets in real-time, clearing loading state
                for (appWidgetId in appWidgetIds) {
                    updateWidgetWithState(context, appWidgetManager, appWidgetId, isLoading = false)
                }
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, isLoading: Boolean = false) {
        updateWidgetWithState(context, appWidgetManager, appWidgetId, isLoading)
    }

    companion object {
        fun updateWidgetWithState(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, isLoading: Boolean = false) {
            Timber.d("Updating widget with ID: $appWidgetId, loading: $isLoading")

            val views = RemoteViews(context.packageName, R.layout.widget_engine_toggle)

            if (isLoading) {
                // Estado de carga
                views.setInt(R.id.widget_engine_icon, "setImageAlpha", 153) // 60% opacity
                views.setViewVisibility(R.id.widget_progress, android.view.View.VISIBLE)
            } else {
                // Estado normal
                views.setInt(R.id.widget_engine_icon, "setImageAlpha", 255) // 100% opacity
                views.setViewVisibility(R.id.widget_progress, android.view.View.GONE)

                val iconResource = if (EngineUtils.isEngineEnabled(context)) {
                    R.drawable.ic_engine_on
                } else {
                    R.drawable.ic_engine_off
                }
                views.setImageViewResource(R.id.widget_engine_icon, iconResource)
            }

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
}