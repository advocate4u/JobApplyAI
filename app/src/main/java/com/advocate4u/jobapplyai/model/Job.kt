package com.advocate4u.jobapplyai.model

data class Job(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val experience: String,
    val salary: String,
    val description: String,
    val url: String,
    val matchScore: Int = 0
)
