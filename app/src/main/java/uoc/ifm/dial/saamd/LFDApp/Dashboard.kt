package uoc.ifm.dial.saamd.LFDApp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uoc.ifm.dial.saamd.LFDApp.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


class Dashboard : AppCompatActivity() {

    companion object {
        // URL for NodeJS REST API POST in Azure
        private const val REST_API_URL = "http://52.149.154.59:3000/lfdtests"
        private const val REST_API_IMAGE_URL = "http://52.149.154.59:3000/upload"
        //private const val REST_API_URL = "http://192.168.0.18:3000/lfdtests"
    }

    var restAPISuccess: Int = -1
    lateinit var dateValue: TextView
    lateinit var testIDValue: TextView
    lateinit var resultValue: TextView
    lateinit var timeValue: TextView
//    lateinit var theLFDImage: Bitmap
    lateinit var theLFDImageBA: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Response10", "KOT test7")
        setContentView(R.layout.activity_dashboard)
        Log.d("Response10", "KOT test7.5")

        // Sets the values coming from the main activity
        if (intent.hasExtra("dateValue")
            && intent.hasExtra("testIDValue")
            && intent.hasExtra("resultValue")
            && intent.hasExtra("timeValue")){
            dateValue = findViewById<TextView>(R.id.date_value)
            dateValue.setText(intent.getStringExtra("dateValue"))
            testIDValue = findViewById<TextView>(R.id.test_id_value)
            testIDValue.setText(intent.getStringExtra("testIDValue"))
            resultValue = findViewById<TextView>(R.id.result_value)
            resultValue.setText(intent.getStringExtra("resultValue"))
            timeValue = findViewById<TextView>(R.id.time_value)
            timeValue.setText(intent.getStringExtra("timeValue"))
            Log.d("Response10", "KOT test7")

            theLFDImageBA = intent.getByteArrayExtra("lfdImage")!!
            Log.d("Response10", "KOT test8")

            //for testing if the image comes from prev activity
//            theLFDImage = BitmapFactory.decodeByteArray(theLFDImageBA, 0, theLFDImageBA.size)
//            val ivImage = findViewById<AppCompatImageView>(R.id.imageView3)
//            ivImage.setImageBitmap(theLFDImage)
        }

        // Action triggered by SHARE button
        // Launches an external activity for sharing the information displayed on screen
        val buttonShare = findViewById<Button>(R.id.button_share)
        buttonShare.setOnClickListener{
            val startIntent = Intent(Intent.ACTION_SEND)
            startIntent.setType("text/plain")
            val subject: String = "Covid-19 test results"
            val body: String = "Date:" + dateValue.text.toString() +
                    "\nDevice ID: " + testIDValue.text.toString() +
                    "\nResult: " +  resultValue.text.toString() +
                    "\nTime: " + timeValue.text.toString()
            startIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            startIntent.putExtra(Intent.EXTRA_TEXT, body)
            startActivity(Intent.createChooser(startIntent, "Share using"))
        }

        // Action triggered by UPLOAD button
        // Posts the information displayed on screen and the image to a REST API
        val buttonUpload = findViewById<Button>(R.id.button_upload)
        buttonUpload.setOnClickListener {
            val jsonContent :String = "{\"date\": \""+ intent.getStringExtra("dateValue") +
                                "\",\"deviceid\": \""+ intent.getStringExtra("testIDValue") +
                                "\",\"result\": \"" + intent.getStringExtra("resultValue") +
                                "\",\"time\": \""+ intent.getStringExtra("timeValue") +"\"}"
            postRestApiImage(REST_API_IMAGE_URL, theLFDImageBA, testIDValue.text.toString()+".PNG")
            postRestApiResults(REST_API_URL, jsonContent)
        }

    }

    fun postRestApiImage(urlString: String, aBitmapBA: ByteArray, bitmapName: String){
        restAPISuccess = -1
//        val client = OkHttpClient.Builder()
//                .connectTimeout(10, TimeUnit.SECONDS)
//                .writeTimeout(180, TimeUnit.SECONDS)
//                .readTimeout(180, TimeUnit.SECONDS)
//                .build()
        val okHttpClient = OkHttpClient()
        //val requestBody: RequestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", bitmapName,  aBitmapBA.toRequestBody("image/png".toMediaTypeOrNull()))
                .build()
        val request = Request.Builder()
                .method("POST", requestBody)
                .url(urlString)
                .build()
        val aCall :Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                restAPISuccess = 0
                Log.d("Exception postRestApi", e.toString())
                Log.d("Failed", ""+restAPISuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, "Image upload failed", 0, 100)
            }
            override fun onResponse(call: Call, response: Response) {
                restAPISuccess = response.code
                Log.d("Response body", response.body.toString())
                Log.d("Success", ""+restAPISuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, "Image uploaded", 0, 100)
            }
        })
    }


    // Called from UPLOAD button listener line 75
    fun postRestApiResults(urlString: String, jsonString: String){
        restAPISuccess = -1
        val okHttpClient = OkHttpClient()
        val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
                .method("POST", requestBody)
                .url(urlString)
                .build()
        val aCall :Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                restAPISuccess = 0
                Log.d("Exception postRestApi", e.toString())
                Log.d("Failed", ""+restAPISuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, "Results upload failed", 0, 0)
            }
            override fun onResponse(call: Call, response: Response) {
                restAPISuccess = response.code
                Log.d("Response body", response.body.toString())
                Log.d("Success", ""+restAPISuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, "Results uploaded", 0, 0)
            }
        })
    }

    fun backgroundThreadShortToast(context: Context?,
                                   msg: String?, xOffSet: Int, yOffSet: Int) {
        if (context != null && msg != null) {
            Handler(Looper.getMainLooper()).post(Runnable {
                val toast :Toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, xOffSet, yOffSet)
                toast.show()

            })
        }
    }
}