package com.example.digitalpass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ReportAdapter(private val reportList: ArrayList<HashMap<String, String>>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reportImageView: ImageView = itemView.findViewById(R.id.reportImageView)
        val reportedByTextView: TextView = itemView.findViewById(R.id.reportedByTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        var department=itemView.findViewById<TextView>(R.id.department)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val currentReport = reportList[position]

        holder.reportedByTextView.text = "Reported By: ${currentReport["reportedBy"]}"
        holder.dateTextView.text = currentReport["date"]
        holder.timeTextView.text = currentReport["time"]
        holder.descriptionTextView.text = currentReport["description"]
        if(currentReport["department"]!="ALL DEPARTMENT")holder.department.text="Department: ${currentReport["department"]}"

        val imageUrl = currentReport["img"]
        if (!imageUrl.isNullOrEmpty()) {
            holder.reportImageView.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(LoginUserDataHolder.getURL(imageUrl))
                .into(holder.reportImageView)
        } else {
            holder.reportImageView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return reportList.size
    }
}
