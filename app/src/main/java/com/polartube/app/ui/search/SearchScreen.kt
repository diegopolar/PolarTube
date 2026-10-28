package com.polartube.app.ui.search

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
