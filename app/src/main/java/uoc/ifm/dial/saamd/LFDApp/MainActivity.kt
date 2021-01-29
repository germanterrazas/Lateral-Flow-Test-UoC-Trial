package uoc.ifm.dial.saamd.LFDApp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_DATE
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_LFD_IMAGE
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_LFD_IMAGE_NAME
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_RESULT
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_TEST_ID
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_TIME
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.DateFormat.getDateInstance
import java.text.DateFormat.getTimeInstance
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val JPEG_COMPRESSION_FOR_EXTRA = 50
        private const val JPEG_COMPRESSION_FOR_ANALYSIS = 20

        // Used to message the user through showToast()
        private const val PROCESS_BUTTON_MESSAGE = "Processing your Covid-19 LFD test"
        private const val LFD_PROCESSING_FAILED = "Picture unavailable -- Please take another one."
        private const val CAMERA_PERMISSION_DENIED = "Permission for camera denied"

        // Used in pythonLFA()
        private const val LFD_POSITIVE = "Positive"
        private const val LFD_NEGATIVE = "Negative"
        private const val LFD_INVALID = "Invalid"
        private const val NO_LFD = "No LFD Image"

        // Used in dispatchTakePictureIntent()
        private const val APP_URI_NAME = "uoc.ifm.dial.saamd.LFDApp"
        private const val PHOTO_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"
    }

    private lateinit var theLFDImage: Bitmap
    private lateinit var currentPhotoPath: String
    private lateinit var photoTimestamp: Date
    private lateinit var photoURI: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (! Python.isStarted()) { // LS ADDED
            Python.start(AndroidPlatform(this))
        }

        val buttonCamera = findViewById<Button>(R.id.button_camera)
        val buttonProcess = findViewById<Button>(R.id.button_process)

        // Action triggered by PROCESS button
        // Processes an LFD image and launches the dashboard activity by passing the results
        buttonProcess.setOnClickListener(View.OnClickListener {
            showToast(PROCESS_BUTTON_MESSAGE, Toast.LENGTH_LONG, Gravity.CENTER, 0, 0)
            // Begin integration with image processing ---------------------------------------------
            // Input to ZL module: theLFDImage
            // output from ZL module: successful or unsuccessful, QR code text, positive or negative
            val (processSuccess: String, testIDValue: String, resultValue: String) = pythonLFA()
//            val processSuccess = "true"
//            val testIDValue = "W40001231D"
//            val resultValue = LFD_POSITIVE
            // End integration with image processing module ----------------------------------------

            if(processSuccess.toLowerCase(Locale.getDefault()).equals("true")){

                // Prepare activity parameters using the values resulting from image processing
                val dateValue: String = getDateInstance().format(photoTimestamp)
                val timeValue: String = getTimeInstance().format(photoTimestamp)
                val stream = ByteArrayOutputStream()
                theLFDImage.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_FOR_EXTRA, stream)

                // Set activity with parameters
                val startIntent = Intent(this, Dashboard::class.java)
                startIntent.putExtra(INTENT_TEST_ID, testIDValue)
                startIntent.putExtra(INTENT_RESULT, resultValue)
                startIntent.putExtra(INTENT_DATE, dateValue)
                startIntent.putExtra(INTENT_TIME, timeValue)
                startIntent.putExtra(INTENT_LFD_IMAGE_NAME, SimpleDateFormat(PHOTO_TIMESTAMP_FORMAT, Locale.UK).format(photoTimestamp))
                startIntent.putExtra(INTENT_LFD_IMAGE, stream.toByteArray())
                startActivity(startIntent)
            } else{
                showToast(LFD_PROCESSING_FAILED, Toast.LENGTH_LONG, Gravity.CENTER, 0, 0)
            }
        })

        // Action triggered by CAMERA button
        // By pressing this button, the user takes a picture of an LFD with the device camera
        buttonCamera.setOnClickListener {
            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                ){
                    dispatchTakePictureIntent()
                }else{
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_CODE
                    )
            }
        }
    }

    // The device requests the user permission to use the camera
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                dispatchTakePictureIntent()
            } else {
                showToast(CAMERA_PERMISSION_DENIED, Toast.LENGTH_LONG, Gravity.CENTER, 0, 0)
            }
        }
    }

    // Creates an image file in temporary folder to store the LFD picture
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        photoTimestamp = Date()
        val photoTimestampStr = SimpleDateFormat(PHOTO_TIMESTAMP_FORMAT, Locale.UK).format(photoTimestamp)
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${ photoTimestampStr}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // Prepares the device to take a picture with the built-in camera
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(
                            this,
                            APP_URI_NAME,
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    // Accesses the picture stored in Android/data/uoc.ifm.dial.saamd.LFDApp/files/Pictures
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE){
                val inputStream: InputStream? = getContentResolver().openInputStream(photoURI)
                theLFDImage = BitmapFactory.decodeStream(inputStream)
                val ivImage = findViewById<AppCompatImageView>(R.id.iv_image)
                ivImage.setImageBitmap(createThumbnailFor(theLFDImage, ivImage.width, ivImage.height))
        }
    }

    // Creates and returns a thumbnail of a given photo with smaller dimension and 4 bytes per pixel
    // aPhoto: the original photo to make a thumbnail from
    // width: the width of the desired thumbnail
    // height: the height of the desired thumbnail
    private fun createThumbnailFor(aPhoto: Bitmap, width:Int, height: Int): Bitmap {
        val ratio: Float = Math.min(
                width.toFloat() / aPhoto.width,
                height.toFloat() / aPhoto.height)
        val newWidth = (ratio * aPhoto.width).roundToInt()
        val newHeight = (ratio * aPhoto.height).roundToInt()
        Log.d("width", width.toString())
        Log.d("height", height.toString())
        Log.d("nWidth", newWidth.toString())
        Log.d("nHeight", newHeight.toString())
        val thumbnail = Bitmap.createScaledBitmap(aPhoto, newWidth, newHeight, false)
        val canvas: Canvas = Canvas(Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888));
        canvas.drawBitmap(thumbnail, 0f, 0f, null);
        return thumbnail
    }

    // Sets and shows a Toast message to the user
    // message: the string to show
    // duration: the length of time message is displayed to the user
    // gravity: the container position relative to horizontal and vertical axis
    // xOffset: the offset on container x-axis
    // yOffset: the offset on container y-axis
    private fun showToast(message: String, duration: Int, gravity: Int, xOffset: Int, yOffset: Int){
        val toast :Toast = Toast.makeText(
                this,
                message,
                duration
        )
        toast.setGravity(gravity, xOffset, yOffset)
        toast.show()
    }

    /*** TEMPORARY CODE FOR TESTING
     * For testing, you can pass one of the images stored in the 'assets' subfolder instead
     * of sending an image that was taken with your phone. See below.
     *
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("onActivityResult", "1")
        if(resultCode == Activity.RESULT_OK){
            val res = ""+requestCode
            Log.d("Request Code", res)
            val res1 = ""+ CAMERA_REQUEST_CODE
            Log.d("CAMERA_REQUEST_CODE", res1)
            //if(requestCode == CAMERA_REQUEST_CODE){ //LS COMMENTED OUT
            if(true){
                Log.d("onActivityResult", "3")

                val stream2 = ByteArrayOutputStream()
                val bMap = android.graphics.BitmapFactory.decodeStream(getAssets().open("noqr_negative.jpg"))
                theLFDImage = bMap
                val ivImage = findViewById<AppCompatImageView>(R.id.iv_image)
                ivImage.setImageBitmap(theLFDImage)

            }
        }
    }
     ***/

    private fun pythonLFA(): List<String> {
        val stream = ByteArrayOutputStream()
        if (theLFDImage != null) {
//            theLFDImage.compress(Bitmap.CompressFormat.PNG, 90, stream)
            // @LS if JPEG_COMPRESSION_FOR_ANALYSIS is set to 50 or over, it takes too much time and crashes
            theLFDImage.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_FOR_ANALYSIS, stream)
        }
        else {
            return listOf("false", "", "")
        }
        val theLFDImageAsByteArray: ByteArray = stream.toByteArray()
        val theLFDImageAsString = android.util.Base64.encodeToString(theLFDImageAsByteArray, android.util.Base64.DEFAULT)

        /***
        val stream2 = ByteArrayOutputStream()
        //val bMap = BitmapFactory.decodeFile("src/main/assets/image_qr_negative.png")
        val bMap = BitmapFactory.decodeStream(getAssets().open("image_qr_negative.png"))
        bMap.compress(Bitmap.CompressFormat.PNG, 90, stream2)
        val theBMAPAsByteArray = stream2.toByteArray()
        val theBMAPAsString = android.util.Base64.encodeToString(theBMAPAsByteArray, android.util.Base64.DEFAULT)
         ***/
        val python = Python.getInstance()
        Log.d("pythonLFA1", "263")
        val pythonFile = python.getModule("pythonLFA") // <-- @LS this generates W/System: A resource failed to call close.
        Log.d("pythonLFA2", "265")
        val pythonOutput = pythonFile.callAttr("runLFA", theLFDImageAsString)
        Log.d("PRINTING PYTHON OUTPUT", pythonOutput.toString())
        Log.d("pythonLFA3", "268")

        //val processSuccess = true
        //val kotlinInputFromPython = pythonOutput.asList()
        // If image processing went well then set values to report
        //val processSuccess = kotlinInputFromPython[0].toBoolean()
        //val processSuccess = true               // set true if successful or false if unsuccessful
        //val testIDValue: String = "W40001231D"  // set QR code text
        //val parts = pythonOutput.toString().split(",:")

         var resultValue = ""

        // Right now there is also debug information returned, so I'm searching for a substring.
        // The debug information will be removed soon.
         if (pythonOutput.toString().contains("positive", ignoreCase = true)) {
             resultValue = LFD_POSITIVE
         }
         else if (pythonOutput.toString().contains("negative", ignoreCase = true)) {
             resultValue = LFD_NEGATIVE
         }
         else if (pythonOutput.toString().contains("fail", ignoreCase = true)) {
             resultValue = NO_LFD
         }
         else {
             resultValue = LFD_INVALID
         }

         //var resultValue: String = "Fail"
        //if (parts.size>1)
        //    resultValue = parts[0]    // set positive or negative
        //val resultValue: String = "Negative"    // set positive or negative
        //val testIDValue: String = kotlinInputFromPython[1].toString()  // set QR code text
        //val resultValue: String = kotlinInputFromPython[2].toString()    // set positive or negative
        // End integration with image processing module ----------------------------------------

        //return listOf("true","W40001231D",resultValue)

        // "true" parameter could probably be removed. The second parameter is the QR code.

        // TODO If resultValue = NO_LFD then this should return false othewise returns true?
        return listOf("true","W40001231D",resultValue)
    }
}