package com.polartube.app.playback

object PlaybackCommands {
    val SET_STREAM = SessionCommand("polartube.SET_STREAM", Bundle.EMPTY)
    const val K_VIDEO = "video"; const val K_AUDIO = "audio"
    const val K_TITLE = "title"; const val K_ARTIST = "artist"
    const val K_ART = "art"; const val K_POS = "pos"
}
