package com.ej.rovadiahyosefcalendar.classes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ej.rovadiahyosefcalendar.R


class HorizontalAdapter(private val items: List<HighlightString>, private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<HorizontalAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textViewHorizontal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.horizontal_textview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position].toString()
        holder.textView.setOnClickListener {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(items[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface OnItemClickListener {
        fun onItemClick(category: HighlightString?)
    }
}
