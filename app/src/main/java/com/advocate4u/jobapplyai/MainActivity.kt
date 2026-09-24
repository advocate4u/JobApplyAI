package com.advocate4u.jobapplyai

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.advocate4u.jobapplyai.data.AppDatabase
import com.advocate4u.jobapplyai.data.JobEntity
import com.advocate4u.jobapplyai.model.Job
import com.advocate4u.jobapplyai.model.CandidateProfile
import com.advocate4u.jobapplyai.matching.JobMatcher
import com.advocate4u.jobapplyai.naukri.NaukriJobExtractor
import com.advocate4u.jobapplyai.ui.JobAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var jobsHeader: TextView
    private lateinit var keyword: EditText
    private lateinit var adapter: JobAdapter
    private val extractor = NaukriJobExtractor()
    private val matcher = JobMatcher()
    private val profile = CandidateProfile()
    private val db by lazy { AppDatabase.get(this) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.naukriWebView)
        status = findViewById(R.id.status)
        jobsHeader = findViewById(R.id.jobsHeader)
        keyword = findViewById(R.id.keyword)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val jobsList = findViewById<RecyclerView>(R.id.jobsList)

        adapter = JobAdapter { job ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(job.url)))
        }
        jobsList.layoutManager = LinearLayoutManager(this)
        jobsList.adapter = adapter

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                status.text = "Loaded Naukri page"
                if (url.contains("naukri.com", true)) extractVisibleJobs()
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return if (url.contains("naukri.com", true)) false
                else {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                }
            }
        }

        searchButton.setOnClickListener { searchNaukri(keyword.text.toString()) }
        keyword.setOnEditorActionListener { _, _, _ ->
            searchNaukri(keyword.text.toString())
            true
        }

        webView.loadUrl("https://www.naukri.com/")
        status.text = "Naukri opened. Sign in normally, then search."
    }

    private fun searchNaukri(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            status.text = "Enter a job title."
            return
        }
        val slug = q.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = "https://www.naukri.com/${slug}-jobs?k=$encoded"
        status.text = "Opening Naukri search for "$q"..."
        webView.loadUrl(url)
    }

    private fun extractVisibleJobs() {
        status.text = "Reading visible Naukri job cards..."
        extractor.extract(webView) { rawItems ->
            val jobs = rawItems.map { extractor.mapToJob(it) }
                .mapIndexed { index, item ->
                    Job(
                        id = stableId(item["url"].orEmpty(), index),
                        title = item["title"].orEmpty(),
                        company = item["company"].orEmpty(),
                        location = item["location"].orEmpty(),
                        experience = item["experience"].orEmpty(),
                        salary = item["salary"].orEmpty(),
                        description = item["description"].orEmpty(),
                        url = item["url"].orEmpty()
                    )
                }
                .distinctBy { it.url }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    db.jobDao().upsertAll(jobs.map {
                        JobEntity(
                            id = it.id,
                            title = it.title,
                            company = it.company,
                            location = it.location,
                            experience = it.experience,
                            salary = it.salary,
                            description = it.description,
                            url = it.url,
                            discoveredAt = System.currentTimeMillis()
                        )
                    })
                }
                adapter.submitList(jobs)
                jobsHeader.text = "Jobs found: ${jobs.size}"
                status.text = if (jobs.isEmpty()) {
                    "No visible job cards detected. The Naukri page structure may have changed."
                } else {
                    "Found ${jobs.size} visible jobs."
                }
            }
        }
    }

    private fun stableId(url: String, fallback: Int): String =
        if (url.isNotBlank()) url.hashCode().toString() else "job-$fallback"

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
