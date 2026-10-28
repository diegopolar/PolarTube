package com.polartube.app.playback

class PlayerConnection(context: Context) {
    private val future = MediaController.Builder(
        context, SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller = _controller.asStateFlow()

    init {
        future.addListener({ _controller.value = future.get() }, MoreExecutors.directExecutor())
    }
    fun release() = MediaController.releaseFuture(future)
}
