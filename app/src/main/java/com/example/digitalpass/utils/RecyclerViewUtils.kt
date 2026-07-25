package com.example.digitalpass.utils

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.digitalpass.R
import java.util.WeakHashMap

private val emptyStateObservers = WeakHashMap<RecyclerView, RecyclerView.AdapterDataObserver>()

fun RecyclerView.setupEmptyState(emptyView: View, emptyText: String = "No Data Found", logoResId: Int = R.drawable.mainapplogo) {
    val emptyStateTextView = emptyView.findViewById<TextView>(R.id.emptyStateText)
    emptyStateTextView?.text = emptyText

    val emptyStateLogoView = emptyView.findViewById<ImageView>(R.id.emptyStateLogo)
    emptyStateLogoView?.setImageResource(logoResId)

    // Unregister previous observer if one was set for this RecyclerView
    val oldObserver = emptyStateObservers[this]
    if (oldObserver != null) {
        try {
            this.adapter?.unregisterAdapterDataObserver(oldObserver)
        } catch (e: Exception) {
            // Ignore if it was not registered to the current adapter
        }
    }

    val adapter = this.adapter
    if (adapter == null) {
        return
    }

    val observer = object : RecyclerView.AdapterDataObserver() {
        private fun checkIfEmpty() {
            val currentAdapter = this@setupEmptyState.adapter
            if (currentAdapter != adapter) return
            
            if (currentAdapter.itemCount == 0) {
                emptyView.visibility = View.VISIBLE
                this@setupEmptyState.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                this@setupEmptyState.visibility = View.VISIBLE
            }
        }

        override fun onChanged() = checkIfEmpty()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkIfEmpty()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkIfEmpty()
    }
    
    // Register the observer
    adapter.registerAdapterDataObserver(observer)
    emptyStateObservers[this] = observer
    
    // We intentionally DO NOT check initially.
    // This allows loading spinners to show without the empty state flashing.
    // When the data loads (or fails to load), notifyDataSetChanged() will trigger the observer.
}

fun RecyclerView.evaluateEmptyState(emptyView: View) {
    val currentAdapter = this.adapter
    if (currentAdapter != null && currentAdapter.itemCount == 0) {
        emptyView.visibility = View.VISIBLE
        this.visibility = View.GONE
    } else {
        emptyView.visibility = View.GONE
        this.visibility = View.VISIBLE
    }
}
