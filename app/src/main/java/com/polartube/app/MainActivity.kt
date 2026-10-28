package com.polartube.app

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
