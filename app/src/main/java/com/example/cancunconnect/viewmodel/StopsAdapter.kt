package com.example.cancunconnect.viewmodel

import com.example.cancunconnect.model.Stop
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cancunconnect.R

class StopsAdapter(private val stopsList: List<Stop>) :
    RecyclerView.Adapter<StopsAdapter.StopViewHolder>() {

    class StopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stopName: TextView = view.findViewById(R.id.stop_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stop, parent, false)
        return StopViewHolder(view)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.stopName.text = stopsList[position].name
    }

    override fun getItemCount(): Int {
        return stopsList.size
    }
}
