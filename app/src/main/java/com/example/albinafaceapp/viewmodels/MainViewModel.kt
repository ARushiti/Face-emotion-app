package com.example.albinafaceapp.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.albinafaceapp.viewmodels.FaceImageHelper.currentPhotoPath
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.Face
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException


class MainViewModel : ViewModel() {
    val imagebitmapLiveData = MutableLiveData<Bitmap>()
    val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiEndpoint = "https://faceappylli.cognitiveservices.azure.com/face/v1.0/"
    private val subscriptionKey = "99434426dbe44124b827429ef40111a2"
    private val faceServiceClient: FaceServiceRestClient =
        FaceServiceRestClient(apiEndpoint, subscriptionKey)

    fun detectAndFrame(imageBitmap: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        viewModelScope.launch(Dispatchers.IO) {
            val result: Array<Face> =
                faceServiceClient.detect(
                    inputStream,
                    true,  // returnFaceId
                    false,  // returnFaceLandmarks
                    arrayOf(FaceServiceClient.FaceAttributeType.Emotion) // returnFaceAttributes:
                )
            viewModelScope.launch(Dispatchers.Main) {
                imagebitmapLiveData.postValue(
                    FaceImageHelper.drawFaceRectanglesOnBitmap(
                        imageBitmap,
                        result
                    )
                )
            }
        }
    }

    fun setPic() {
        BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(currentPhotoPath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val orientation = exif!!.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            val bmRotated = FaceImageHelper.rotateBitmap(bitmap, orientation)
            imagebitmapLiveData.postValue(bmRotated)
            if (bmRotated != null) {
                detectAndFrame(bmRotated)
            }
        }
    }

}