package me.timschneeberger.rootlessjamesdsp.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.EngineUtils
import me.timschneeberger.rootlessjamesdsp.utils.EngineUtils.toggleEnginePower
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import timber.log.Timber

class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var timeoutRunnable: Runnable? = null

        fun cancelLoadingTimeout() {
            timeoutRunnable?.let {
                handler.removeCallbacks(it)
                Timber.d("Loading timeout cancelled - service responded")
            }
            timeoutRunnable = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Widget action received")
        Timber.d("WidgetActionReceiver: onReceive() triggered")
        when (intent.action) {
            Constants.ACTION_WIDGET_CLICK -> {
                val newState = !EngineUtils.isEngineEnabled(context)
                Timber.d("Alternando el estado del motor: $newState")

                // Mostrar loading en el widget
                if (isRootless()) {
                    updateAllWidgets(context, isLoading = true)

                    // Clear loading state after timeout (5 seconds) as fallback
                    // This will be cancelled if service responds with broadcast
                    cancelLoadingTimeout() // Cancel any previous timeout
                    timeoutRunnable = Runnable {
                        Timber.d("Loading timeout reached, clearing loading state (fallback)")
                        updateAllWidgets(context, isLoading = false)
                        timeoutRunnable = null
                    }
                    handler.postDelayed(timeoutRunnable!!, 5000)
                }

                context.toggleEnginePower(newState)
            }
        }
    }

    private fun updateAllWidgets(context: Context, isLoading: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, EngineToggleWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        for (appWidgetId in appWidgetIds) {
            EngineToggleWidgetProvider.updateWidgetWithState(context, appWidgetManager, appWidgetId, isLoading)
        }
    }
}