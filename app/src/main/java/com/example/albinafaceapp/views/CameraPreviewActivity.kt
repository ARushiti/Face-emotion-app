package com.example.albinafaceapp.views

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.albinafaceapp.utilities.Constants
import com.example.albinafaceapp.R
import com.example.albinafaceapp.databinding.CameraPreviewBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraPreviewActivity : AppCompatActivity() {
    private lateinit var binding: CameraPreviewBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CameraPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startCamera()
        outputDirectory = getOutputDirectory()
        binding.takePhoto.setOnClickListener {
            takePhoto()
        }
        binding.switcher.setOnClickListener {
            cameraSwitcher()
        }


    }

    private fun cameraSwitcher() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        startCamera()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }

        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)

                }
            imageCapture = ImageCapture.Builder()
                .build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (e: Exception) {
                Log.d(Constants.TAG, "startCamera Fail: ", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = " Photo saved"
                    Toast.makeText(this@CameraPreviewActivity, "$msg $savedUri", Toast.LENGTH_LONG)
                        .show()
                    val intent = Intent(this@CameraPreviewActivity, MainActivity::class.java)
                    photoUri = savedUri.toString()
                    intent.putExtra(photoURI, savedUri)
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG, "onError: ${exception.message}", exception)
                }
            })
    }

    companion object {
        const val photoURI = "photoUri"
        var photoUri: String = ""
    }
}