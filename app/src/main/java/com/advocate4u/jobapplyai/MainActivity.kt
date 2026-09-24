package com.advocate4u.jobapplyai

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.advocate4u.jobapplyai.data.*
import com.advocate4u.jobapplyai.matching.JobMatcher
import com.advocate4u.jobapplyai.model.CandidateProfile
import com.advocate4u.jobapplyai.model.Job
import com.advocate4u.jobapplyai.naukri.NaukriJobExtractor
import com.advocate4u.jobapplyai.ui.JobAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var webView:WebView
    private lateinit var status:TextView
    private lateinit var jobsHeader:TextView
    private lateinit var keyword:EditText
    private lateinit var adapter:JobAdapter
    private val extractor=NaukriJobExtractor()
    private val db by lazy{AppDatabase.get(this)}
    private val prefs by lazy{Preferences(this)}
    private val matcher=JobMatcher()
    private var currentJobs=emptyList<Job>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView=findViewById(R.id.naukriWebView); status=findViewById(R.id.status)
        jobsHeader=findViewById(R.id.jobsHeader); keyword=findViewById(R.id.keyword)
        val search=findViewById<Button>(R.id.searchButton)
        val profile=findViewById<Button>(R.id.profileButton)
        val tracker=findViewById<Button>(R.id.trackerButton)
        val saved=findViewById<Button>(R.id.savedButton)
        val daily=findViewById<Switch>(R.id.dailySwitch)
        val minMatch=findViewById<EditText>(R.id.minMatch)
        val list=findViewById<RecyclerView>(R.id.jobsList)

        keyword.setText(prefs.keywords); minMatch.setText(prefs.minimumMatch.toString()); daily.isChecked=prefs.dailySearchEnabled
        adapter=JobAdapter{job->showJobActions(job)}
        list.layoutManager=LinearLayoutManager(this); list.adapter=adapter

        webView.settings.javaScriptEnabled=true; webView.settings.domStorageEnabled=true
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true)
        webView.webChromeClient=WebChromeClient()
        webView.webViewClient=object:WebViewClient(){
            override fun onPageFinished(view:WebView,url:String){status.text="Naukri loaded"; if(url.contains("naukri.com",true)) extractVisibleJobs()}
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
                if(request.url.toString().contains("naukri.com",true)) return false
                startActivity(Intent(Intent.ACTION_VIEW,request.url)); return true
            }
        }
        search.setOnClickListener{prefs.keywords=keyword.text.toString(); prefs.minimumMatch=minMatch.text.toString().toIntOrNull()?:50; searchNaukri(keyword.text.toString())}
        profile.setOnClickListener{showProfileDialog()}
        tracker.setOnClickListener{showTrackerDialog()}
        saved.setOnClickListener{showSavedSearches()}
        daily.setOnCheckedChangeListener{_,enabled->prefs.dailySearchEnabled=enabled; scheduleDailySearch(enabled)}
        webView.loadUrl("https://www.naukri.com/")
        createNotificationChannel()
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),44)
        scheduleDailySearch(prefs.dailySearchEnabled)
    }

    private fun searchNaukri(q0:String){
        val q=q0.trim(); if(q.isBlank()){status.text="Enter a job title";return}
        prefs.keywords=q
        val slug=q.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"),"-").trim('-')
        val url="https://www.naukri.com/${slug}-jobs?k=${URLEncoder.encode(q,"UTF-8")}"
        status.text="Opening Naukri search for $q..."
        webView.loadUrl(url)
    }

    private fun extractVisibleJobs(){
        status.text="Reading visible job cards..."
        extractor.extract(webView){raw->
            val profile=CandidateProfile(prefs.name,prefs.experience,prefs.skills.split(",").map{it.trim()}.filter{it.isNotBlank()}.toSet(),prefs.locations.split(",").map{it.trim()}.filter{it.isNotBlank()}.toSet(),prefs.minimumSalaryLpa)
            val jobs=raw.mapIndexed{index,item->
                val m=extractor.mapToJob(item)
                val id=stableId(m["url"].orEmpty(),index)
                val j=Job(id,m["title"].orEmpty(),m["company"].orEmpty(),m["location"].orEmpty(),m["experience"].orEmpty(),m["salary"].orEmpty(),m["description"].orEmpty(),m["url"].orEmpty())
                j.copy(matchScore=matcher.match(profile,j).score)
            }.distinctBy{it.url}.filter{it.matchScore>=prefs.minimumMatch}
            currentJobs=jobs
            lifecycleScope.launch{
                withContext(Dispatchers.IO){db.jobDao().upsertAll(jobs.map{val mr=matcher.match(profile,it);JobEntity(it.id,it.title,it.company,it.location,it.experience,it.salary,it.description,it.url,System.currentTimeMillis(),"NEW",it.matchScore,mr.matched.joinToString(","),mr.missing.joinToString(","))})}
                adapter.submitList(jobs); jobsHeader.text="Jobs found: ${jobs.size} (match ≥ ${prefs.minimumMatch}%)"
                status.text="Found ${jobs.size} matching jobs. Final application submission always requires your confirmation."
            }
        }
    }

    private fun showJobActions(job:Job){
        val options=arrayOf("View job","Save","Mark Applied","Skip","Generate application message")
        AlertDialog.Builder(this).setTitle(job.title).setItems(options){_,which->
            when(which){
                0->startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(job.url)))
                1->setApplication(job,"SAVED")
                2->confirmApply(job)
                3->setApplication(job,"SKIPPED")
                4->showMessage(job)
            }
        }.show()
    }

    private fun confirmApply(job:Job){
        AlertDialog.Builder(this).setTitle("Confirm application").setMessage("Open the original listing and review all fields before submitting?").setNegativeButton("Cancel",null).setPositiveButton("Open"){_,_->setApplication(job,"APPLIED");startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(job.url))) }.show()
    }

    private fun setApplication(job:Job,statusValue:String){
        lifecycleScope.launch(Dispatchers.IO){
            val existing=db.applicationDao().findByJobId(job.id)
            db.applicationDao().upsert(ApplicationEntity(existing?.id?:job.id,job.id,statusValue,System.currentTimeMillis(),existing?.note?:"",if(statusValue=="APPLIED")System.currentTimeMillis() else existing?.appliedAt))
        }
        status.text="Marked ${statusValue}: ${job.title}"
    }

    private fun showTrackerDialog(){
        lifecycleScope.launch{
            val apps=withContext(Dispatchers.IO){db.applicationDao().getAll()}
            if(apps.isEmpty()){Toast.makeText(this@MainActivity,"No applications tracked yet.",Toast.LENGTH_SHORT).show();return@launch}
            val jobs=withContext(Dispatchers.IO){db.jobDao().getAll().associateBy{it.id}}
            val lines=apps.map{a->"${a.status} • ${jobs[a.jobId]?.title?:"Unknown job"}"}
            AlertDialog.Builder(this@MainActivity).setTitle("Application tracker").setItems(lines.toTypedArray()){_,i->showStatusPicker(apps[i])}.setPositiveButton("Close",null).show()
        }
    }

    private fun showStatusPicker(app:ApplicationEntity){
        val states=arrayOf("SAVED","APPLIED","INTERVIEW","REJECTED","SKIPPED","FAILED","EXTERNAL")
        AlertDialog.Builder(this).setTitle("Update status").setItems(states){_,i->
            lifecycleScope.launch(Dispatchers.IO){db.applicationDao().setStatus(app.id,states[i],System.currentTimeMillis(),if(states[i]=="APPLIED")System.currentTimeMillis() else app.appliedAt)}
            Toast.makeText(this,"Status updated",Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun showProfileDialog(){
        val box=LinearLayout(this); box.orientation=LinearLayout.VERTICAL; box.setPadding(32,16,32,8)
        fun field(hint:String,value:String):EditText=EditText(this).apply{this.hint=hint;setText(value)}
        val name=field("Name",prefs.name); val exp=field("Years experience",prefs.experience.toString()); val skills=field("Skills, comma separated",prefs.skills)
        val loc=field("Preferred locations, comma separated",prefs.locations); val sal=field("Minimum salary LPA",prefs.minimumSalaryLpa.toString()); val notice=field("Notice period",prefs.noticePeriod); val resume=field("Resume text (paste or import .txt)",prefs.resumeText)
        resume.minLines=4
        listOf(name,exp,skills,loc,sal,notice,resume).forEach{box.addView(it)}
        AlertDialog.Builder(this).setTitle("Candidate profile & preferences").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
            prefs.name=name.text.toString();prefs.experience=exp.text.toString().toIntOrNull()?:10;prefs.skills=skills.text.toString();prefs.locations=loc.text.toString();prefs.minimumSalaryLpa=sal.text.toString().toDoubleOrNull()?:0.0;prefs.noticePeriod=notice.text.toString();prefs.resumeText=resume.text.toString()
        }.show()
    }

    private fun showMessage(job:Job){
        val name=if(prefs.name.isBlank())"Hiring Team" else prefs.name
        val text="Hello, I am \$name. I am interested in ${job.title} at ${job.company}. My experience and skills align with the role. I would be happy to discuss my background and relevant experience."
        EditText(this).apply{setText(text);setSelectAllOnFocus(false);AlertDialog.Builder(this@MainActivity).setTitle("Application message").setView(this).setPositiveButton("Copy"){_,_->(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Application message",text))}.setNegativeButton("Close",null).show()}
    }


    private fun showSavedSearches(){
        val saved=prefs.savedSearches.split("\\n").map{it.trim()}.filter{it.isNotBlank()}.toMutableList()
        val items=(listOf("Save current: ${prefs.keywords}")+saved).toTypedArray()
        AlertDialog.Builder(this).setTitle("Saved searches").setItems(items){_,i->
            if(i==0){if(prefs.keywords.isNotBlank()&&!saved.contains(prefs.keywords)){saved.add(prefs.keywords);prefs.savedSearches=saved.joinToString("\\n");Toast.makeText(this,"Search saved",Toast.LENGTH_SHORT).show()}}
            else {keyword.setText(saved[i-1]);searchNaukri(saved[i-1])}
        }.setNegativeButton("Close",null).show()
    }
    private fun scheduleDailySearch(enabled:Boolean){
        val wm=WorkManager.getInstance(this)
        if(!enabled){wm.cancelUniqueWork("daily-job-search");return}
        val request=PeriodicWorkRequestBuilder<JobSearchWorker>(24,TimeUnit.HOURS).setInitialDelay(24,TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork("daily-job-search",ExistingPeriodicWorkPolicy.UPDATE,request)
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("jobs","JobApply AI",NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun stableId(url:String,fallback:Int)=if(url.isNotBlank())url.hashCode().toString() else "job-${fallback}"
    override fun onBackPressed(){if(webView.canGoBack())webView.goBack() else super.onBackPressed()}
}
