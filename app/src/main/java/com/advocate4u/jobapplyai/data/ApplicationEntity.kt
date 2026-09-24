package com.advocate4u.jobapplyai.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "applications")
data class ApplicationEntity(@PrimaryKey val id:String,val jobId:String,val status:String,val updatedAt:Long,val note:String="",val appliedAt:Long?=null)
