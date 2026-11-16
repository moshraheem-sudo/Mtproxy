package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.ProxyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyDao {

    @Query(
        """
        SELECT * FROM proxies 
        ORDER BY isReachable DESC, 
                 CASE WHEN latencyMs > 0 THEN latencyMs ELSE 999999 END ASC, 
                 isFavorite DESC
        """
    )
    fun observeAllProxies(): Flow<List<ProxyEntity>>

    @Query(
        """
        SELECT * FROM proxies 
        ORDER BY isReachable DESC, 
                 CASE WHEN latencyMs > 0 THEN latencyMs ELSE 999999 END ASC
        """
    )
    suspend fun getAllProxiesOnce(): List<ProxyEntity>

    @Query("SELECT * FROM proxies WHERE id = :id LIMIT 1")
    suspend fun getProxyById(id: String): ProxyEntity?

    @Query(
        """
        SELECT * FROM proxies 
        WHERE isReachable = 1 AND latencyMs > 0 
        ORDER BY latencyMs ASC 
        LIMIT 1
        """
    )
    suspend fun getFastestReachableProxy(): ProxyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(proxies: List<ProxyEntity>)

    @Query(
        """
        UPDATE proxies 
        SET latencyMs = :latencyMs, isReachable = :isReachable, lastCheckedAt = :checkedAt 
        WHERE id = :id
        """
    )
    suspend fun updateLatency(id: String, latencyMs: Int, isReachable: Boolean, checkedAt: Long)

    @Query("UPDATE proxies SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM proxies WHERE isFavorite = 0 AND id NOT IN (:keepIds)")
    suspend fun deleteStaleNonFavorites(keepIds: List<String>)

    @Query("DELETE FROM proxies WHERE isFavorite = 0")
    suspend fun deleteAllNonFavorites()

    @Query("DELETE FROM proxies WHERE isReachable = 0 AND lastCheckedAt > 0")
    suspend fun deleteUnreachableProxies(): Int

    @Transaction
    suspend fun mergeSyncedProxies(freshProxies: List<ProxyEntity>) {
        if (freshProxies.isEmpty()) return
        val existingMap = getAllProxiesOnce().associateBy { it.id }
        val merged = freshProxies.map { fresh ->
            val existing = existingMap[fresh.id]
            if (existing != null) {
                fresh.copy(
                    latencyMs = existing.latencyMs,
                    isReachable = existing.isReachable,
                    isFavorite = existing.isFavorite,
                    lastCheckedAt = existing.lastCheckedAt,
                    addedAt = existing.addedAt
                )
            } else {
                fresh
            }
        }
        val keepIds = merged.map { it.id }
        if (keepIds.size <= 900) {
            deleteStaleNonFavorites(keepIds)
        } else {
            deleteAllNonFavorites()
        }
        insertAll(merged)
    }
}
