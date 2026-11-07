package tj.soni.editor.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

object MediaSaver {
    fun saveVideo(ctx: Context, src: File, name: String): Uri =
        save(ctx, name, "video/mp4", MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MOVIES) { out ->
            src.inputStream().use { it.copyTo(out) }
        }

    fun saveJpeg(ctx: Context, bmp: Bitmap, name: String): Uri =
        save(ctx, name, "image/jpeg", MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_PICTURES) { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

    private fun save(ctx: Context, name: String, mime: String, collection: Uri, dir: String, write: (OutputStream) -> Unit): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/SoniEditor")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = ctx.contentResolver.insert(collection, values) ?: error("MediaStore insert failed")
        try {
            ctx.contentResolver.openOutputStream(uri)?.use(write) ?: error("Cannot open output")
            if (Build.VERSION.SDK_INT >= 29) {
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                ctx.contentResolver.update(uri, done, null, null)
            }
        } catch (e: Exception) {
            ctx.contentResolver.delete(uri, null, null)
            throw e
        }
        return uri
    }
}
