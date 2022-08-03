package me.timschneeberger.rootlessjamesdsp.model.room

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class AppBlocklistViewModel(private val repository: AppBlocklistRepository) : ViewModel() {

    // Using LiveData and caching what blocklist returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val blockedApps: LiveData<List<BlockedApp>> = repository.blocklist.asLiveData()

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(app: BlockedApp) = viewModelScope.launch {
        repository.insert(app)
    }

    /**
     * Launching a new coroutine to remove data in a non-blocking way
     */
    fun delete(word: BlockedApp) = viewModelScope.launch {
        repository.delete(word)
    }
}

class AppBlocklistViewModelFactory(private val repository: AppBlocklistRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppBlocklistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppBlocklistViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}