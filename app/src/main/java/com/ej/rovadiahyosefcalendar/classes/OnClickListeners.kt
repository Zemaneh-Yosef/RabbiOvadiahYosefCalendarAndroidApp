package com.ej.rovadiahyosefcalendar.classes

class OnClickListeners {
    interface OnItemClickListener {
        fun onItemClick(category: HighlightString?)
    }
    interface OnZmanClickListener {
        fun onItemClick()
    }
    interface ScraperCallback {
        fun onScraperFinished()
    }
}
