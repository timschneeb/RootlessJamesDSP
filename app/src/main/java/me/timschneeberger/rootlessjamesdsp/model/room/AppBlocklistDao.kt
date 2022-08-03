package me.timschneeberger.rootlessjamesdsp.model.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Data access object
@Dao
interface AppBlocklistDao {
    @Query("SELECT * FROM blockedapp")
    fun getAll(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blockedapp WHERE uid IN (:uids)")
    fun findAllByUid(uids: IntArray): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blockedapp WHERE package_name LIKE :packageName LIMIT 1")
    fun findByPackage(packageName: String): Flow<BlockedApp>

    @Query("SELECT * FROM blockedapp WHERE uid IS :uid LIMIT 1")
    fun findByUid(uid: Int): Flow<BlockedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg blockedApps: BlockedApp)

    @Delete
    suspend fun delete(user: BlockedApp)

    @Query("DELETE FROM blockedapp")
    suspend fun deleteAll()
}