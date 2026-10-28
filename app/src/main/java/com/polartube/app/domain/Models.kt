package com.polartube.app.domain

enum class ResultType { VIDEO, CHANNEL, PLAYLIST }

data class SearchItem(
    val url: String, val title: String, val channel: String,
    val durationSec: Long, val thumbnail: String?, val type: ResultType
)

/** videoUrl+audioUrl = DASH-like (se mezclan); solo videoUrl = muxed; solo audioUrl = solo audio */
data class StreamOption(val label: String, val videoUrl: String?, val audioUrl: String?) {
    companion object { const val AUDIO_ONLY = "Solo audio" }
}

data class StreamDetails(
    val title: String, val uploader: String, val thumbnail: String?,
    val options: List<StreamOption>
)
