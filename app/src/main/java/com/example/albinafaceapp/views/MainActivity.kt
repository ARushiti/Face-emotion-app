package com.example.albinafaceapp.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.example.albinafaceapp.Constants
import com.example.albinafaceapp.databinding.ActivityMainBinding
import com.example.albinafaceapp.viewmodels.FaceImageHelper
import com.example.albinafaceapp.viewmodels.MainViewModel
import com.example.albinafaceapp.views.CameraPreviewActivity.Companion.photoUri
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val CameraRequestCode = 1
    private val GalleryRequestCode = 2
    private lateinit var mainViewModel: MainViewModel


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.imagebitmapLiveData.observe(this) {
            binding.imageView.load(it)
        }

        binding.btnCamera.setOnClickListener {
            cameraCheckPermission()
        }
        binding.btnGallery.setOnClickListener {
            galleryCheckPermission()
        }
        takePhotoFromIntent()

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun takePhotoFromIntent() {
        if (!photoUri.isEmpty()) {
            val uri = photoUri.toUri()
            val src = ImageDecoder.createSource(contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(src)
            binding.imageView.load(bitmap)
            mainViewModel.detectAndFrame(bitmap)
        }
    }


    private fun showDialogForCamera() {
        AlertDialog.Builder(this)
            .setMessage("Choose the camera")
            .setPositiveButton("Camera APP") { _, _ ->
                dispatchTakePictureIntent()
            }.setNeutralButton("Camera Preview") { _, _ ->
                startActivity(Intent(this, CameraPreviewActivity::class.java))
            }.create().show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                // startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }


    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED

        }


    private fun galleryCheckPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    gallery()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        this@MainActivity,
                        "You have denied the storage permission to select image",
                        Toast.LENGTH_SHORT
                    ).show()
                    showRationalDialogForPermission()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogForPermission()

                }

            }).onSameThread().check()
    }

    private fun gallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GalleryRequestCode)
    }

    private fun cameraCheckPermission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    //    dispatchTakePictureIntent()
                    showDialogForCamera()

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        applicationContext,
                        "To use the camera function you need to grant the camera permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    FaceImageHelper.createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, CameraRequestCode)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val uri = data?.data
            when (requestCode) {
                CameraRequestCode -> {
                    uri?.let {
                        val src = ImageDecoder.createSource(contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(src)
                        binding.imageView.load(bitmap)
                        mainViewModel.detectAndFrame(bitmap)
                    }
                }

                GalleryRequestCode -> {
                    val src = ImageDecoder.createSource(contentResolver, data?.data!!)
                    val bitmap = ImageDecoder.decodeBitmap(src)
                    binding.imageView.load(bitmap)
                    mainViewModel.detectAndFrame(bitmap)


                }
            }
        }
    }

    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions" + "required for this feature.It can be enable under App settings!!!")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)

                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()

                }
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}
