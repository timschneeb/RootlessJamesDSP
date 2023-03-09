package me.timschneeberger.rootlessjamesdsp.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.adapter.AutoEqResultAdapter
import me.timschneeberger.rootlessjamesdsp.api.AutoEqClient
import me.timschneeberger.rootlessjamesdsp.contract.AutoEqSelectorContract
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityAeqSelectorBinding
import me.timschneeberger.rootlessjamesdsp.model.api.AeqSearchResult
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getSerializableAs
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.hideKeyboardFrom

class AeqSelectorActivity : BaseActivity() {

    private lateinit var binding: ActivityAeqSelectorBinding
    private lateinit var autoEqClient: AutoEqClient

    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAeqSelectorBinding.inflate(layoutInflater)

        autoEqClient = AutoEqClient(this)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.searchView.isSubmitButtonEnabled = true
        binding.searchView.isIconified = false

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if(query != null) {
                    if(query.isBlank())
                        return true

                    hideKeyboard()

                    if(!isLoading) {
                        triggerQuery(query)
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        // Hide close button of search view (isIconifiedDefault is not sufficient)
        val closeButton = binding.searchView.findViewById<View>(androidx.appcompat.R.id.search_close_btn)
        closeButton.scaleX = 0f
        closeButton.scaleY = 0f
        closeButton.setOnClickListener {}

        // Setup recycler view
        val llm = LinearLayoutManager(this)
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.profileList.layoutManager = llm
        binding.profileList.addItemDecoration(
            DividerItemDecoration(this, llm.orientation)
        )

        val initialResults = savedInstanceState
            ?.getSerializableAs<Array<AeqSearchResult>>(STATE_RESULTS)
            ?: arrayOf()

        val isLoadingOld = savedInstanceState?.getBoolean(STATE_IS_LOADING, false) ?: false
        val isPartialOld = savedInstanceState?.getBoolean(STATE_IS_PARTIAL, false) ?: false
        binding.partialResultsCard.isVisible = isPartialOld
        binding.partialResultsCard.bodyText = getString(R.string.autoeq_partial_results_warning, initialResults.size)

        if(initialResults.isNotEmpty() || isLoadingOld) {
            hideKeyboard()
        }

        val adapter = AutoEqResultAdapter(initialResults)
        adapter.onClickListener = ret@ {
            if(it.id == null)
                return@ret

            isLoading = true
            updateViewStates()

            autoEqClient.getProfile(
                it.id!!,
                onResponse = { profile, _  ->
                    val response = Intent()
                    response.putExtra(AutoEqSelectorContract.EXTRA_RESULT, profile)
                    setResult(RESULT_OK, response)
                    finish()
                },
                onFailure = (::handleFailure)
            )
        }

        binding.profileList.adapter = adapter

        if(isLoadingOld) {
            triggerQuery(binding.searchView.query.toString())
        }

        updateViewStates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val adapter = binding.profileList.adapter as AutoEqResultAdapter
        super.onSaveInstanceState(outState.apply
        {
            putSerializable(STATE_RESULTS, adapter.results)
            putBoolean(STATE_IS_LOADING, isLoading)
            putBoolean(STATE_IS_PARTIAL, binding.partialResultsCard.isVisible)
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun triggerQuery(query: String) {
        isLoading = true
        updateViewStates()

        autoEqClient.queryProfiles(
            query,
            onResponse = { results, isPartial  ->
                binding.partialResultsCard.bodyText = getString(R.string.autoeq_partial_results_warning, results.size)
                binding.partialResultsCard.isVisible = isPartial

                val adapter = binding.profileList.adapter as AutoEqResultAdapter
                adapter.results = results
                adapter.notifyDataSetChanged()

                // Replace hint text in empty view
                binding.emptyViewText.text = getString(R.string.autoeq_no_results)

                isLoading = false
                updateViewStates()
            },
            onFailure = (::handleFailure)
        )
    }

    private fun handleFailure(error: String) {
        Snackbar
            .make(binding.root, error, Snackbar.LENGTH_SHORT)
            .setTextMaxLines(5)
            .show()
        isLoading = false
        updateViewStates()
    }

    private fun updateViewStates() {
        val isEmpty = (binding.profileList.adapter?.itemCount ?: 0) < 1
        binding.profileListContainer.isVisible = !isEmpty && !isLoading
        binding.emptyView.isVisible = isEmpty && !isLoading
        binding.progress.isVisible = isLoading
    }

    private fun hideKeyboard() {
        this.hideKeyboardFrom(binding.searchView)
        binding.searchView.clearFocus()
    }

    companion object {
        private const val STATE_RESULTS = "results"
        private const val STATE_IS_LOADING = "isLoading"
        private const val STATE_IS_PARTIAL = "isPartial"
    }
}