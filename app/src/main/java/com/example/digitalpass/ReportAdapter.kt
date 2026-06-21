package com.example.digitalpass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ReportAdapter(
    private val reportList: ArrayList<HashMap<String, String>>,
    private val onSelectionChange: (Int) -> Unit
) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    val selectedItems = mutableSetOf<Int>()
    var isSelectionMode = false

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootCardView: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.rootCardView)
        val reportImageView: ImageView = itemView.findViewById(R.id.reportImageView)
        val reportedByTextView: TextView = itemView.findViewById(R.id.reportedByTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        var department = itemView.findViewById<TextView>(R.id.textView4)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val currentReport = reportList[position]

        var dateTimeSplittedArray=currentReport["dateTime"]?.split(" ")

        holder.reportedByTextView.text = "Reported By: ${currentReport["reportedBy"]}"
        holder.dateTextView.text = dateTimeSplittedArray?.get(0)
        holder.timeTextView.text = dateTimeSplittedArray?.get(1)
        holder.descriptionTextView.text = "Report Description: ${currentReport["description"]}"
        if(currentReport["department"]!="ALL DEPARTMENT") holder.department.text="Department: ${currentReport["department"]}"

        val imageUrl = currentReport["img"]
        if (!imageUrl.isNullOrEmpty()) {
            holder.reportImageView.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(LoginUserDataHolder.getURL(imageUrl))
                .into(holder.reportImageView)
        } else {
            holder.reportImageView.visibility = View.GONE
        }
        
        if (selectedItems.contains(position)) {
            holder.rootCardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F2FE"))
            holder.rootCardView.strokeColor = android.graphics.Color.parseColor("#3B82F6")
            holder.rootCardView.strokeWidth = 4
        } else {
            holder.rootCardView.setCardBackgroundColor(android.graphics.Color.WHITE)
            holder.rootCardView.strokeWidth = 1
            holder.rootCardView.strokeColor = android.graphics.Color.parseColor("#E2E8F0")
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(position)
            }
            true
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        
        if (selectedItems.isEmpty()) {
            isSelectionMode = false
        }
        
        notifyItemChanged(position)
        onSelectionChange(selectedItems.size)
    }

    fun getSelectedReports(): List<HashMap<String, String>> {
        return selectedItems.map { reportList[it] }
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChange(0)
    }

    override fun getItemCount(): Int {
        return reportList.size
    }
}
