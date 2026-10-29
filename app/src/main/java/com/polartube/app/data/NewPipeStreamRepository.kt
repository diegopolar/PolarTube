package com.polartube.app.data

import com.polartube.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeStreamRepository : StreamRepository {
    private val yt get() = ServiceList.YouTube

    override suspend fun search(query: String): List<SearchItem> = withContext(Dispatchers.IO) {
        val handler = yt.searchQHFactory.fromQuery(query, emptyList(), "")
        SearchInfo.getInfo(yt, handler).relatedItems.mapNotNull { item ->
            when (item) {
                is StreamInfoItem -> SearchItem(item.url, item.name, item.uploaderName ?: "", item.duration, item.thumbnails.firstOrNull()?.url, ResultType.VIDEO)
                is ChannelInfoItem -> SearchItem(item.url, item.name, "${item.subscriberCount} suscriptores", 0, item.thumbnails.firstOrNull()?.url, ResultType.CHANNEL)
                is PlaylistInfoItem -> SearchItem(item.url, item.name, item.uploaderName ?: "", 0, item.thumbnails.firstOrNull()?.url, ResultType.PLAYLIST)
                else -> null
            }
        }
    }

    override suspend fun streamDetails(url: String): StreamDetails = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(yt, url)

        val audio = info.audioStreams.firstOrNull { it.format == MediaFormat.M4A } ?: info.audioStreams.firstOrNull()
        val videoOnly = info.videoOnlyStreams.filter { it.format == MediaFormat.MPEG_4 || it.format == MediaFormat.WEBM }
        val muxed = info.videoStreams.filter { it.format == MediaFormat.MPEG_4 }

        val options = buildList {
            for (h in listOf(1080, 720, 480, 360)) {
                val vo = videoOnly.firstOrNull { it.height == h }
                val mx = muxed.firstOrNull { it.height == h }
                when {
                    vo != null && audio != null -> add(StreamOption("${h}p", vo.content, audio.content))
                    mx != null -> add(StreamOption("${h}p", mx.content, null))
                }
            }
            audio?.let { add(StreamOption(StreamOption.AUDIO_ONLY, null, it.content)) }
        }
        
        require(options.isNotEmpty()) { "No se encontraron streams de video reproducibles" }
        StreamDetails(info.name, info.uploaderName ?: "", info.thumbnails.firstOrNull()?.url, options)
    }
}
