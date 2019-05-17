/**
 * Copyright 2018, FleetTLC. All rights reserved
 */
package com.cartlc.tracker.ui.util.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.AsyncTask
import android.os.Environment
import android.widget.ImageView
import com.cartlc.tracker.R

import com.cartlc.tracker.ui.app.TBApplication

import java.io.File
import java.io.FileOutputStream

import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * Created by dug on 6/1/17.
 */
object BitmapHelper {

    // See also R.dimen.image_full_max_height
    private const val SCALED_SIZE = 600
    private const val QUALITY = 100

    fun createScaledFilename(originalFilename: String): String {
        val pos = originalFilename.lastIndexOf('.')
        val sbuf = StringBuilder()
        sbuf.append(originalFilename.substring(0, pos))
        sbuf.append("_")
        sbuf.append(".jpg")
        return sbuf.toString()
    }

    fun createScaled(unscaledFile: File, scaledFilename: String): Boolean {
        try {
            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED != state) {
                Timber.e("No external media was mounted")
                return false
            }
            if (!unscaledFile.exists()) {
                Timber.e("File does not exist: $unscaledFile.absolutePath")
                return false
            }
            val bitmap = loadScaledFile(unscaledFile.absolutePath, SCALED_SIZE, SCALED_SIZE)
            val fos = FileOutputStream(scaledFilename)
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos)
            fos.close()

            bitmap.recycle()
        } catch (ex: Exception) {
            TBApplication.ReportError(ex, BitmapHelper::class.java, "createScaled()", unscaledFile.absolutePath)
            return false
        }
        return true
    }

    private fun loadScaledFile(pathname: String, dstWidth: Int, dstHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(pathname, options)
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, dstWidth, dstHeight)
        return BitmapFactory.decodeFile(pathname, options)
    }

    private fun calculateSampleSize(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): Int {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()
        return if (srcAspect > dstAspect) {
            srcWidth / dstWidth
        } else {
            srcHeight / dstHeight
        }
    }

    fun rotate(picture: File, degrees: Int) {
        try {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            val bitmap = BitmapFactory.decodeFile(picture.absolutePath)
            if (bitmap == null) {
                TBApplication.ReportError(picture.absolutePath, BitmapHelper::class.java, "rotate()", "bitmap")
                return
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val fos = FileOutputStream(picture.absoluteFile)
            rotated.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
        } catch (ex: Exception) {
            TBApplication.ReportError(ex, BitmapHelper::class.java, "rotate()", picture.toString())
        }
    }

    fun loadBitmap(pathname: String, dstHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(pathname, options)
        val origHeight = options.outHeight
        options.inJustDecodeBounds = false
        val sampleSize = 1f / (dstHeight.toFloat() / origHeight.toFloat())
        options.inSampleSize = sampleSize.roundToInt()
        return BitmapFactory.decodeFile(pathname, options)
    }

    private class LoadTask(
        imageView: ImageView,
        private val dstHeight: Int
    ) : AsyncTask<String, String, Bitmap>() {

        val ref = WeakReference<ImageView>(imageView)

        override fun onPreExecute() {
            ref.get()?.setImageResource(R.drawable.loading)
        }

        override fun doInBackground(vararg params: String): Bitmap {
            return loadBitmap(params[0], dstHeight)
        }

        override fun onPostExecute(result: Bitmap) {
            ref.get()?.setImageBitmap(result)
        }
    }

    fun loadBitmap(pathname: String, dstHeight: Int, imageView: ImageView) {
        LoadTask(imageView, dstHeight).execute(pathname)
    }


}
