package me.timschneeberger.rootlessjamesdsp.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import me.timschneeberger.rootlessjamesdsp.activity.AeqSelectorActivity

class AutoEqSelectorContract : ActivityResultContract<Int, String?>() {

    /* input unused */
    override fun createIntent(context: Context, input: Int): Intent {
        return Intent(context, AeqSelectorActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? = when {
        resultCode != Activity.RESULT_OK -> null
        else -> intent?.getStringExtra(EXTRA_RESULT) ?: ""
    }

    companion object {
        const val EXTRA_RESULT = "result"
    }
}