package com.aitsuki.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.lang.Integer.max

object Compressor {

    fun compress(
        context: Context,
        file: File,
        maxResolution: Int = 1080,
        quality: Int = 70
    ): Result<File> {
        try {
            check(maxResolution > 0) { "maxResolution must greater than 0" }
            val resolution = getBitmapResolution(file)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = inSampleSizeFor(resolution / maxResolution)
                // based on BitmapFactory.setDensityFromOptions
                inScaled = resolution / inSampleSize > maxResolution
                inDensity = if (inScaled) resolution else 0
                inTargetDensity = maxResolution * inSampleSize
            }
            var bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            bitmap = transformBitmap(bitmap, getOrientation(file))
            val fe = getFormatAndExt(file)
            val tempFile = File.createTempFile("compressed", fe.second, context.cacheDir)
            if (tempFile.outputStream().use { bitmap.compress(fe.first, quality, it) }) {
                return Result.success(tempFile)
            }
            error("bitmap compress failed")
        } catch (t: Throwable) {
            return Result.failure(t)
        }
    }

    @Suppress("DEPRECATION")
    private fun getFormatAndExt(file: File): Pair<CompressFormat, String> {
        return when (file.extension.lowercase()) {
            "png" -> CompressFormat.PNG to ".png"
            "webp" -> CompressFormat.WEBP to ".webp"
            else -> CompressFormat.JPEG to ".jpg"
        }
    }

    private fun transformBitmap(src: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setScale(-1f, 1f)
                matrix.postRotate(180f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun getOrientation(file: File): Int {
        return ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
    }

    private fun inSampleSizeFor(scale: Int): Int {
        if (scale <= 1) return 1
        var n = scale
        n = n or (n ushr 1)
        n = n or (n ushr 2)
        n = n or (n ushr 4)
        n = n or (n ushr 8)
        n = n or (n ushr 16)
        return ++n ushr 1
    }

    private fun getBitmapResolution(file: File): Int {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return max(opts.outWidth, opts.outHeight)
    }
}