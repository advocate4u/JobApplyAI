package com.advocate4u.jobapplyai.model

data class CandidateProfile(
    val name: String = "",
    val yearsExperience: Int = 10,
    val skills: Set<String> = setOf("C#", ".NET", "ASP.NET Core", "Web API", "SQL", "Microservices", "Kafka", "JavaScript", "TypeScript"),
    val locations: Set<String> = emptySet(),
    val minimumSalaryLpa: Double = 0.0,
    val resumeText: String = ""
)
