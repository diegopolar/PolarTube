package com.polartube.app.playback

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


@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA).build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)   // pausa al desconectar auriculares
            .setWakeMode(C.WAKE_MODE_NETWORK)    // mantiene CPU y WiFi con pantalla apagada
            .build()
        session = MediaSession.Builder(this, player).setCallback(Callback(player)).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }

    private class Callback(private val player: ExoPlayer) : MediaSession.Callback {
        private val dataSource = DefaultHttpDataSource.Factory()
            .setUserAgent("PolarTube/1.0")
            .setAllowCrossProtocolRedirects(true)
        private val factory = ProgressiveMediaSource.Factory(dataSource)

        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(PlaybackCommands.SET_STREAM).build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands).build()
        }

        override fun onCustomCommand(
            session: MediaSession, controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand, args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != PlaybackCommands.SET_STREAM.customAction)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))

            val video = args.getString(PlaybackCommands.K_VIDEO)
            val audio = args.getString(PlaybackCommands.K_AUDIO)
            val meta = MediaMetadata.Builder()
                .setTitle(args.getString(PlaybackCommands.K_TITLE))
                .setArtist(args.getString(PlaybackCommands.K_ARTIST))
                .setArtworkUri(args.getString(PlaybackCommands.K_ART)?.toUri())
                .build()
            fun item(url: String, withMeta: Boolean) = MediaItem.Builder()
                .setUri(url).apply { if (withMeta) setMediaMetadata(meta) }.build()

            // La fuente con metadatos va primero: MergingMediaSource expone el MediaItem de la primera
            val sources = buildList {
                audio?.let { add(factory.createMediaSource(item(it, true))) }
                video?.let { add(factory.createMediaSource(item(it, audio == null))) }
            }
            val source = if (sources.size == 1) sources[0] else MergingMediaSource(*sources.toTypedArray())

            player.setMediaSource(source, args.getLong(PlaybackCommands.K_POS, 0L))
            player.prepare()
            player.playWhenReady = true
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
