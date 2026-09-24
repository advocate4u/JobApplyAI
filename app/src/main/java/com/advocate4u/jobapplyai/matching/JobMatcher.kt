package com.advocate4u.jobapplyai.matching

import com.advocate4u.jobapplyai.model.CandidateProfile
import com.advocate4u.jobapplyai.model.Job
import java.util.Locale

data class MatchResult(val score: Int, val matched: List<String>, val missing: List<String>)

class JobMatcher {
    private val aliases = mapOf(
        ".net" to setOf(".net", "dotnet", "asp.net", "asp.net core"),
        "c#" to setOf("c#", "c sharp"),
        "web api" to setOf("web api", "webapi"),
        "sql" to setOf("sql", "sql server", "mssql"),
        "microservices" to setOf("microservices", "microservices architecture"),
        "javascript" to setOf("javascript", "js"),
        "typescript" to setOf("typescript", "ts"),
        "kafka" to setOf("kafka")
    )
    fun match(profile: CandidateProfile, job: Job): MatchResult {
        val text = "${job.title} ${job.description}".lowercase(Locale.US)
        val matched = profile.skills.filter { skill ->
            aliases[skill.lowercase(Locale.US)]?.any { text.contains(it) } ?: text.contains(skill.lowercase(Locale.US))
        }
        val missing = profile.skills.filterNot { it in matched }
        val score = ((matched.size.toDouble() / profile.skills.size.coerceAtLeast(1)) * 100).toInt()
        return MatchResult(score.coerceIn(0,100), matched, missing)
    }
}
