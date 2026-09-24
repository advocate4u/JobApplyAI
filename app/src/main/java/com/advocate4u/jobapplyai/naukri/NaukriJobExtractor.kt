package com.advocate4u.jobapplyai.naukri

import android.webkit.WebView
import org.json.JSONArray

class NaukriJobExtractor {
    fun extract(webView: WebView, onResult: (List<Map<String, String>>) -> Unit) {
        val script = """
            (() => {
              const seen = new Set(), out = [];
              const anchors = Array.from(document.querySelectorAll('a[href]'));
              for (const a of anchors) {
                const href = a.href || '';
                const title = (a.innerText || a.textContent || '').trim().replace(/\s+/g, ' ');
                if (!title || title.length < 5 || !/naukri\.com/i.test(href) || !/(job|jobs|job-listings)/i.test(href)) continue;
                let root = a.closest('article, li, div[class*="jobTuple"], div[class*="cust-job"], div[class*="tuple"], div[class*="job-tuple"]') || a.parentElement;
                if (!root) continue;
                const raw = (root.innerText || '').trim().replace(/\n{2,}/g, '\n');
                if (raw.length < 30) continue;
                const key = href.split('?')[0];
                if (seen.has(key)) continue;
                seen.add(key);
                out.push({title, url: href, text: raw.slice(0, 5000)});
                if (out.length >= 80) break;
              }
              return JSON.stringify(out);
            })()
        """.trimIndent()
        webView.evaluateJavascript(script) { raw ->
            val jsonText = raw.removePrefix(""").removeSuffix(""")
                .replace("\"", """).replace("\\", "\")
            val array = try { JSONArray(jsonText) } catch (_: Exception) { JSONArray() }
            val result = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val title=item.optString("title").trim(); val url=item.optString("url").trim()
                    if(title.isNotBlank() && url.isNotBlank()) add(mapOf("title" to title,"url" to url,"text" to item.optString("text").trim()))
                }
            }
            onResult(result)
        }
    }

    fun mapToJob(item: Map<String,String>): Map<String,String> {
        val lines=item["text"].orEmpty().lines().map{it.trim()}.filter{it.isNotBlank()}
        val title=item["title"].orEmpty()
        val company=lines.firstOrNull{it!=title && !looksLikeMeta(it)} ?: "Unknown company"
        val location=lines.firstOrNull{it.contains("remote",true)||it.contains("hybrid",true)||locationWords.any{w->it.contains(w,true)}} ?: "Location not detected"
        val experience=lines.firstOrNull{Regex("""\d+\s*(?:-|to)\s*\d+\s*(?:years?|yrs?)""",RegexOption.IGNORE_CASE).containsMatchIn(it) || it.contains("experience",true)} ?: "Experience not detected"
        val salary=lines.firstOrNull{it.contains("lpa",true)||it.contains("lakh",true)||it.contains("salary",true)||it.contains("₹",true)||Regex("""\d+(?:\.\d+)?\s*(?:-|to)\s*\d+\s*(?:lpa|lakhs?)""",RegexOption.IGNORE_CASE).containsMatchIn(it)} ?: "Salary not disclosed"
        return mapOf("title" to title,"company" to company,"location" to location,"experience" to experience,"salary" to salary,"description" to item["text"].orEmpty(),"url" to item["url"].orEmpty())
    }
    private val locationWords=listOf("Delhi","Gurgaon","Gurugram","Noida","Chandigarh","Bengaluru","Bangalore","Hyderabad","Pune","Mumbai","Chennai","Kolkata","Jaipur","Remote")
    private fun looksLikeMeta(s:String)=s.contains("year",true)||s.contains("lpa",true)||s.contains("₹")||locationWords.any{s.contains(it,true)}
}