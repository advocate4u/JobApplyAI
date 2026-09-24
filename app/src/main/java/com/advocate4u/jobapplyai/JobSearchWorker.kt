package com.advocate4u.jobapplyai
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.advocate4u.jobapplyai.data.Preferences
class JobSearchWorker(appContext:Context,params:WorkerParameters):CoroutineWorker(appContext,params){
 override suspend fun doWork():Result{
  val prefs=Preferences(applicationContext)
  if(!prefs.dailySearchEnabled)return Result.success()
  applicationContext.getSystemService(android.app.NotificationManager::class.java)?.notify(1001,android.app.Notification.Builder(applicationContext,"jobs").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("JobApply AI").setContentText("Your scheduled job search is ready. Open the app to review Naukri results.").setAutoCancel(true).build())
  return Result.success()
 }
}