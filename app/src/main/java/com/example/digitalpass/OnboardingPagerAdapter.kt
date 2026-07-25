package com.example.digitalpass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.HashMap

class OnboardingPagerAdapter(private val onboardingItems: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val layoutContainer = view.findViewById<FrameLayout>(R.id.onboardingLayoutContainer)
        private val textTitle = view.findViewById<TextView>(R.id.onboardingTitle)
        private val textDescription = view.findViewById<TextView>(R.id.onboardingDescription)

        fun bind(onboardingItem: OnboardingItem) {
            textTitle.text = onboardingItem.title
            textDescription.text = onboardingItem.description
            
            layoutContainer.removeAllViews()
            try {
                // Inflate the actual app layout to serve as a visual preview
                val previewView = LayoutInflater.from(itemView.context)
                    .inflate(onboardingItem.layoutResId, layoutContainer, false)
                
                // Populate any RecyclerViews so they don't look empty in the preview
                populateDummyDataInPreview(previewView)
                
                // The parent CardView contains a transparent touch interceptor overlay
                // to prevent the user from actually interacting with this preview layout.
                layoutContainer.addView(previewView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        private fun populateDummyDataInPreview(previewView: View) {
            val context = previewView.context
            val packageName = context.packageName
            
            // Dynamically find RecyclerViews to avoid compilation errors
            val rvId1 = previewView.resources.getIdentifier("recyclerView", "id", packageName)
            val rvId2 = previewView.resources.getIdentifier("recyclerViewUserManagement", "id", packageName)
            
            val recyclerViews = mutableListOf<RecyclerView>()
            if (rvId1 != 0) previewView.findViewById<RecyclerView>(rvId1)?.let { recyclerViews.add(it) }
            if (rvId2 != 0) previewView.findViewById<RecyclerView>(rvId2)?.let { recyclerViews.add(it) }
            
            if (recyclerViews.isNotEmpty()) {
                val dummyList = ArrayList<HashMap<String, String>>()
                
                val item1 = HashMap<String, String>().apply {
                    put("name", "John Doe")
                    put("status", "approved")
                    put("applyDate", "Today, 10:00 AM")
                    put("entryDate", "Today, 10:00 AM")
                    put("img", "")
                }
                val item2 = HashMap<String, String>().apply {
                    put("name", "Jane Smith")
                    put("status", "pending")
                    put("applyDate", "Today, 11:30 AM")
                    put("entryDate", "Today, 11:30 AM")
                    put("img", "")
                }
                dummyList.add(item1)
                dummyList.add(item2)
                
                // Use RecentPassAdapter as a generic preview adapter
                val dummyAdapter = RecentPassAdapter("gatePass", dummyList)
                
                for (rv in recyclerViews) {
                    rv.layoutManager = LinearLayoutManager(context)
                    rv.adapter = dummyAdapter
                }
                
                // Dynamically hide empty state layouts
                val emptyId1 = previewView.resources.getIdentifier("emptyStateLayout", "id", packageName)
                val emptyId2 = previewView.resources.getIdentifier("emptyStateSelected", "id", packageName)
                val emptyId3 = previewView.resources.getIdentifier("emptyStateUnselected", "id", packageName)
                
                if (emptyId1 != 0) previewView.findViewById<View>(emptyId1)?.visibility = View.GONE
                if (emptyId2 != 0) previewView.findViewById<View>(emptyId2)?.visibility = View.GONE
                if (emptyId3 != 0) previewView.findViewById<View>(emptyId3)?.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        return OnboardingViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_onboarding,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int {
        return onboardingItems.size
    }
}
