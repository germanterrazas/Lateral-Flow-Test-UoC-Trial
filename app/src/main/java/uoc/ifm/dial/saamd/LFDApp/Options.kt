package uoc.ifm.dial.saamd.LFDApp

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import uoc.ifm.dial.saamd.LFDApp.util.Constants
import uoc.ifm.dial.saamd.LFDApp.util.SharedPreferences
import java.io.IOException
import java.util.concurrent.TimeUnit

class Options : AppCompatActivity() {

    private var postRestApiResultsSuccess: Int = -1
    private var postRestApiImageSuccess: Int = -1
    private lateinit var photoSentTextView: TextView
    private lateinit var responseSentTextView: TextView
    private lateinit var buttonSubmit: Button
    private lateinit var buttonClose: Button
    private var photoSent: Boolean = false
    private var responseSent: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        buttonSubmit = findViewById(R.id.button_submit)
        buttonClose = findViewById(R.id.button_close)
        val rgLFTOptions = findViewById<RadioGroup>(R.id.lft_options)
        val rbNegativeWOS = findViewById<RadioButton>(R.id.negative_wos)
        val rbPositive = findViewById<RadioButton>(R.id.positive)
        val rbNegativeWS = findViewById<RadioButton>(R.id.negative_ws)
        val rbVoid = findViewById<RadioButton>(R.id.voids)
        photoSentTextView = findViewById(R.id.textView_photoSent)
        responseSentTextView = findViewById(R.id.textView_responseSent)

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
        val theLFDImageBA: ByteArray = intent.getByteArrayExtra(Constants.INTENT_LFD_IMAGE)!!
        val lfdImageName: String = intent.getStringExtra(Constants.INTENT_LFD_IMAGE_NAME)+".jpeg"

        buttonSubmit.setOnClickListener {
            val optionChecked = rgLFTOptions.checkedRadioButtonId
            val rbChecked = findViewById<RadioButton>(optionChecked)
            val sharedPreference = SharedPreferences(this)
            val jsonContent :String = "{\"date\": \"${dateValue}\"" +
                    ",\"deviceid\": \"${sharedPreference.getValueString(Constants.SHARED_PREF_DEVICE_ID)}\"" +
                    ",\"result\": \"${rbChecked.text}\"" +
                    ",\"time\": \"${timeValue}\"}"
            postRestApiImage(theLFDImageBA, lfdImageName)
            postRestApiResults(jsonContent)
            photoSentTextView.text = Constants.IMAGE_UPLOAD_WAIT
            photoSentTextView.setVisibility(View.VISIBLE)
            responseSentTextView.text = Constants.CHOICE_UPLOAD_WAIT
            responseSentTextView.setVisibility(View.VISIBLE)
        }

        buttonClose.setOnClickListener {
            finishAffinity()
        }

    }

    // Sends the photo of the LFD device to the REST API
    private fun postRestApiImage(aBitmapBA: ByteArray, bitmapName: String){
        postRestApiImageSuccess = -1
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
        //val okHttpClient = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", bitmapName,  aBitmapBA.toRequestBody(Constants.REQUEST_BODY_IMAGE.toMediaTypeOrNull()))
            .build()
        val request = Request.Builder()
            .method("POST", requestBody)
            .url(Constants.REST_API_IMAGE_URL)
            .build()
        val aCall : Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postRestApiImageSuccess = 0
//                Log.d("Exception postRestApi", e.toString())
//                Log.d("Failed", ""+postRestApiImageSuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, Constants.IMAGE_UPLOAD_FAILED, 0, 100)
            }
            override fun onResponse(call: Call, response: Response) {
                postRestApiImageSuccess = response.code
//                Log.d("Response body", response.body.toString())
//                Log.d("Success", ""+postRestApiImageSuccess)
                val context = applicationContext
//                backgroundThreadShortToast(context, IMAGE_UPLOAD_SUCCESSFUL, 0, 100)
                backgroundThreadDataSent(context,1)
            }
        })
    }

    // Sends the results in json format to the REST API
    private fun postRestApiResults(jsonString: String){
        postRestApiResultsSuccess = -1
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
        //val okHttpClient = OkHttpClient()
        val requestBody = jsonString.toRequestBody(Constants.REQUEST_BODY_JSON.toMediaTypeOrNull())
        val request = Request.Builder()
            .method("POST", requestBody)
            .url(Constants.REST_API_URL)
            .build()
        val aCall : Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postRestApiResultsSuccess = 0
//                Log.d("Exception postRestApi", e.toString())
//                Log.d("Failed", ""+postRestApiResultsSuccess)
                val context = applicationContext
                backgroundThreadShortToast(context, Constants.RESULTS_UPLOAD_FAILED, 0, 0)
            }
            override fun onResponse(call: Call, response: Response) {
                postRestApiResultsSuccess = response.code
//                Log.d("Response body", response.body.toString())
//                Log.d("Success", ""+postRestApiResultsSuccess)
                val context = applicationContext
//                backgroundThreadShortToast(context, RESULTS_UPLOAD_SUCCESSFUL, 0, 0)
                backgroundThreadDataSent(context,2)
            }
        })
    }

    private fun backgroundThreadDataSent(context: Context?,from: Int) {
        if (context != null) {
            Handler(Looper.getMainLooper()).post(Runnable {
                if(from==1) {
                    photoSentTextView.text = Constants.IMAGE_UPLOAD_SUCCESSFUL
                    photoSent = true
                }
                if(from==2){
                    responseSentTextView.text = Constants.CHOICE_UPLOAD_SUCCESSFUL
                    responseSent = true
                }
                if(responseSent && photoSent){
                    buttonClose.setVisibility(View.VISIBLE)
                    buttonSubmit.setVisibility(View.INVISIBLE)
                }
            })
        }
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