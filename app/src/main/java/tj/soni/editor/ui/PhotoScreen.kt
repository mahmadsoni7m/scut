package tj.soni.editor.ui

import android.graphics.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tj.soni.editor.R
import tj.soni.editor.core.MediaSaver

data class PhotoState(
    val brightness: Float = 0f,   // -1..1
    val contrast: Float = 1f,     // 0.5..1.5
    val saturation: Float = 1f,   // 0..2
    val rotation: Int = 0,
    val flip: Boolean = false
)

fun PhotoState.colorMatrixArray(): FloatArray {
    val t = (-0.5f * contrast + 0.5f) * 255f + brightness * 255f
    val con = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, t,
        0f, contrast, 0f, 0f, t,
        0f, 0f, contrast, 0f, t,
        0f, 0f, 0f, 1f, 0f
    ))
    val sat = ColorMatrix().apply { setSaturation(saturation) }
    val cm = ColorMatrix(sat)
    cm.postConcat(con)
    return cm.array
}

fun decodeBitmap(ctx: android.content.Context, uri: Uri, maxDim: Int): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
    ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
} catch (e: Exception) { null }

fun renderEdited(src: Bitmap, s: PhotoState): Bitmap {
    val m = Matrix()
    m.postRotate(s.rotation.toFloat())
    if (s.flip) m.postScale(-1f, 1f)
    val oriented = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    val out = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(s.colorMatrixArray())) }
    Canvas(out).drawBitmap(oriented, 0f, 0f, paint)
    return out
}

@Composable
fun PhotoScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var st by remember { mutableStateOf(PhotoState()) }
    val undo = remember { mutableStateListOf<PhotoState>() }
    val redo = remember { mutableStateListOf<PhotoState>() }
    var dragging by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val sSaved = stringResource(R.string.saved)
    val sFail = stringResource(R.string.export_failed)
    val sMissing = stringResource(R.string.media_missing)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            uri = u; st = PhotoState(); undo.clear(); redo.clear()
            scope.launch {
                bitmap = withContext(Dispatchers.IO) { decodeBitmap(ctx, u, 2048) }
                status = if (bitmap == null) sMissing else ""
            }
        }
    }

    fun commit(n: PhotoState) { undo.add(st); redo.clear(); st = n }
    fun slide(n: PhotoState) { if (!dragging) { undo.add(st); redo.clear(); dragging = true }; st = n }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { picker.launch(arrayOf("image/*")) }) { Text(stringResource(R.string.pick_photo)) }
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(), contentDescription = null,
                colorFilter = if (showOriginal) null else ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(st.colorMatrixArray())),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(300.dp).graphicsLayer {
                    if (!showOriginal) { rotationZ = st.rotation.toFloat(); scaleX = if (st.flip) -1f else 1f }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = undo.isNotEmpty(), onClick = { redo.add(st); st = undo.removeAt(undo.lastIndex) }) { Text(stringResource(R.string.undo)) }
                OutlinedButton(enabled = redo.isNotEmpty(), onClick = { undo.add(st); st = redo.removeAt(redo.lastIndex) }) { Text(stringResource(R.string.redo)) }
                FilterChip(selected = showOriginal, onClick = { showOriginal = !showOriginal }, label = { Text(stringResource(R.string.before_after)) })
            }
            Text(stringResource(R.string.brightness))
            Slider(st.brightness, { slide(st.copy(brightness = it)) }, valueRange = -0.5f..0.5f, onValueChangeFinished = { dragging = false })
            Text(stringResource(R.string.contrast))
            Slider(st.contrast, { slide(st.copy(contrast = it)) }, valueRange = 0.5f..1.5f, onValueChangeFinished = { dragging = false })
            Text(stringResource(R.string.saturation))
            Slider(st.saturation, { slide(st.copy(saturation = it)) }, valueRange = 0f..2f, onValueChangeFinished = { dragging = false })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { commit(st.copy(rotation = (st.rotation + 90) % 360)) }) { Text(stringResource(R.string.rotate)) }
                OutlinedButton(onClick = { commit(st.copy(flip = !st.flip)) }) { Text(stringResource(R.string.flip)) }
            }
            Button(onClick = {
                val u = uri ?: return@Button
                scope.launch {
                    status = try {
                        withContext(Dispatchers.IO) {
                            val full = decodeBitmap(ctx, u, 4096) ?: error(sMissing)
                            MediaSaver.saveJpeg(ctx, renderEdited(full, st), "SONI_${System.currentTimeMillis()}.jpg")
                        }
                        sSaved
                    } catch (e: Exception) { sFail + ": " + e.message }
                }
            }) { Text(stringResource(R.string.save)) }
        }
        if (status.isNotEmpty()) Text(status)
    }
}
