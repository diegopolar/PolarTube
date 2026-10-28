package com.polartube.app.ui.player

data class PlayerUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val channel: String = "",
    val options: List<StreamOption> = emptyList(),
    val selectedLabel: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
) {
    val isAudioOnly get() = selectedLabel == StreamOption.AUDIO_ONLY
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val getStream = GetStreamUseCase((app as PolarTubeApp).repository)
    private val connection = PlayerConnection(app)
    val player: StateFlow<Player?> = connection.controller   // para PlayerView

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui = _ui.asStateFlow()

    private var details: StreamDetails? = null
    private var currentUrl: String? = null
    private val controller get() = connection.controller.value

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = _ui.update { it.copy(isPlaying = isPlaying) }
        override fun onPlaybackStateChanged(state: Int) =
            _ui.update { it.copy(isBuffering = state == Player.STATE_BUFFERING) }
        override fun onPlayerError(error: PlaybackException) =
            _ui.update { it.copy(error = error.message, loading = false) }
    }

    init {
        viewModelScope.launch {
            connection.controller.filterNotNull().first().let { c ->
                c.addListener(listener)
                _ui.update { it.copy(isPlaying = c.isPlaying) }
            }
        }
        viewModelScope.launch {   // ticker de progreso
            while (isActive) {
                controller?.takeIf { it.isPlaying }?.let { c ->
                    _ui.update { it.copy(positionMs = c.currentPosition, durationMs = c.duration.coerceAtLeast(0)) }
                }
                delay(500)
            }
        }
    }

    fun load(url: String) {
        if (url == currentUrl) return          // evita reiniciar al volver de PiP / rotar
        currentUrl = url
        viewModelScope.launch {
            _ui.value = PlayerUiState(loading = true)
            runCatching { getStream(url) }
                .onSuccess { d ->
                    details = d
                    _ui.update { it.copy(title = d.title, channel = d.uploader, options = d.options) }
                    val default = d.options.firstOrNull { it.label == "720p" } ?: d.options.first()
                    play(default, 0L)
                }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Error al extraer el video") } }
        }
    }

    fun setQuality(label: String) {
        val opt = details?.options?.firstOrNull { it.label == label } ?: return
        viewModelScope.launch { play(opt, controller?.currentPosition ?: 0L) }   // conserva la posición
    }

    private suspend fun play(opt: StreamOption, startMs: Long) {
        val c = connection.controller.filterNotNull().first()
        val d = details ?: return
        val args = Bundle().apply {
            putString(PlaybackCommands.K_VIDEO, opt.videoUrl)
            putString(PlaybackCommands.K_AUDIO, opt.audioUrl)
            putString(PlaybackCommands.K_TITLE, d.title)
            putString(PlaybackCommands.K_ARTIST, d.uploader)
            putString(PlaybackCommands.K_ART, d.thumbnail)
            putLong(PlaybackCommands.K_POS, startMs)
        }
        c.sendCustomCommand(PlaybackCommands.SET_STREAM, args)
        _ui.update { it.copy(loading = false, selectedLabel = opt.label, error = null) }
    }

    fun togglePlay() = controller?.let { if (it.isPlaying) it.pause() else it.play() }
    fun pause() = controller?.pause()
    fun seekTo(ms: Long) = controller?.seekTo(ms)
    fun skip(deltaMs: Long) = controller?.let { it.seekTo((it.currentPosition + deltaMs).coerceAtLeast(0)) }

    /** Ahorra batería: desactiva la decodificación de video cuando no hay pantalla visible */
    fun setVideoEnabled(enabled: Boolean) {
        val c = controller ?: return
        c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !enabled).build()
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        connection.release()   // libera el controller; el servicio sigue vivo si hay reproducción
    }
}
