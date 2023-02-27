package me.timschneeberger.rootlessjamesdsp.model.room

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class AppBlocklistRepository(private val appBlocklistDao: AppBlocklistDao) {
    val blocklist: Flow<List<BlockedApp>> = appBlocklistDao.getAll()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @WorkerThread
    suspend fun insert(app: BlockedApp) {
        appBlocklistDao.insertAll(app)
    }

    @WorkerThread
    suspend fun delete(app: BlockedApp) {
        appBlocklistDao.delete(app)
    }
}