package me.timschneeberger.rootlessjamesdsp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityOnboardingBinding
import me.timschneeberger.rootlessjamesdsp.fragment.OnboardingFragment
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert


class OnboardingActivity : AppCompatActivity(){
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var fragment: OnboardingFragment

    override fun onCreate(savedInstanceState:Bundle?)
    {
        WindowCompat.setDecorFitsSystemWindows(window,false)
        super.onCreate(savedInstanceState)

        fragment = if (savedInstanceState != null) {
            supportFragmentManager.getFragment(savedInstanceState, "onboarding") as OnboardingFragment
        } else {
            OnboardingFragment.newInstance()
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.onboarding_fragment_container, fragment)
            .commit()

        // Request to fix permissions using the wizard
        if(intent.getBooleanExtra(EXTRA_FIX_PERMS, false)){
            showAlert(R.string.onboarding_fix_permissions_title, R.string.onboarding_fix_permissions)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        supportFragmentManager.putFragment(outState, "onboarding", fragment)
    }

    private fun navigateUp(): Boolean
    {
        val finished = !fragment.onBackPressed()
        if(finished)
        {
            this.finishAffinity()
        }
        return finished
    }

    override fun onBackPressed() {
        navigateUp()
    }

    override fun onSupportNavigateUp(): Boolean
    {
        return navigateUp()
    }

    companion object
    {
        const val TAG = "OnboardingActivity"
        const val EXTRA_FIX_PERMS = "FixPermissions"
    }
}