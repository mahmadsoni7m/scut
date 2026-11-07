package tj.soni.editor.ui

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.*
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tj.soni.editor.R
import tj.soni.editor.core.MediaSaver
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun VideoScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val opened = vm.opened
    val init = remember(opened) {
        runCatching { JSONObject(opened!!.data) }.getOrNull()?.takeIf { it.optString("type") == "video" }
    }
    var uri by remember(opened) { mutableStateOf(init?.optString("uri")?.takeIf { it.isNotEmpty() }?.let(Uri::parse)) }
    var dur by remember(opened) { mutableStateOf(0f) }
    var range by remember(opened) { mutableStateOf(0f..0f) }
    var bright by remember(opened) { mutableStateOf(init?.optDouble("b", 0.0)?.toFloat() ?: 0f) }
    var contrast by remember(opened) { mutableStateOf(init?.optDouble("c", 0.0)?.toFloat() ?: 0f) }
    var missing by remember(opened) { mutableStateOf(false) }
    var transformer by remember { mutableStateOf<Transformer?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("") }

    val sDone = stringResource(R.string.export_done)
    val sFail = stringResource(R.string.export_failed)
    val sSaved = stringResource(R.string.saved)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            uri = u
        }
    }

    val player = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(uri) {
        val u = uri ?: return@LaunchedEffect
        val d = withContext(Dispatchers.IO) {
            try {
                val r = MediaMetadataRetriever()
                r.setDataSource(ctx, u)
                val v = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                r.release()
                v
            } catch (e: Exception) { 0L }
        }
        missing = d <= 0
        dur = d.toFloat()
        val s = init?.optDouble("start", 0.0)?.toFloat() ?: 0f
        val e = init?.optDouble("end", 0.0)?.toFloat() ?: 0f
        range = if (e > s && e <= dur) s..e else 0f..dur
        if (!missing) { player.setMediaItem(MediaItem.fromUri(u)); player.prepare() }
    }

    fun json(): String = JSONObject().apply {
        put("type", "video"); put("uri", uri.toString())
        put("start", range.start.toDouble()); put("end", range.endInclusive.toDouble())
        put("b", bright.toDouble()); put("c", contrast.toDouble())
    }.toString()

    // Autosave into the opened project.
    LaunchedEffect(uri, range, bright, contrast) {
        delay(800)
        val p = vm.opened
        if (p != null && uri != null && !missing) vm.saveData(p, json())
    }

    // Export progress polling.
    LaunchedEffect(transformer) {
        val t = transformer ?: return@LaunchedEffect
        val holder = ProgressHolder()
        while (transformer === t) {
            if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) progress = holder.progress / 100f
            delay(300)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { picker.launch(arrayOf("video/*")) }) { Text(stringResource(R.string.pick_video)) }
        if (missing) Text(stringResource(R.string.media_missing), color = MaterialTheme.colorScheme.error)
        if (uri != null && !missing) {
            AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxWidth().height(220.dp))
            Text(stringResource(R.string.trim) + ": ${(range.start / 1000).toInt()}s – ${(range.endInclusive / 1000).toInt()}s")
            RangeSlider(value = range, onValueChange = { range = it }, valueRange = 0f..dur)
            Text(stringResource(R.string.brightness))
            Slider(value = bright, onValueChange = { bright = it }, valueRange = -0.5f..0.5f)
            Text(stringResource(R.string.contrast))
            Slider(value = contrast, onValueChange = { contrast = it }, valueRange = -0.5f..0.5f)
            Text(stringResource(R.string.export_note), style = MaterialTheme.typography.bodySmall)
            val exporting = transformer != null
            if (exporting) {
                LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                Text(stringResource(R.string.exporting))
                OutlinedButton(onClick = { transformer?.cancel(); transformer = null }) { Text(stringResource(R.string.cancel)) }
            } else {
                Button(onClick = {
                    status = ""; progress = 0f
                    val u = uri ?: return@Button
                    val media = MediaItem.Builder().setUri(u).setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(range.start.toLong())
                            .setEndPositionMs(range.endInclusive.toLong()).build()
                    ).build()
                    val fx = mutableListOf<Effect>()
                    if (bright != 0f) fx += Brightness(bright)
                    if (contrast != 0f) fx += Contrast(contrast)
                    val edited = EditedMediaItem.Builder(media).setEffects(Effects(emptyList(), fx)).build()
                    val out = File(ctx.cacheDir, "export_${System.currentTimeMillis()}.mp4")
                    val t = Transformer.Builder(ctx)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                transformer = null
                                scope.launch {
                                    status = try {
                                        withContext(Dispatchers.IO) { MediaSaver.saveVideo(ctx, out, "SONI_${System.currentTimeMillis()}.mp4"); out.delete() }
                                        sDone
                                    } catch (e: Exception) { sFail + ": " + e.message }
                                }
                            }
                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                transformer = null
                                status = sFail + ": " + exportException.message
                            }
                        }).build()
                    transformer = t
                    t.start(edited, out.absolutePath)
                }) { Text(stringResource(R.string.export)) }
            }
            if (vm.opened == null) {
                OutlinedButton(onClick = { vm.create("Video " + System.currentTimeMillis() % 100000, json()); status = sSaved }) {
                    Text(stringResource(R.string.save_project))
                }
            }
            if (status.isNotEmpty()) Text(status)
        }
    }
}
