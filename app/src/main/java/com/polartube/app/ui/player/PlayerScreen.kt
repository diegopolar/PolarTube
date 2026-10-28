package com.polartube.app.ui.player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    url: String,
    vm: PlayerViewModel,
    isInPip: Boolean,
    onEnterPip: () -> Unit,
    onPipEligible: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(url) { vm.load(url) }

    // PiP solo tiene sentido si hay video reproduciéndose
    LaunchedEffect(ui.isPlaying, ui.isAudioOnly) { onPipEligible(ui.isPlaying && !ui.isAudioOnly) }
    DisposableEffect(Unit) { onDispose { onPipEligible(false) } }

    // Auto-ocultar controles
    LaunchedEffect(showControls, ui.isPlaying) {
        if (showControls && ui.isPlaying) { delay(3000); showControls = false }
    }

    val videoSurface: @Composable (Modifier) -> Unit = { modifier ->
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false                       // controles propios en Compose
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { it.player = player },
            onRelease = { it.player = null },
        )
    }

    // En PiP: solo el video, sin UI
    if (isInPip) {
        Box(Modifier.fillMaxSize().background(Color.Black)) { videoSurface(Modifier.fillMaxSize()) }
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16 / 9f).background(Color.Black)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    showControls = !showControls
                }
        ) {
            videoSurface(Modifier.fillMaxSize())

            if (ui.loading || ui.isBuffering) CircularProgressIndicator(Modifier.align(Alignment.Center))

            AnimatedVisibility(showControls, enter = fadeIn(), exit = fadeOut()) {
                PlayerControls(ui, vm, onBack, onEnterPip)
            }
        }

        Column(Modifier.padding(16.dp)) {
            Text(ui.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(ui.channel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ui.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PlayerControls(ui: PlayerUiState, vm: PlayerViewModel, onBack: () -> Unit, onEnterPip: () -> Unit) {
    var qualityMenu by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf<Float?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
        // Barra superior
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEnterPip) { Icon(Icons.Default.PictureInPictureAlt, "Pantalla flotante", tint = Color.White) }
            Box {
                TextButton(onClick = { qualityMenu = true }) {
                    Icon(Icons.Default.Settings, null, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text(ui.selectedLabel.ifEmpty { "Calidad" }, color = Color.White)
                }
                DropdownMenu(qualityMenu, { qualityMenu = false }) {
                    ui.options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.label) },
                            leadingIcon = { if (opt.label == ui.selectedLabel) Icon(Icons.Default.Check, null) },
                            onClick = { qualityMenu = false; vm.setQuality(opt.label) },
                        )
                    }
                }
            }
        }

        // Centro
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.skip(-10_000) }) { Icon(Icons.Default.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(36.dp)) }
            IconButton(onClick = vm::togglePlay, modifier = Modifier.size(64.dp)) {
                Icon(if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pausa", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            IconButton(onClick = { vm.skip(10_000) }) { Icon(Icons.Default.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(36.dp)) }
        }

        // Barra de progreso
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(dragging?.toLong() ?: ui.positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Slider(
                value = dragging ?: ui.positionMs.toFloat(),
                onValueChange = { dragging = it },
                onValueChangeFinished = { dragging?.let { vm.seekTo(it.toLong()) }; dragging = null },
                valueRange = 0f..ui.durationMs.coerceAtLeast(1).toFloat(),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text(formatTime(ui.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun formatTime(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
