package com.example.albinafaceapp.viewmodels

import android.graphics.*
import android.media.ExifInterface
import android.os.Environment
import com.example.albinafaceapp.application.FaceApp
import com.microsoft.projectoxford.face.contract.Face
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FaceImageHelper {
    lateinit var currentPhotoPath: String

    fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = FaceApp.instance.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1F, 1F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180F)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90F)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90F)
            else -> return bitmap
        }
        return try {
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            return bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }


    fun drawFaceRectanglesOnBitmap(

        originalBitmap: Bitmap, faces: Array<Face>
    ): Bitmap {


        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.strokeWidth = 10f
        val textPaint = Paint().apply {
            color = Color.RED
            textSize = 80f
        }
        for (face in faces) {
            val faceRectangle = face?.faceRectangle
            if (faceRectangle != null) {
                canvas.drawRect(
                    faceRectangle.left.toFloat(),
                    faceRectangle.top.toFloat(), (
                            faceRectangle.left + faceRectangle.width).toFloat(), (
                            faceRectangle.top + faceRectangle.height).toFloat(),
                    paint
                )
                canvas.drawText(
                    getEmotion(face),
                    (faceRectangle.left + 50).toFloat(),
                    faceRectangle.top + (faceRectangle.height + 100f),
                    textPaint
                )
            }
        }
        return bitmap
    }

    private fun getEmotion(face: Face): String {
        var emotion = " "
        var max = 0.0
        if (face.faceAttributes.emotion.anger > max) {
            max = face.faceAttributes.emotion.anger
            emotion = "angry"
        }
        if (face.faceAttributes.emotion.contempt > max) {
            max = face.faceAttributes.emotion.contempt
            emotion = "feeling contempt"
        }
        if (face.faceAttributes.emotion.disgust > max) {
            max = face.faceAttributes.emotion.disgust
            emotion = "disgusted"
        }
        if (face.faceAttributes.emotion.fear > max) {
            max = face.faceAttributes.emotion.fear
            emotion = "scared"
        }
        if (face.faceAttributes.emotion.happiness > max) {
            max = face.faceAttributes.emotion.happiness
            emotion = "happy"
        }
        if (face.faceAttributes.emotion.neutral > max) {
            max = face.faceAttributes.emotion.neutral
            emotion = "neutral"
        }
        if (face.faceAttributes.emotion.sadness > max) {
            max = face.faceAttributes.emotion.sadness
            emotion = "sad"
        }
        if (face.faceAttributes.emotion.surprise > max) {
            max = face.faceAttributes.emotion.surprise
            emotion = "surprised"
        }
        return emotion


    }
}