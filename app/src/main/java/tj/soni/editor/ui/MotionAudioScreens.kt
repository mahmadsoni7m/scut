package tj.soni.editor.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import tj.soni.editor.R
import tj.soni.editor.core.Keyframe
import tj.soni.editor.core.KeyframeEngine

@Composable
fun MotionScreen() {
    val frames = remember {
        mutableStateListOf(
            Keyframe(0f, -0.6f, 0f, 0.6f, 0f, 0.2f),
            Keyframe(1f, 0f, -0.4f, 1.2f, 180f, 1f),
            Keyframe(2f, 0.6f, 0f, 0.6f, 360f, 0.2f)
        )
    }
    var time by remember { mutableStateOf(0f) }
    var playing by remember { mutableStateOf(false) }
    var sel by remember { mutableStateOf(0) }
    val total = frames.last().t

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanosCompat()
        while (playing) {
            val now = withFrameNanosCompat()
            time += (now - last) / 1_000_000_000f
            last = now
            if (time >= total) { time = total; playing = false }
        }
    }
    val cur = KeyframeEngine.sample(frames, time)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(220.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            val w = maxWidth; val h = maxHeight
            Box(
                Modifier.size(72.dp)
                    .graphicsLayer {
                        translationX = cur.x * w.toPx() / 2f
                        translationY = cur.y * h.toPx() / 2f
                        scaleX = cur.scale; scaleY = cur.scale; rotationZ = cur.rotation
                    }
                    .alpha(cur.opacity.coerceIn(0f, 1f))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (time >= total) time = 0f; playing = !playing }) {
                Text(stringResource(if (playing) R.string.pause else R.string.play))
            }
            Text(stringResource(R.string.motion_time) + ": " + "%.2f".format(time))
        }
        Slider(time, { time = it; playing = false }, valueRange = 0f..total)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            frames.forEachIndexed { i, f ->
                FilterChip(selected = sel == i, onClick = { sel = i; time = f.t }, label = { Text(stringResource(R.string.keyframe) + " ${f.t.toInt()}s") })
            }
        }
        val k = frames[sel]
        @Composable fun row(label: Int, v: Float, range: ClosedFloatingPointRange<Float>, set: (Float) -> Keyframe) {
            Text(stringResource(label))
            Slider(v, { frames[sel] = set(it) }, valueRange = range)
        }
        row(R.string.pos_x, k.x, -1f..1f) { k.copy(x = it) }
        row(R.string.pos_y, k.y, -1f..1f) { k.copy(y = it) }
        row(R.string.scale, k.scale, 0.2f..2f) { k.copy(scale = it) }
        row(R.string.rotation, k.rotation, 0f..720f) { k.copy(rotation = it) }
        row(R.string.opacity, k.opacity, 0f..1f) { k.copy(opacity = it) }
    }
}

private suspend fun withFrameNanosCompat(): Long = androidx.compose.runtime.withFrameNanos { it }

@Composable
fun AudioScreen() {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var volume by remember { mutableStateOf(1f) }
    var playing by remember { mutableStateOf(false) }
    val player = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(volume) { player.volume = volume }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            uri = u; player.setMediaItem(MediaItem.fromUri(u)); player.prepare(); playing = false
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { picker.launch(arrayOf("audio/*")) }) { Text(stringResource(R.string.pick_audio)) }
        if (uri != null) {
            Button(onClick = { if (playing) player.pause() else player.play(); playing = !playing }) {
                Text(stringResource(if (playing) R.string.pause else R.string.play))
            }
            Text(stringResource(R.string.volume))
            Slider(volume, { volume = it }, valueRange = 0f..1f)
        }
    }
}
