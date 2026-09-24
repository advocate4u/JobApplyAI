package com.advocate4u.jobapplyai.matching
import com.advocate4u.jobapplyai.model.CandidateProfile
import com.advocate4u.jobapplyai.model.Job
import java.util.Locale
data class MatchResult(val score:Int,val matched:List<String>,val missing:List<String>)
class JobMatcher{
 private val aliases=mapOf(".net" to setOf(".net","dotnet","asp.net","asp.net core"),"c#" to setOf("c#","c sharp"),"web api" to setOf("web api","webapi"),"sql" to setOf("sql","sql server","mssql"),"microservices" to setOf("microservices","microservices architecture"),"javascript" to setOf("javascript","js"),"typescript" to setOf("typescript","ts"),"kafka" to setOf("kafka"))
 fun match(profile:CandidateProfile,job:Job):MatchResult{
  val text="${job.title} ${job.description}".lowercase(Locale.US)
  val skills=profile.skills.filter{it.isNotBlank()}
  val matched=skills.filter { skill ->\n    val key=skill.lowercase(Locale.US)\n    aliases[key]?.any { alias -> text.contains(alias) } ?: text.contains(key)\n  }
  val missing=skills.filterNot{it in matched}
  val skillScore=if(skills.isEmpty())100 else (matched.size*100/skills.size)
  val locationScore=if(profile.locations.isEmpty()||profile.locations.any{textContains(job.location,it}))100 else 0
  val score=((skillScore*80)+(locationScore*20))/100
  return MatchResult(score.coerceIn(0,100),matched,missing)
 }
 private fun textContains(value:String,needle:String)=value.contains(needle,true)
}