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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_DATE
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_LFD_IMAGE
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_LFD_IMAGE_NAME
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_RESULT
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_TEST_ID
import uoc.ifm.dial.saamd.LFDApp.util.Constants.Companion.INTENT_TIME
import java.io.IOException
import java.util.concurrent.TimeUnit


class Dashboard : AppCompatActivity() {

    companion object {
        // For NodeJS REST API POST in Azure
        private const val REST_API_URL = "http://52.149.154.59:3000/lfdtests"
        private const val REST_API_IMAGE_URL = "http://52.149.154.59:3000/upload"
        private const val CONNECT_TIMEOUT :Long = 10
        private const val WRITE_TIMEOUT :Long = 180
        private const val READ_TIMEOUT :Long = 180

        // For use when SHARE button is pressed
        private const val SHARE_SUBJECT = "Covid-19 test results"
        private const val TEXT_PLAIN_TYPE = "text/plain"
        private const val CHOOSER_TITLE = "Share using"

        // For use when UPLOAD button is pressed
        private const val REQUEST_BODY_IMAGE = "image/png"
        private const val REQUEST_BODY_JSON = "application/json; charset=utf-8"
        private const val IMAGE_UPLOAD_FAILED = "LFD photo upload failed"
        private const val IMAGE_UPLOAD_SUCCESSFUL = "LFD photo sent"
        private const val RESULTS_UPLOAD_FAILED = "Results upload failed"
        private const val RESULTS_UPLOAD_SUCCESSFUL = "Results sent"
    }

    private var postRestApiResultsSuccess: Int = -1
    private var postRestApiImageSuccess: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        lateinit var dateValue: TextView
        lateinit var testIDValue: TextView
        lateinit var resultValue: TextView
        lateinit var timeValue: TextView
        lateinit var theLFDImageBA: ByteArray
        lateinit var lfdImageName: String

        // Sets the values coming from the main activity
        if (intent.hasExtra(INTENT_DATE)
                && intent.hasExtra(INTENT_TEST_ID)
                && intent.hasExtra(INTENT_RESULT)
                && intent.hasExtra(INTENT_TIME)
                && intent.hasExtra(INTENT_LFD_IMAGE_NAME)){
            dateValue = findViewById(R.id.date_value)
            testIDValue = findViewById(R.id.test_id_value)
            resultValue = findViewById(R.id.result_value)
            timeValue = findViewById(R.id.time_value)

            dateValue.setText(intent.getStringExtra(INTENT_DATE))
            testIDValue.setText(intent.getStringExtra(INTENT_TEST_ID))
            resultValue.setText(intent.getStringExtra(INTENT_RESULT))
            timeValue.setText(intent.getStringExtra(INTENT_TIME))
            theLFDImageBA = intent.getByteArrayExtra(INTENT_LFD_IMAGE)!!
            lfdImageName = intent.getStringExtra(INTENT_LFD_IMAGE_NAME)+".jpeg"

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
            startIntent.setType(TEXT_PLAIN_TYPE)
            val body: String = "Date: ${dateValue.text}" +
                               "\nDevice ID: ${testIDValue.text}" +
                               "\nResult: ${resultValue.text}" +
                               "\nTime: ${timeValue.text}"
            startIntent.putExtra(Intent.EXTRA_SUBJECT, SHARE_SUBJECT)
            startIntent.putExtra(Intent.EXTRA_TEXT, body)
            startActivity(Intent.createChooser(startIntent, CHOOSER_TITLE))
        }

        // Action triggered by UPLOAD button
        // Posts the information displayed on screen and the image to a REST API
        val buttonUpload = findViewById<Button>(R.id.button_upload)
        buttonUpload.setOnClickListener {
            val jsonContent :String = "{\"date\": \"${dateValue.text}\"" +
                                      ",\"deviceid\": \"${testIDValue.text}\"" +
                                      ",\"result\": \"${resultValue.text}\"" +
                                      ",\"time\": \"${timeValue.text}\"}"
            postRestApiImage(REST_API_IMAGE_URL, theLFDImageBA, lfdImageName)
            postRestApiResults(REST_API_URL, jsonContent)
        }

    }

    // Sends the photo of the LFD device to the REST API
    private fun postRestApiImage(urlString: String, aBitmapBA: ByteArray, bitmapName: String){
        postRestApiImageSuccess = -1
        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build()
        //val okHttpClient = OkHttpClient()
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", bitmapName,  aBitmapBA.toRequestBody(REQUEST_BODY_IMAGE.toMediaTypeOrNull()))
                .build()
        val request = Request.Builder()
                .method("POST", requestBody)
                .url(urlString)
                .build()
        val aCall :Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postRestApiImageSuccess = 0
                Log.d("Exception postRestApi", e.toString())
                Log.d("Failed", ""+postRestApiImageSuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, IMAGE_UPLOAD_FAILED, 0, 100)
            }
            override fun onResponse(call: Call, response: Response) {
                postRestApiImageSuccess = response.code
                Log.d("Response body", response.body.toString())
                Log.d("Success", ""+postRestApiImageSuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, IMAGE_UPLOAD_SUCCESSFUL, 0, 100)
            }
        })
    }

    // Sends the results in json format to the REST API
    private fun postRestApiResults(urlString: String, jsonString: String){
        postRestApiResultsSuccess = -1
        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build()
        //val okHttpClient = OkHttpClient()
        val requestBody = jsonString.toRequestBody(REQUEST_BODY_JSON.toMediaTypeOrNull())
        val request = Request.Builder()
                .method("POST", requestBody)
                .url(urlString)
                .build()
        val aCall :Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postRestApiResultsSuccess = 0
                Log.d("Exception postRestApi", e.toString())
                Log.d("Failed", ""+postRestApiResultsSuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, RESULTS_UPLOAD_FAILED, 0, 0)
            }
            override fun onResponse(call: Call, response: Response) {
                postRestApiResultsSuccess = response.code
                Log.d("Response body", response.body.toString())
                Log.d("Success", ""+postRestApiResultsSuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, RESULTS_UPLOAD_SUCCESSFUL, 0, 0)
            }
        })
    }

    // Sets and shows a Toast message to the user
    // message: the string to show
    // xOffset: the offset on container x-axis
    // yOffset: the offset on container y-axis
    private fun backgroundThreadShortToast(context: Context?,
                                   message: String?, xOffSet: Int, yOffSet: Int) {
        if (context != null && message != null) {
            Handler(Looper.getMainLooper()).post(Runnable {
                val toast :Toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, xOffSet, yOffSet)
                toast.show()

            })
        }
    }
}