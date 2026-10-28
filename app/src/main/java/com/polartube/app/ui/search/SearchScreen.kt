package com.polartube.app.ui.search

@Composable
fun SearchScreen(onVideoClick: (String) -> Unit, vm: SearchViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = ui.query, onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Buscar videos, canales, playlists") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit() }),
        )
        if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }

        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(ui.results, key = { it.url }) { item ->
                ResultRow(item, onClick = { if (item.type == ResultType.VIDEO) onVideoClick(item.url) })
            }
        }
    }
}

@Composable
private fun ResultRow(item: SearchItem, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Box(Modifier.width(160.dp).aspectRatio(16 / 9f).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
            AsyncImage(item.thumbnail, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (item.type == ResultType.VIDEO && item.durationSec > 0) {
                Text(formatTime(item.durationSec * 1000), color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .background(Color.Black.copy(0.75f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(item.channel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.type != ResultType.VIDEO)
                AssistChip(onClick = {}, label = { Text(if (item.type == ResultType.CHANNEL) "Canal" else "Playlist") })
        }
    }
}
