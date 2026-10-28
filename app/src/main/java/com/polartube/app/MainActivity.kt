package com.polartube.app

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


class MainActivity : ComponentActivity() {
    private val playerVm: PlayerViewModel by viewModels()   // scope de Activity: sobrevive a la navegación
    private var isInPip by mutableStateOf(false)
    private var pipEligible = false

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        addOnPictureInPictureModeChangedListener { info -> isInPip = info.isInPictureInPictureMode }

        setContent {
            PolarTubeTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "search") {
                    composable("search") {
                        SearchScreen(onVideoClick = { nav.navigate("player/${Uri.encode(it)}") })
                    }
                    composable("player/{url}") { entry ->
                        PlayerScreen(
                            url = entry.arguments!!.getString("url")!!,
                            vm = playerVm,
                            isInPip = isInPip,
                            onEnterPip = ::enterPip,
                            onPipEligible = ::setPipEligible,
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    // ---- PiP ----
    private fun pipParams(autoEnter: Boolean) = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .apply {
            if (Build.VERSION.SDK_INT >= 31) {
                setAutoEnterEnabled(autoEnter)       // entra solo al hacer gesto home/recientes
                setSeamlessResizeEnabled(true)
            }
        }.build()

    private fun setPipEligible(eligible: Boolean) {
        pipEligible = eligible
        setPictureInPictureParams(pipParams(eligible))   // mantiene el auto-enter actualizado
    }

    private fun enterPip() { if (pipEligible) enterPictureInPictureMode(pipParams(true)) }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (pipEligible && Build.VERSION.SDK_INT < 31) enterPip()   // Android 8–11
    }

    // ---- Ciclo de vida ----
    override fun onStart() {
        super.onStart()
        playerVm.setVideoEnabled(true)
    }

    override fun onStop() {
        super.onStop()
        if (isInPictureInPictureMode) playerVm.pause()   // el usuario cerró la ventana PiP (X)
        else playerVm.setVideoEnabled(false)             // minimizada: solo audio, sin decodificar video
    }
}
