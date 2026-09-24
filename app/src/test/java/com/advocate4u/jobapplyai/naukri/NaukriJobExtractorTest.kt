package com.advocate4u.jobapplyai.naukri
import org.junit.Assert.assertEquals
import org.junit.Test
class NaukriJobExtractorTest {
 @Test fun mapsSalaryAndLocation() {
  val x=NaukriJobExtractor().mapToJob(mapOf("title" to "Senior .NET Developer","url" to "https://www.naukri.com/job-listings/test","text" to "Senior .NET Developer\nABC Tech\nGurugram\n8-12 LPA\n5-10 years experience"))
  assertEquals("Gurugram",x["location"]); assertEquals("8-12 LPA",x["salary"])
 }
}