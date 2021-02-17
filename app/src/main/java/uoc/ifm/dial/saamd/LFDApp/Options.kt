package uoc.ifm.dial.saamd.LFDApp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import uoc.ifm.dial.saamd.LFDApp.util.Constants
import java.io.IOException
import java.util.concurrent.TimeUnit

class Options : AppCompatActivity() {

    companion object {
        // For NodeJS REST API POST in Azure
        private const val REST_API_URL = "http://52.149.154.59:3000/lfdtests"
        private const val REST_API_IMAGE_URL = "http://52.149.154.59:3000/upload"
        private const val CONNECT_TIMEOUT: Long = 10
        private const val WRITE_TIMEOUT: Long = 180
        private const val READ_TIMEOUT: Long = 180

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
        setContentView(R.layout.activity_options)

        lateinit var theLFDImageBA: ByteArray
        lateinit var lfdImageName: String

        val buttonSubmit = findViewById<Button>(R.id.button_submit)
        val rgLFTOptions = findViewById<RadioGroup>(R.id.lft_options)
        val rbNegativeWOS = findViewById<RadioButton>(R.id.negative_wos)
        val rbPositive = findViewById<RadioButton>(R.id.positive)
        val rbNegativeWS = findViewById<RadioButton>(R.id.negative_ws)
        val rbVoid = findViewById<RadioButton>(R.id.voids)

        rbNegativeWOS.setOnClickListener {
            buttonSubmit.setVisibility(View.VISIBLE)
        }

        rbPositive.setOnClickListener {
            buttonSubmit.setVisibility(View.VISIBLE)
        }

        rbNegativeWS.setOnClickListener {
            buttonSubmit.setVisibility(View.VISIBLE)
        }

        rbVoid.setOnClickListener {
            buttonSubmit.setVisibility(View.VISIBLE)
        }

        val dateValue = intent.getStringExtra(Constants.INTENT_DATE)
        val timeValue = intent.getStringExtra(Constants.INTENT_TIME)
        theLFDImageBA = intent.getByteArrayExtra(Constants.INTENT_LFD_IMAGE)!!
        lfdImageName = intent.getStringExtra(Constants.INTENT_LFD_IMAGE_NAME)+".jpeg"

        buttonSubmit.setOnClickListener {
            val optionChecked = rgLFTOptions.checkedRadioButtonId
            val rbChecked = findViewById<RadioButton>(optionChecked)
            Log.d("value", rbChecked.text.toString())

            val jsonContent :String = "{\"date\": \"${dateValue}\"" +
                    ",\"deviceid\": \"UoC-Innova\"" +
                    ",\"result\": \"${rbChecked.text.toString()}\"" +
                    ",\"time\": \"${timeValue}\"}"
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
        val aCall : Call = okHttpClient.newCall(request)
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
        val aCall : Call = okHttpClient.newCall(request)
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
                val toast : Toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, xOffSet, yOffSet)
                toast.show()

            })
        }
    }

}