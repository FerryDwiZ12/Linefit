package com.example.linefit.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.linefit.data.ActivitySummary
import com.example.linefit.renderer.MercatorProjector
import com.example.linefit.renderer.NeonRouteRenderer
import com.example.linefit.renderer.RouteStyleConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PngExporter {

    /**
     * Renders high-resolution transparent PNG bitmap of the activity route & overlay.
     */
    fun createOverlayBitmap(
        summary: ActivitySummary,
        config: RouteStyleConfig,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): Bitmap {
        // Adjust resolution based on aspect ratio
        val (finalWidth, finalHeight) = when (config.aspectRatio) {
            com.example.linefit.renderer.AspectRatioOption.STORY_9_16 -> Pair(1080, 1920)
            com.example.linefit.renderer.AspectRatioOption.SQUARE_1_1 -> Pair(1080, 1080)
            com.example.linefit.renderer.AspectRatioOption.FEED_4_5 -> Pair(1080, 1350)
        }

        val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clear canvas to ensure transparent background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Project track points onto canvas dimensions
        val projectedPoints = MercatorProjector.projectToCanvas(
            summary.trackPoints,
            finalWidth.toFloat(),
            finalHeight.toFloat(),
            paddingPercentage = 0.15f
        )

        // Render neon route & overlay (with transparent background)
        NeonRouteRenderer.renderOverlay(
            canvas = canvas,
            width = finalWidth.toFloat(),
            height = finalHeight.toFloat(),
            summary = summary,
            projectedPoints = projectedPoints,
            config = config,
            isTransparentBg = true
        )

        return bitmap
    }

    /**
     * Saves the bitmap to device gallery (Pictures/PathCraft) as a PNG file.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        val fileName = "PathCraft_${System.currentTimeMillis()}.png"
        val mimeType = "image/png"

        val resolver = context.contentResolver
        val imageUri: Uri?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PathCraft")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            imageUri = resolver.insert(collection, contentValues)

            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val pathCraftFolder = File(picturesDir, "PathCraft")
            if (!pathCraftFolder.exists()) {
                pathCraftFolder.mkdirs()
            }
            val imageFile = File(pathCraftFolder, fileName)
            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }

        return imageUri
    }

    /**
     * Triggers Intent to share the saved PNG file.
     */
    fun shareImage(context: Context, imageUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PathCraft Overlay"))
    }
}
