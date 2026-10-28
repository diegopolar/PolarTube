package com.polartube.app

class PolarTubeApp : Application() {
    val repository: StreamRepository by lazy { NewPipeStreamRepository() }

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(OkHttpDownloader(OkHttpClient.Builder().build()), Localization("es", "PE"))
    }
}
