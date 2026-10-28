import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
package com.polartube.app.data

import android.app.*
import android.content.*
import android.net.Uri
import android.os.*
import android.util.Rational
import android.Manifest
import androidx.activity.*
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
    import androidx.media3.datasource.*
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.source.*
import androidx.media3.session.*
import androidx.media3.ui.*
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.google.common.util.concurrent.*
import com.polartube.app.*
import com.polartube.app.data.*
import com.polartube.app.domain.*
import com.polartube.app.playback.*
import com.polartube.app.ui.player.*
import com.polartube.app.ui.search.*
import com.polartube.app.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.*
import org.schabi.newpipe.extractor.downloader.*
import org.schabi.newpipe.extractor.exceptions.*
import org.schabi.newpipe.extractor.localization.*
import org.schabi.newpipe.extractor.search.*
import org.schabi.newpipe.extractor.stream.*


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
