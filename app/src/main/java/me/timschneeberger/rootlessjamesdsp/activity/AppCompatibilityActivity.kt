package me.timschneeberger.rootlessjamesdsp.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityAppCompatibilityBinding
import me.timschneeberger.rootlessjamesdsp.fragment.AppCompatibilityFragment
import me.timschneeberger.rootlessjamesdsp.service.AudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.getParcelableAs

class AppCompatibilityActivity : BaseActivity() {
    private lateinit var prefsVar: SharedPreferences

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsVar = getSharedPreferences(Constants.PREF_VAR, Context.MODE_PRIVATE)

        val binding = ActivityAppCompatibilityBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            val uid = intent.extras?.getInt(AudioProcessorService.EXTRA_APP_UID, -1) ?: -1
            val internalCall = intent.extras?.getBoolean(AudioProcessorService.EXTRA_APP_COMPAT_INTERNAL_CALL, false) ?: false
            val projectIntent = intent.extras?.getParcelableAs<Intent>(AudioProcessorService.EXTRA_MEDIA_PROJECTION_DATA)
            if(uid < 0) {
                finish()
                return
            }

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, AppCompatibilityFragment.newInstance(uid, projectIntent, internalCall))
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onPause() {
        super.onPause()
        prefsVar
            .edit()
            .putBoolean(getString(R.string.key_is_app_compat_activity_active), false)
            .apply()
    }

    override fun onDestroy() {
        prefsVar.edit().putBoolean(getString(R.string.key_is_app_compat_activity_active), false)
            .apply()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        prefsVar.edit()
            .putBoolean(getString(R.string.key_is_app_compat_activity_active), true)
            .apply()
    }
}