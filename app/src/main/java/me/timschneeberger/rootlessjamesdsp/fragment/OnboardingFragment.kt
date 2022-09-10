package me.timschneeberger.rootlessjamesdsp.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import me.timschneeberger.hiddenapi_impl.ShizukuSystemServerApi
import me.timschneeberger.hiddenapi_impl.UserHandle
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.MainActivity
import me.timschneeberger.rootlessjamesdsp.databinding.OnboardingFragmentBinding
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.isPackageInstalled
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.launchApp
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.openPlayStoreApp
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.loadHtml
import rikka.shizuku.Shizuku
import timber.log.Timber


class OnboardingFragment : Fragment() {
    private lateinit var container: ViewGroup

    private val pageMap = mapOf(
        PAGE_WELCOME                to R.id.onboarding_page1,
        PAGE_LIMITATIONS            to R.id.onboarding_page2,
        PAGE_ADB_SETUP              to R.id.onboarding_page3,
        PAGE_RUNTIME_PERMISSIONS    to R.id.onboarding_page4,
        PAGE_SELF_CHECK             to R.id.onboarding_page5,
        PAGE_READY                  to R.id.onboarding_page6,
    )

    private var currentPage = PAGE_WELCOME

    private lateinit var backButton: Button
    private lateinit var nextButton: Button
    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<Array<String>>

    private var shizukuAlive = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Timber.tag(TAG).d("Shizuku binder received")
        shizukuAlive = true
    }
    private val binderDeadListener =  Shizuku.OnBinderDeadListener {
        Timber.tag(TAG).d("Shizuku binder died")
        shizukuAlive = false
    }
    private val requestPermissionResultListener = OnRequestPermissionResult()

    private lateinit var binding: OnboardingFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runtimePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.all { it.value }) {
                // This callback is async, call goToPage directly from here
                if(currentPage == PAGE_RUNTIME_PERMISSIONS)
                {
                    changePage(forward = true, ignoreConditions = true)
                }
            } else {
                requireContext().showAlert(R.string.onboarding_perm_missing_title,
                    R.string.onboarding_perm_missing)
            }
        }
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?
    ): View {
        binding = OnboardingFragmentBinding.inflate(layoutInflater, viewGroup, false)
        return binding.root
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        val controlsLayout = view.findViewById<View>(R.id.controls_layout)
        container = view.findViewById(R.id.onboarding_container)
        backButton = controlsLayout.findViewById(R.id.back_button)
        nextButton = controlsLayout.findViewById(R.id.next_button)
        backButton.setOnClickListener { changePage(false) }
        nextButton.setOnClickListener { changePage(true) }

        // Limitation page
        val limitations = view.findViewById<TextView>(R.id.onboarding_limitations)
        limitations.text = loadHtml(getString(R.string.onboarding_limitations))

        // ADB permission page
        val adbPage = view.findViewById<View>(R.id.onboarding_page3)
        adbPage.findViewById<Button>(R.id.onboarding_adb_shizuku_install_button).setOnClickListener {
            // Open Shizuku play store page
            viewShizukuInMarket()
        }
        adbPage.findViewById<Button>(R.id.onboarding_adb_shizuku_open_button).setOnClickListener {
            // Launch Shizuku
            if(!requireContext().launchApp(SHIZUKU_PKG))
            {
                requestShizukuInstallation()
            }
        }
        adbPage.findViewById<Button>(R.id.onboarding_adb_shizuku_grant_button).setOnClickListener {
            if(!requireContext().isPackageInstalled(SHIZUKU_PKG))
            {
                requestShizukuInstallation()
                return@setOnClickListener
            }

            if(!shizukuAlive)
            {
                requireContext().showAlert(R.string.onboarding_adb_shizuku_grant_fail_server_dead_title,
                    R.string.onboarding_adb_shizuku_grant_fail_server_dead)
                return@setOnClickListener
            }

            if(Shizuku.isPreV11())
            {
                val alert = MaterialAlertDialogBuilder(requireContext())
                alert.setMessage(getString(R.string.onboarding_adb_shizuku_grant_fail_version))
                alert.setTitle(getString(R.string.onboarding_adb_shizuku_grant_fail_version_title))
                alert.setPositiveButton(getString(R.string.update)) { _, _ -> viewShizukuInMarket() }
                alert.setNegativeButton(android.R.string.ok, null)
                alert.create().show()
                return@setOnClickListener
            }

            if(checkPermission(REQUEST_CODE_SHIZUKU_GRANT))
            {
                // Permission already granted
                changePage(true)
            }
        }

        adbPage.findViewById<Button>(R.id.onboarding_adb_manual_button).setOnClickListener {
            requireContext().showAlert(R.string.onboarding_adb_manual_title,
                R.string.onboarding_adb_manual_detail_instruction)
        }

        bundle?.let { goToPage(it.getInt("currentPage")) }

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("currentPage", currentPage)
        super.onSaveInstanceState(outState)
    }

    private fun requestShizukuInstallation(){
        val alert = MaterialAlertDialogBuilder(requireContext())
        alert.setMessage(getString(R.string.onboarding_adb_shizuku_not_installed))
        alert.setTitle(getString(R.string.onboarding_adb_shizuku_not_installed_title))
        alert.setPositiveButton(getString(R.string.install)) { _, _ -> viewShizukuInMarket() }
        alert.setNegativeButton(android.R.string.cancel, null)
        alert.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    fun onBackPressed(): Boolean {
        if (!isFirstPage) {
            changePage(false)
            return true
        }
        return false
    }

    private fun checkPermission(code: Int): Boolean {
        if (Shizuku.isPreV11()) {
            return false
        }
        try {
            return if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
                true
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                showDeniedMessage()
                false
            } else {
                Shizuku.requestPermission(code)
                false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        showDeniedMessage()
        return false
    }

    private fun showDeniedMessage(){
        requireContext().showAlert(R.string.onboarding_adb_shizuku_grant_fail_denied_title,
            R.string.onboarding_adb_shizuku_grant_fail_denied)
    }

    private fun viewShizukuInMarket(){
        requireContext().openPlayStoreApp(SHIZUKU_PKG)
    }

    private inner class OnRequestPermissionResult : Shizuku.OnRequestPermissionResultListener
    {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (grantResult == PERMISSION_GRANTED) {
                when (requestCode) {
                    REQUEST_CODE_SHIZUKU_GRANT -> {
                        changePage(true)
                    }
                }
            } else {
                showDeniedMessage()
            }
        }
    }

    private fun goToPage(number: Int)
    {
        // Check if we're finished
        if(number > pageMap.count())
        {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_FORCE_SHOW_CAPTURE_PROMPT, true)
            startActivity(intent)
            requireActivity().finish()
            
            // Mark setup as done
            requireContext()
                .getSharedPreferences(Constants.PREF_VAR, AppCompatActivity.MODE_PRIVATE)
                .edit()
                .putBoolean(getString(R.string.key_firstboot), false)
                .apply()
            return
        }

        if(number == PAGE_ADB_SETUP) {
            updateAdbInstructions()
        }
        else if(number == PAGE_RUNTIME_PERMISSIONS) {
            val pageBinding = binding.onboardingPage4
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                pageBinding.requireViewById<View>(R.id.onboarding_page4_notification).visibility = View.GONE
            }
        }

        val prev = pageMap[currentPage]
        val next = pageMap[number]
        if(prev == null || next == null) {
            Timber.tag(TAG).e("Unknown page resource (prev=$prev; next=$next)")
            return
        }

        val prevView = view?.findViewById<View>(prev)
        val nextView = view?.findViewById<View>(next)
        if(prevView == null || nextView == null) {
            Timber.tag(TAG).e("View is null (prev=$prev; next=$next)")
            return
        }

        // Change the visibility of the start and end views, animating using a shared axis transition.
        val sharedAxis = createTransition(currentPage <= number, prevView, nextView)
        TransitionManager.beginDelayedTransition(container, sharedAxis)

        prevView.visibility = View.GONE
        nextView.visibility = View.VISIBLE

        currentPage = number

        backButton.isEnabled = !isFirstPage
        nextButton.text = getString(if (isLastPage) R.string.onboarding_finish else R.string.onboarding_next)
    }

    private fun changePage(forward: Boolean, ignoreConditions: Boolean = false) {

        val nextIndex = if (forward) requestNextPage(currentPage + 1, forward) else requestNextPage(currentPage - 1, forward)
        if(nextIndex < 1) {
            Timber.tag(TAG).w("Page index out of range ($nextIndex)")
            return
        }

        if(forward && !canAccessNextPage(currentPage) && !ignoreConditions) {
            Timber.tag(TAG).d("Next page not ready; instructions not yet fulfilled by the user")
            return
        }

        goToPage(nextIndex)
    }

    private fun requestNextPage(nextPage: Int, forward: Boolean): Int
    {
        var shouldSkip = when (nextPage) {
            PAGE_ADB_SETUP -> requireContext().checkSelfPermission(DUMP_PERM) == PERMISSION_GRANTED
            PAGE_RUNTIME_PERMISSIONS -> {
                val notificationGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
                else requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PERMISSION_GRANTED
                notificationGranted && requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED
            }
            PAGE_SELF_CHECK -> true /* FIXME skip dummy page */
            else -> false
        }

        Timber.tag(TAG).d("requestNextPage: shouldSkip $nextPage? $shouldSkip")

        if(!shouldSkip)
            return nextPage

        return requestNextPage(if(forward) nextPage + 1 else nextPage - 1, forward)
    }

    private fun canAccessNextPage(currentPage: Int): Boolean
    {
        return when (currentPage) {
            PAGE_ADB_SETUP -> ensureDumpPermission()
            PAGE_RUNTIME_PERMISSIONS -> ensureRuntimePermissions()
            else -> true
        }
    }

    private fun ensureDumpPermission(): Boolean{
        // Permission already granted?
        if(requireContext().checkSelfPermission(DUMP_PERM) == PERMISSION_GRANTED) {
            Timber.tag(TAG).d("DUMP permission granted")
            return true
        }

        // If not, use Shizuku to grant it if connected
        if(shizukuAlive && Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
            val pkg = requireContext().packageName
            Timber.tag(TAG)
                .d("Granting $DUMP_PERM via Shizuku (uid ${Shizuku.getUid()}) for $pkg")

            // Grant DUMP as system
            ShizukuSystemServerApi.PermissionManager_grantRuntimePermission(pkg, DUMP_PERM, UserHandle.USER_SYSTEM)

            // Re-check permission
            return if (requireContext().checkSelfPermission(DUMP_PERM) == PERMISSION_GRANTED) {
                Timber.tag(TAG).d("DUMP permission via Shizuku granted")
                true
            } else {
                Timber.tag(TAG).e("$DUMP_PERM not granted")
                requireContext().showAlert(R.string.onboarding_adb_shizuku_no_dump_perm_title,
                    R.string.onboarding_adb_shizuku_no_dump_perm)

                // Fallback just in case
                @Suppress("DEPRECATION")
                val proc = Shizuku.newProcess(arrayOf<String>("pm", "grant", pkg, DUMP_PERM), null, null)
                proc.waitFor()
                false
            }
        }

        requireContext().showAlert(R.string.onboarding_adb_dump_not_granted_title,
            R.string.onboarding_adb_dump_not_granted)
        return false
    }

    private fun ensureRuntimePermissions(): Boolean
    {
        val requestedPermissions = arrayListOf<String>()
        if(requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_DENIED) {
            requestedPermissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PERMISSION_DENIED) {
            requestedPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return if(requestedPermissions.isNotEmpty()) {
            runtimePermissionLauncher.launch(requestedPermissions.toTypedArray())
            false
        }
        else {
            true
        }
    }

    private fun updateAdbInstructions() {
        val pageBinding = binding.onboardingPage3Include

        val installed = requireContext().isPackageInstalled(SHIZUKU_PKG)

        pageBinding.onboardingAdbShizukuInstallLayout.visibility = if(installed) View.GONE else View.VISIBLE
        pageBinding.onboardingAdbShizukuOpenLayout.visibility = if(installed && shizukuAlive) View.GONE else View.VISIBLE

        // Only one instruction, hide 'step X' header
        pageBinding.onboardingAdbShizukuGrantHeader.visibility = if(installed && shizukuAlive) View.GONE else View.VISIBLE

        // Install instruction hidden
        if(installed && !shizukuAlive) {
            pageBinding.onboardingAdbShizukuOpenHeader.text = getString(R.string.onboarding_adb_step_1)
            pageBinding.onboardingAdbShizukuGrantHeader.text = getString(R.string.onboarding_adb_step_2)
        }
        // All instructions shown
        else {
            pageBinding.onboardingAdbShizukuOpenHeader.text = getString(R.string.onboarding_adb_step_2)
            pageBinding.onboardingAdbShizukuGrantHeader.text = getString(R.string.onboarding_adb_step_3)
        }

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            pageBinding.onboardingAdbShizukuLayout.isVisible = false
            pageBinding.onboardingAdbManualHeader.text = getString(R.string.onboarding_adb_android10_header)
            pageBinding.onboardingAdbManualContent.text = getString(R.string.onboarding_adb_manual_detail_instruction)
            pageBinding.onboardingAdbManualButton.isVisible = false
            pageBinding.onboardingAdbAndroid10Warning.isVisible = true
        }
    }

    private fun createTransition(entering: Boolean, start: View, end: View): MaterialSharedAxis {
        val transition = MaterialSharedAxis(MaterialSharedAxis.X, entering)

        // Add targets for this transition to explicitly run transitions only on these views. Without
        // targeting, a MaterialSharedAxis transition would be run for every view in the
        // the ViewGroup's layout.
        transition.addTarget(start)
        transition.addTarget(end)
        return transition
    }

    private val isFirstPage: Boolean
        get() = currentPage == 1
    private val isLastPage: Boolean
        get() = currentPage == pageMap.count()

    companion object
    {
        fun newInstance() = OnboardingFragment()

        const val TAG = "OnboardingFragment"
        const val DUMP_PERM = "android.permission.DUMP"
        const val SHIZUKU_PKG = "moe.shizuku.privileged.api"

        const val REQUEST_CODE_SHIZUKU_GRANT = 1

        const val PAGE_WELCOME = 1
        const val PAGE_LIMITATIONS = 2
        const val PAGE_ADB_SETUP = 3
        const val PAGE_RUNTIME_PERMISSIONS = 4
        const val PAGE_SELF_CHECK = 5
        const val PAGE_READY = 6
    }
}