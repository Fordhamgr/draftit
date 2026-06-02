package com.flash.draft.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors

object StickerMaker {
    // Run heavy image processing off the main UI thread
    private val executor = Executors.newSingleThreadExecutor()

    fun extractSubject(context: Context, originalUriString: String, onResult: (String?) -> Unit) {
        executor.execute {
            try {
                val uri = Uri.parse(originalUriString)

                // 1. Get the original Bitmap safely
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                // ML Kit requires a software bitmap (not hardware accelerated)
                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                // 2. Configure the AI to grab the subject and output a transparent Bitmap
                val options = SubjectSegmenterOptions.Builder()
                    .enableForegroundBitmap()
                    .build()

                val segmenter = SubjectSegmentation.getClient(options)
                val inputImage = InputImage.fromBitmap(softwareBitmap, 0)

                // 3. Process the image
                segmenter.process(inputImage)
                    .addOnSuccessListener { result ->
                        val fgBitmap = result.foregroundBitmap
                        if (fgBitmap == null) {
                            (context as? Activity)?.runOnUiThread { onResult(null) }
                            return@addOnSuccessListener
                        }

                        // 4. Save the new transparent sticker to the cache directory
                        executor.execute {
                            try {
                                val file = File(context.cacheDir, "sticker_${UUID.randomUUID()}.png")
                                FileOutputStream(file).use { out ->
                                    fgBitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // PNG preserves transparency
                                }
                                (context as? Activity)?.runOnUiThread {
                                    onResult(Uri.fromFile(file).toString())
                                }
                            } catch (e: Exception) {
                                (context as? Activity)?.runOnUiThread { onResult(null) }
                            }
                        }
                    }
                    .addOnFailureListener {
                        (context as? Activity)?.runOnUiThread { onResult(null) }
                    }

            } catch (e: Exception) {
                e.printStackTrace()
                (context as? Activity)?.runOnUiThread { onResult(null) }
            }
        }
    }
}