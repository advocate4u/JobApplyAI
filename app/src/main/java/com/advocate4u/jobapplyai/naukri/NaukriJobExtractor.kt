package com.advocate4u.jobapplyai.naukri

import android.webkit.WebView
import org.json.JSONArray

class NaukriJobExtractor {
    fun extract(webView: WebView, onResult: (List<Map<String, String>>) -> Unit) {
        val script = """
            (() => {
              const seen = new Set();
              const out = [];
              const anchors = Array.from(document.querySelectorAll('a[href]'));

              for (const a of anchors) {
                const href = a.href || '';
                const title = (a.innerText || a.textContent || '').trim().replace(/\s+/g, ' ');
                if (!title || title.length < 5) continue;
                if (!/naukri\.com/i.test(href)) continue;
                if (!/(job|jobs|job-listings)/i.test(href)) continue;

                let root = a.closest('article, li, div[class*="jobTuple"], div[class*="cust-job"], div[class*="tuple"]');
                if (!root) root = a.parentElement;
                if (!root) continue;

                const raw = (root.innerText || '').trim().replace(/\n{2,}/g, '\n');
                if (raw.length < 30) continue;

                const key = href.split('?')[0];
                if (seen.has(key)) continue;
                seen.add(key);

                out.push({ title: title, url: href, text: raw.slice(0, 3000) });
                if (out.length >= 40) break;
              }
              return JSON.stringify(out);
            })()
        """.trimIndent()

        webView.evaluateJavascript(script) { raw ->
            val jsonText = raw
                .removePrefix(""")
                .removeSuffix(""")
                .replace("\"", """)
                .replace("\\", "\")
            val array = try { JSONArray(jsonText) } catch (_: Exception) { JSONArray() }

            val result = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim()
                    val url = item.optString("url").trim()
                    val text = item.optString("text").trim()
                    if (title.isNotBlank() && url.isNotBlank()) {
                        add(mapOf("title" to title, "url" to url, "text" to text))
                    }
                }
            }
            onResult(result)
        }
    }

    fun mapToJob(item: Map<String, String>): Map<String, String> {
        val lines = item["text"].orEmpty().lines().map { it.trim() }.filter { it.isNotBlank() }
        val title = item["title"].orEmpty()
        val company = lines.firstOrNull { it != title && it.length in 2..100 } ?: "Unknown company"
        val location = lines.firstOrNull {
            it.contains("Delhi", true) || it.contains("Gurgaon", true) ||
            it.contains("Gurugram", true) || it.contains("Noida", true) ||
            it.contains("Chandigarh", true) || it.contains("Bengaluru", true) ||
            it.contains("Hyderabad", true) || it.contains("Pune", true) ||
            it.contains("Mumbai", true)
        } ?: "Location not detected"
        val experience = lines.firstOrNull {
            it.contains("year", true) && (it.contains("experience", true) || it.matches(Regex(".*\\d+.*")))
        } ?: "Experience not detected"
        val salary = lines.firstOrNull {
            it.contains("lakh", true) || it.contains("LPA", true) ||
            it.contains("salary", true) || it.contains("₹")
        } ?: "Salary not disclosed"

        return mapOf(
            "title" to title,
            "company" to company,
            "location" to location,
            "experience" to experience,
            "salary" to salary,
            "description" to item["text"].orEmpty(),
            "url" to item["url"].orEmpty()
        )
    }
}
