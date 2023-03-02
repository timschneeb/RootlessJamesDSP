package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.Translator
import java.util.Locale


class SettingsAboutFragment : PreferenceFragmentCompat() {

    private val version by lazy { findPreference<Preference>(getString(R.string.key_credits_version)) }
    private val buildInfo by lazy { findPreference<Preference>(getString(R.string.key_credits_build_info)) }
    private val translatorsGroup by lazy { findPreference<PreferenceGroup>(getString(R.string.key_translators)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_about_preferences, rootKey)

        version?.summary = BuildConfig.VERSION_NAME
        buildInfo?.isVisible = BuildConfig.DEBUG || BuildConfig.PREVIEW
        val type = if(BuildConfig.PREVIEW)
            "Preview"
        else if(BuildConfig.DEBUG)
            "Debug"
        else
            "Release"

        buildInfo?.summary = "$type build @${BuildConfig.COMMIT_SHA} (${BuildConfig.FLAVOR_dependencies}) (compiled at ${BuildConfig.BUILD_TIME})"

        Translator.readLanguageMap(requireContext()).forEach { (cc, tls) ->
            translatorsGroup?.addPreference(Preference(requireContext()).apply {
                val language = Locale.forLanguageTag(cc).getDisplayLanguage(requireContext().resources.configuration.locales[0])
                val region = Locale.forLanguageTag(cc).getDisplayCountry(requireContext().resources.configuration.locales[0])

                isIconSpaceReserved = false
                title = if(region.isNullOrBlank()) language else "$language ($region)"
                summary = tls.joinToString(", ") { it.name }

                setOnPreferenceClickListener {
                    if(tls.size == 1)
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/profile/${tls[0].user}")))
                    else {
                        MaterialAlertDialogBuilder(requireContext())
                            .setItems(tls.map { it.name }.toTypedArray()) { dialogInterface, i ->
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/profile/${tls[i].user}")))
                                dialogInterface.dismiss()
                            }
                            .setTitle(title)
                            .setNegativeButton(getString(android.R.string.cancel)){ _, _ -> }
                            .create()
                            .show()
                    }
                    true
                }
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val a = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.windowBackground, a, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && a.isColorType) {
            view.setBackgroundColor(a.data)
        } else {
            view.background = ResourcesCompat.getDrawable(requireContext().resources, a.resourceId, requireContext().theme)
        }
        return view
    }

    companion object {
        fun newInstance(): SettingsAboutFragment {
            return SettingsAboutFragment()
        }
    }
}