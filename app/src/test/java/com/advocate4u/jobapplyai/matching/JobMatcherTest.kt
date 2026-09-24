package com.advocate4u.jobapplyai.matching
import com.advocate4u.jobapplyai.model.CandidateProfile
import com.advocate4u.jobapplyai.model.Job
import org.junit.Assert.assertEquals
import org.junit.Test
class JobMatcherTest{
 @Test fun matchingSkillsProduceScore(){
  val p=CandidateProfile(skills=setOf("C#",".NET","Kafka"))
  val j=Job("1","Senior .NET Developer","","","", "", "C# .NET Kafka Web API","https://example.com")
  assertEquals(100,JobMatcher().match(p,j).score)
 }
 @Test fun missingSkillIsReported(){
  val p=CandidateProfile(skills=setOf("C#","Kafka"))
  val j=Job("1","Developer","","","", "", "C# Web API","https://example.com")
  val r=JobMatcher().match(p,j)
  assertEquals(listOf("Kafka"),r.missing)
 }
}