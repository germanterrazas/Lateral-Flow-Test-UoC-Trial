package uoc.ifm.dial.saamd.LFDApp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
    }

    lateinit var theLFDImage: Bitmap
    //lateinit var theLFDImageCopy: Bitmap
    lateinit var stream: ByteArrayOutputStream
    lateinit var theLFDImageAsByteArray: ByteArray

    private lateinit var viewModel: MainViewModel

/*    fun post1(url: String, json: String): String ?{
        Fuel.post("https://httpbin.org/post")
                .jsonBody("{ \"foo\" : \"bar\" }")
                .also { println(it) }
                .response { result -> }
    }

    fun post(urlString: String, jsonString: String): String {
        val okHttpClient = OkHttpClient()
        val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
                .method("POST", requestBody)
                .url(urlString)
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle this
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle this
            }
        })
        return "g"
    }
*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (! Python.isStarted()) { // LS ADDED
            Python.start(AndroidPlatform(this))
        }

        val buttonCamera = findViewById<Button>(R.id.button_camera)
        val buttonProcess = findViewById<Button>(R.id.button_process)

        //post("http://192.168.0.18:3000/lfdtests", "{\"date\": \"15-01-2021\",\"time\": \"12:26\",\"testid\": \"HHMM009911\",\"result\": \"Positive\"}")

        /*
        // Begin for integration to REST API
        val repository = Repository()
        val viewModelFactory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        // POST example
        val myPost = Post("14012021", "HHME002", "Negative", "1605")
        viewModel.pushPost(myPost)
        viewModel.myResponse.observe(this, androidx.lifecycle.Observer { response ->
            if(response.isSuccessful){
                Log.d("Main", response.body().toString())
                Log.d("Main", response.code().toString())
                Log.d("Main", response.message())
            } else {
                Log.d("Response", response.errorBody().toString())
                Log.d("Error Code", response.code().toString())
            }
        })*/

        // GET example
/*        viewModel.getPost()
        viewModel.myResponse.observe(this, androidx.lifecycle.Observer{ response ->
            if(response.isSuccessful){
                Log.d("Response1", response.body()?.userId.toString())
                Log.d("Response2", response.body()?.id.toString())
                Log.d("Response3", response.body()?.title!!)
                Log.d("Response4", response.body()?.body!!)
            } else {
                Log.d("Response", response.errorBody().toString())
                Log.d("Error Code", response.code().toString())
            }
        })*/
        // End for integration to REST API

        buttonProcess.setOnClickListener(View.OnClickListener {
            val toast :Toast = Toast.makeText(
                this,
                "Processing your Covid-19 LFD test",
                Toast.LENGTH_LONG
            )
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            // Begin integration with image processing ---------------------------------------------
            // Input to ZL module: theLFDImage
            // output from ZL module: successful or unsuccessful, QR code text, positive or negative

            var (processSuccess: String, testIDValue: String, resultValue: String) = pythonLFA()

            // If image processing went well then set values to report
            //val processSuccess = true               // set true if successful or false if unsuccessful
            //val testIDValue: String = "W40001231D"  // set QR code text
            //val resultValue: String = "Negative"    // set positive or negative
            // End integration with image processing module ----------------------------------------

            if(processSuccess.toLowerCase().equals("true")){
                //Log.d("Response10", "KOT test1")

                // Set values for next activity using the values above
                val startIntent = Intent(this, Dashboard::class.java)
                startIntent.putExtra("testIDValue", testIDValue)
                startIntent.putExtra("resultValue", resultValue)
                val dateValue: String = SimpleDateFormat("dd-MM-yyyy").format(Date())
                startIntent.putExtra("dateValue", dateValue)
                val timeValue: String = SimpleDateFormat("HH:mm:ss").format(Date())
                startIntent.putExtra("timeValue", timeValue)
                //Log.d("Response10", "KOT test2")

                stream = ByteArrayOutputStream()
                theLFDImage.compress(Bitmap.CompressFormat.JPEG, 15, stream)
                val byteArray: ByteArray = stream.toByteArray()

                startIntent.putExtra("lfdImage", byteArray)
                ////startIntent.putExtra("lfdImage", theLFDImage)

                startActivity(startIntent)
            } else{
                val toast: Toast = Toast.makeText(
                    this,
                    "Your Covid-19 LFD test processing failed. Please take another picture.",
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
        })

        buttonCamera.setOnClickListener {
            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                )== PackageManager.PERMISSION_GRANTED
                ){
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent,
                        CAMERA_PERMISSION_CODE
                    )
                }else{
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_CODE
                    )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent,
                    CAMERA_REQUEST_CODE
                )
            } else {
                val toast :Toast = Toast.makeText(
                    this,
                    "Permission for camera denied",
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        Log.d("onActivityResult", "1")
        if(resultCode == Activity.RESULT_OK){
            val res = ""+requestCode
//            Log.d("Request Code", res)
            val res1 = ""+ CAMERA_REQUEST_CODE
//            Log.d("CAMERA_REQUEST_CODE", res1)
            if(true) { // LS ADDED TEMPORARILY AS THE FOLLOWING LINE DOES NOT WORK
            //if(requestCode == CAMERA_REQUEST_CODE){

                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap

/**
                val values = android.content.ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "LFD Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "Picture of a COVID-19 LFD test")
                val imageUri = this.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                val inputStream: InputStream =
                    mContext.getContentResolver().openInputStream(imageUri)
                val thumbnail = BitmapFactory.decodeStream(inputStream)

                val proj =
                    arrayOf(MediaStore.Images.Media.DATA)
                val cursor: Cursor = managedQuery(imageUri, proj, null, null, null)
                val column_index: Int = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                val imageurl = cursor.getString(column_index)
                //Log.d("TEST", "file://" +imageurl)
                Log.d("TEST", imageUri.toString())

                //val image = File(imageurl)
                //val thumbnail = BitmapFactory.decodeFile(image.getAbsolutePath())
                val inputStream = URL("file://" +imageurl).openStream()
                val thumbnail = BitmapFactory.decodeStream(inputStream)
**/
                //URL(imageurl).openStream()
                  //  .use({ `is` -> val thumbnail = BitmapFactory.decodeStream(`is`) })

                //val galleryPermissions = arrayOf(
                //    Manifest.permission.READ_EXTERNAL_STORAGE,
                //    Manifest.permission.WRITE_EXTERNAL_STORAGE
                //)

                //if (EasyPermissions.hasPermissions(this, galleryPermissions)) {
                //    pickImageFromGallery()
                //} else {
                //    EasyPermissions.requestPermissions(
                //        this, "Access for storage",
                //        101, galleryPermissions
                //    )
                //}

                //val bmOptions: BitmapFactory.Options = BitmapFactory.Options()
                //var thumbnail: Bitmap? = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions)
                //bitmap = Bitmap.createScaledBitmap(bitmap!!, parent. getWidth(), parent.getHeight(), true)
                //imageView.setImageBitmap(bitmap)
                //val thumbnail = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

                //val thumbnail = MediaStore.Images.Media.getBitmap(this.contentResolver, android.net.Uri.fromFile( java.io.File(
                //    imageUri?.path
                //)))

                //val thumbnail = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                //val imgView = android.widget.ImageView()
                //imgView.setImageBitmap(thumbnail)
                //val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
                //if (thumbnail != null) {
                //    theLFDImage = thumbnail
                //}

                theLFDImage = thumbnail

                val ivImage = findViewById<AppCompatImageView>(R.id.iv_image)
                ivImage.setImageBitmap(theLFDImage)
            }
        }
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


    fun pythonLFA(): List<String> {

        stream = ByteArrayOutputStream()
        if (theLFDImage != null) {
            theLFDImage.compress(Bitmap.CompressFormat.PNG, 90, stream)
        }
        else {
            return listOf("false", "", "")
        }

        theLFDImageAsByteArray = stream.toByteArray()
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
        val pythonFile = python.getModule("pythonLFA")
        val pythonOutput = pythonFile.callAttr("runLFA", theLFDImageAsString)

        println("---PRINTING PYTHON OUTPUT---")
        println(pythonOutput.toString())

        //val processSuccess = true
        //val kotlinInputFromPython = pythonOutput.asList()
        // If image processing went well then set values to report
        //val processSuccess = kotlinInputFromPython[0].toBoolean()
        //val processSuccess = true               // set true if successful or false if unsuccessful
        //val testIDValue: String = "W40001231D"  // set QR code text
        //val parts = pythonOutput.toString().split(",:")

         var resultValue: String = ""

        // Right now there is also debug information returned, so I'm searching for a substring.
        // The debug information will be removed soon.
         if (pythonOutput.toString().contains("positive", ignoreCase = true)) {
             resultValue = "Positive"
         }
         else if (pythonOutput.toString().contains("negative", ignoreCase = true)) {
             resultValue = "Negative"
         }
         else if (pythonOutput.toString().contains("fail", ignoreCase = true)) {
             resultValue = "No LFD in image"
         }
         else {
             resultValue = "Invalid"
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
        return listOf("true","W40001231D",resultValue)

    } /***/
}