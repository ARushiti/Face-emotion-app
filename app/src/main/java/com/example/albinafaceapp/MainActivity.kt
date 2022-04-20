package com.example.albinafaceapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.albinafaceapp.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.Face
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val apiEndpoint = "https://faceappylli.cognitiveservices.azure.com/face/v1.0/"
    private val subscriptionKey = "99434426dbe44124b827429ef40111a2"
    private val faceServiceClient: FaceServiceRestClient =
        FaceServiceRestClient(apiEndpoint, subscriptionKey)

    private lateinit var binding: ActivityMainBinding
    lateinit var currentPhotoPath: String
    private val CAMERA_REQUEST_CODE = 1
    private val GALLERY_REQUEST_CODE = 2
    //  val PICK_IMG = 2
//    private val scope: CoroutineScope by lazy {
//        CoroutineScope(SupervisorJob() + Dispatchers.IO) + CoroutineExceptionHandler { _, throwable ->
//            Log.d(
//                TAG,
//                "$throwable: "
//            )
//        }
//    }

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCamera.setOnClickListener {
            cameraCheckPremission()
            //dispatchTakePictureIntent()
        }
        binding.btnGallery.setOnClickListener {
            galleryCheckPermission()
        }
//        analyzeBtn.setOnClickListener(object : View.OnClickListener{
//            override fun onClick(p0: View?) {
//                imageView.setImageBitmap(drawRectangleOnFace(bitmap, result))
//            }
//        })
//        binding.btnProcess.setOnClickListener {
//
//              //  imageLoader.setImageBitmap(drawFaceRectanglesOnBitmap(bitmap))
//
//        }
        binding.imageView.setOnClickListener {
            val pictureDialog = AlertDialog.Builder(this)
            pictureDialog.setTitle("Select Action")
            val pictureDialogItem = arrayOf(
                "Select photo from Gallery",
                "Capture photo from Camera"
            )
            pictureDialog.setItems(pictureDialogItem) { _, which ->
                when (which) {
                    0 -> gallery()
                    1 -> dispatchTakePictureIntent()
                }
            }
            pictureDialog.show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }


    private fun galleryCheckPermission() {
        Dexter.withContext(this).withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    gallery()
                    // dispatchTakePictureIntent()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        this@MainActivity,
                        "You have denied the storage permission to select image",
                        Toast.LENGTH_SHORT
                    ).show()
                    showRorationalDialogForPermission()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    showRorationalDialogForPermission()

                }

            }).onSameThread().check()
    }

    private fun gallery() {
        Log.d(TAG, "gallery: ")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun cameraCheckPremission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    dispatchTakePictureIntent()
                    Log.d("sunn", "onPermissionGranted: ")
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
                    showRorationalDialogForPermission()
                }
            }).onSameThread().check()
    }

//    private fun cameraCheckPremission() {
//        Dexter.withContext(this)
//            .withPermissions(
//                android.Manifest.permission.CAMERA).withListener(object : PermissionListener {
//                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
//                dispatchTakePictureIntent()
//                }  override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
//                    Toast.makeText(
//                        applicationContext,
//                        "To use the camera function you need to grant the camera permission",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//
//                override fun onPermissionRationaleShouldBeShown(
//                    p0: PermissionRequest?,
//                    p1: PermissionToken?
//                ) {
//                    showRorationalDialogForPermission()                }
//            }).onSameThread().check()
//    }

//    private fun camera() {
//        Log.d(TAG, "camera: ")
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        startActivityForResult(intent, CAMERA_REQUEST_CODE)
//    }

    //    val REQUEST_IMAGE_CAPTURE = 1
    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakePictureIntent() {
        Log.d("albinarushiti", "dispatchTakePictureIntent: ")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
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
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: ")
        if (resultCode == RESULT_OK) {
            val uri = data?.data
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    setPic()
                }
                GALLERY_REQUEST_CODE -> {
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    binding.imageView.setImageURI(uri)
                    detectAndFrame(bitmap)
                }
            }
        }
    }

    private fun setPic() {
        Log.d("albina11", "setPic: ")
        val imageView = findViewById<ImageView>(R.id.imageView)
        // Get the dimensions of the View
        val targetW: Int = imageView.width
        val targetH: Int = imageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
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
            val bmRotated = rotateBitmap(bitmap, orientation)
            imageView.setImageBitmap(bmRotated)
            if (bmRotated != null) {
                detectAndFrame(bmRotated)
            }
//            getEmotion(bmRotated!!)
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
            Log.d(TAG, "rotateBitmap: ")
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }

    fun detectAndFrame(imageBitmap: Bitmap) {
        Log.d("snow", "detectAndFrame: ")
        val outputStream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        scope.launch {
            val result: Array<Face?> =
                faceServiceClient.detect(
                    inputStream,
                    true,  // returnFaceId
                    false,  // returnFaceLandmarks
                    arrayOf(FaceServiceClient.FaceAttributeType.Emotion) // returnFaceAttributes:
                )
            result.forEach {
                Log.d("test1", "emtions:" + it?.faceAttributes?.emotion)
            }
            withContext(Dispatchers.Main) {

                val imageView = findViewById<ImageView>(R.id.imageView)
                imageView.setImageBitmap(
                    drawFaceRectanglesOnBitmap(imageBitmap, result)
                )
            }

        }
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
        Log.d("emotion", "getEmotion: ")
        return emotion


    }

    private fun showRorationalDialogForPermission() {
        Log.d(TAG, "showRorationalDialogForPermission: ")
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

    private fun drawFaceRectanglesOnBitmap(

        originalBitmap: Bitmap, faces: Array<Face?>
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
                Log.d("albinatest1", "$: ")
            }
        }
        return bitmap
    }
}




