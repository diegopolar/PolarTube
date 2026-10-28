package com.polartube.app.data

class NewPipeStreamRepository : StreamRepository {
    private val yt get() = ServiceList.YouTube

    override suspend fun search(query: String): List<SearchItem> = withContext(Dispatchers.IO) {
        val handler = yt.searchQHFactory.fromQuery(query, emptyList(), "")
        SearchInfo.getInfo(yt, handler).relatedItems.mapNotNull { item ->
            when (item) {
                is StreamInfoItem -> SearchItem(item.url, item.name, item.uploaderName ?: "",
                    item.duration, item.thumbnails.firstOrNull()?.url, ResultType.VIDEO)
                is ChannelInfoItem -> SearchItem(item.url, item.name, "${item.subscriberCount} suscriptores",
                    0, item.thumbnails.firstOrNull()?.url, ResultType.CHANNEL)
                is PlaylistInfoItem -> SearchItem(item.url, item.name, item.uploaderName ?: "",
                    0, item.thumbnails.firstOrNull()?.url, ResultType.PLAYLIST)
                else -> null
            }
        }
    }

    override suspend fun streamDetails(url: String): StreamDetails = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(yt, url)
        fun <T : Stream> List<T>.progressive() = filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }

        val audio = info.audioStreams.progressive()
            .filter { it.format == MediaFormat.M4A }
            .maxByOrNull { it.averageBitrate }
        val videoOnly = info.videoOnlyStreams.progressive().filter { it.format == MediaFormat.MPEG_4 }
        val muxed = info.videoStreams.progressive().filter { it.format == MediaFormat.MPEG_4 }

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
        require(options.isNotEmpty()) { "No se encontraron streams reproducibles" }
        StreamDetails(info.name, info.uploaderName ?: "", info.thumbnails.firstOrNull()?.url, options)
    }
}
