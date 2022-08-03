package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.adapter.AppBlocklistAdapter
import me.timschneeberger.rootlessjamesdsp.databinding.FragmentBlocklistBinding
import me.timschneeberger.rootlessjamesdsp.model.AppInfo
import me.timschneeberger.rootlessjamesdsp.model.ItemViewModel
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistViewModel
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistViewModelFactory
import me.timschneeberger.rootlessjamesdsp.model.room.BlockedApp
import me.timschneeberger.rootlessjamesdsp.session.SessionRecordingPolicyManager
import me.timschneeberger.rootlessjamesdsp.session.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.session.dump.provider.AudioPolicyServiceDumpProvider
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getAppIcon
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getAppNameFromUid
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getAppNameFromUidSafe
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showYesNoAlert
import me.timschneeberger.rootlessjamesdsp.utils.Utils.loadHtml
import kotlin.coroutines.CoroutineContext

class BlocklistFragment : Fragment() {

    private lateinit var binding: FragmentBlocklistBinding
    private lateinit var appsListFragment: AppsListFragment
    private lateinit var adapter: AppBlocklistAdapter
    private val viewModel: AppBlocklistViewModel by viewModels {
        AppBlocklistViewModelFactory((requireActivity().application as MainApplication).blockedAppRepository)
    }
    private val appListViewModel: ItemViewModel<AppInfo> by viewModels()

    private val iconCache: HashMap<Int, Drawable> = hashMapOf()

    private lateinit var sessionRecordingPolicyManager: SessionRecordingPolicyManager
    private val policyPollingScope = CoroutineScope(Dispatchers.Main)
    private var restrictedApps = arrayOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBlocklistBinding.inflate(layoutInflater, container, false)

        sessionRecordingPolicyManager = SessionRecordingPolicyManager(requireContext())

        appsListFragment = AppsListFragment()
        appListViewModel.selectedItem.observe(viewLifecycleOwner) { item ->
            // TODO merge BlockedApp and AppInfo?
            viewModel.insert(BlockedApp(item.uid, item.packageName, item.appName))
        }

        adapter = AppBlocklistAdapter()
        adapter.setOnItemClickListener {
            requireContext().showYesNoAlert(R.string.blocklist_delete_title, R.string.blocklist_delete) { confirm ->
                if(confirm)
                    viewModel.delete(it)
            }
        }

        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())

        binding.notice.setOnClickListener {
            requireContext().showAlert(
                getString(R.string.blocklist_unsupported_apps),
                loadHtml(getString(R.string.blocklist_unsupported_apps_message, restrictedApps.joinToString("<br/>")))
            )
        }
        updateUnsupportedApps()

        viewModel.blockedApps.observe(requireActivity()) { apps ->
            val isEmpty = apps == null || apps.isEmpty()

            // Update the cached copy of the blocked apps in the adapter.
            apps?.let { list ->
                list.forEach {
                    it.appIcon = if(iconCache.containsKey(it.uid))
                        iconCache[it.uid]
                    else {
                        it.packageName?.let { pkg ->
                            val icon = requireContext().getAppIcon(pkg)
                            icon?.let { i ->
                                iconCache[it.uid] = i
                            }
                            icon
                        }
                    }
                }
                adapter.submitList(list.sortedBy { it.appName })
            }
            binding.recyclerview.isVisible = !isEmpty
            binding.emptyview.isVisible = isEmpty
        }

        return binding.root
    }

    override fun onDestroyView() {
        sessionRecordingPolicyManager.destroy()
        super.onDestroyView()
    }

    override fun onResume() {
        updateUnsupportedApps()
        super.onResume()
    }

    fun showAppSelector() {
        appsListFragment.show(childFragmentManager, AppsListFragment::class.java.name)
    }

    private fun updateUnsupportedApps() {
        policyPollingScope.launch {
            val isAllowed = requireContext().getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                .getBoolean(getString(R.string.key_session_exclude_restricted), true)

            restrictedApps = if(!isAllowed) {
                arrayOf()
            } else {
                DumpManager.get(requireContext()).dumpCaptureAllowlistLog()?.let {
                    sessionRecordingPolicyManager.update(it)
                }

                sessionRecordingPolicyManager
                    .getRestrictedUids()
                    .map { requireContext().getAppNameFromUidSafe(it) }
                    .toTypedArray()
            }

            this@BlocklistFragment.binding.notice.isVisible = restrictedApps.isNotEmpty()
            this@BlocklistFragment.binding.noticeLabel.text =
                requireContext().resources.getQuantityString(
                    R.plurals.unsupported_apps,
                    restrictedApps.size,
                    restrictedApps.size
                )
        }
    }

    companion object {
        fun newInstance(): BlocklistFragment {
            return BlocklistFragment()
        }
    }
}