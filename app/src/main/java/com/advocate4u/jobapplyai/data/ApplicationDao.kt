package com.advocate4u.jobapplyai.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
@Dao interface ApplicationDao {
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(item:ApplicationEntity)
 @Query("SELECT * FROM applications ORDER BY updatedAt DESC") suspend fun getAll():List<ApplicationEntity>
 @Query("SELECT * FROM applications WHERE jobId=:jobId LIMIT 1") suspend fun findByJobId(jobId:String):ApplicationEntity?
 @Query("UPDATE applications SET status=:status,updatedAt=:updatedAt,appliedAt=:appliedAt WHERE id=:id") suspend fun setStatus(id:String,status:String,updatedAt:Long,appliedAt:Long?)
}
