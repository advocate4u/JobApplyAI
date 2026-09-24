package com.advocate4u.jobapplyai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val company: String,
    val location: String,
    val experience: String,
    val salary: String,
    val description: String,
    val url: String,
    val discoveredAt: Long
)
