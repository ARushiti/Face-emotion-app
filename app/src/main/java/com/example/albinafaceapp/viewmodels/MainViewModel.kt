package com.example.albinafaceapp.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.Face
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class MainViewModel : ViewModel() {
    val imagebitmapLiveData = MutableLiveData<Bitmap>()
    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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


}