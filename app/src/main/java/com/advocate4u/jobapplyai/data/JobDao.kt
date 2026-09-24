package com.advocate4u.jobapplyai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<JobEntity>)

    @Query("SELECT * FROM jobs ORDER BY discoveredAt DESC")
    suspend fun getAll(): List<JobEntity>

    @Query("UPDATE jobs SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("SELECT * FROM jobs WHERE status = :status ORDER BY discoveredAt DESC")
    suspend fun getByStatus(status: String): List<JobEntity>
}
