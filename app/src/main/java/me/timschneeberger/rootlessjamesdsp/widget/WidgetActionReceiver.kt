package me.timschneeberger.rootlessjamesdsp.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.EngineUtils
import me.timschneeberger.rootlessjamesdsp.utils.EngineUtils.toggleEnginePower // <-- Correct import
import timber.log.Timber

class WidgetActionReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Widget action received")
        Timber.d("WidgetActionReceiver: onReceive() triggered") // Add this log
        when (intent.action) {
            Constants.ACTION_WIDGET_CLICK -> {  // Maneja la acción del widget
                Timber.d("Alternando el estado del motor: ${!EngineUtils.isEngineEnabled(context)}")
                context.toggleEnginePower(!EngineUtils.isEngineEnabled(context))

                // Notificar al servicio sobre el cambio de estado
                val updateIntent = Intent(Constants.ACTION_ENGINE_STATE_CHANGED)
                context.sendBroadcast(updateIntent)  // Utilizar sendBroadcast()
            }
        }
    }
}