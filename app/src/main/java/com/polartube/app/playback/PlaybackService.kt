package com.polartube.app.playback

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
            .setUserAgent(OkHttpDownloader.USER_AGENT)
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
