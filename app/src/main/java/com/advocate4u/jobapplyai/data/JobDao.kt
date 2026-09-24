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
}
