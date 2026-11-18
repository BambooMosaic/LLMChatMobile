package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiConfigDao {

    @Query("SELECT * FROM api_configs ORDER BY id ASC")
    fun observeAll(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE isDefault = 1 ORDER BY id ASC LIMIT 1")
    suspend fun getDefault(): ApiConfigEntity?

    @Query("SELECT * FROM api_configs ORDER BY id ASC LIMIT 1")
    suspend fun getFirst(): ApiConfigEntity?

    @Query("SELECT COUNT(*) FROM api_configs")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: ApiConfigEntity): Long

    @Update
    suspend fun update(entity: ApiConfigEntity)

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun delete(id: Long)

    /** 单条 SQL 保证全局只有一条默认配置。 */
    @Query("UPDATE api_configs SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefault(id: Long)
}
