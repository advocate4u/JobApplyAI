package com.advocate4u.jobapplyai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.advocate4u.jobapplyai.R
import com.advocate4u.jobapplyai.model.Job

class JobAdapter(private val onView: (Job) -> Unit) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {
    private val items = mutableListOf<Job>()
    fun submitList(newItems: List<Job>) { items.clear(); items.addAll(newItems); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        JobViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_job, parent, false))
    override fun onBindViewHolder(holder: JobViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
    inner class JobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title=view.findViewById<TextView>(R.id.jobTitle)
        private val company=view.findViewById<TextView>(R.id.company)
        private val meta=view.findViewById<TextView>(R.id.meta)
        private val description=view.findViewById<TextView>(R.id.description)
        private val button=view.findViewById<Button>(R.id.viewButton)
        fun bind(job: Job) {
            title.text = "${job.title}  •  ${job.matchScore}%"
            company.text = job.company
            meta.text = "${job.location} • ${job.experience} • ${job.salary}"
            description.text = job.description.take(220)
            button.text = "View / Apply"
            button.setOnClickListener { onView(job) }
        }
    }
}
