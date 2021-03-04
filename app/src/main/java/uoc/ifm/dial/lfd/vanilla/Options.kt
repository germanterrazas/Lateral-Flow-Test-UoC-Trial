package uoc.ifm.dial.lfd.vanilla

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import uoc.ifm.dial.lfd.vanilla.util.Constants
import uoc.ifm.dial.lfd.vanilla.util.SharedPreferences
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
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
        disableButton(buttonSubmit)
        buttonClose = findViewById(R.id.button_close)
        disableButton(buttonClose)
        val rgLFTOptions = findViewById<RadioGroup>(R.id.lft_options)
        val rbNegativeWOS = findViewById<RadioButton>(R.id.negative_wos)
        val rbPositive = findViewById<RadioButton>(R.id.positive)
        val rbNegativeWS = findViewById<RadioButton>(R.id.negative_ws)
        val rbVoid = findViewById<RadioButton>(R.id.voids)
        photoSentTextView = findViewById(R.id.textView_responseSent)
        responseSentTextView = findViewById(R.id.textView_photoSent)

        rbNegativeWOS.setOnClickListener {
            enableButton(buttonSubmit)
        }
        rbPositive.setOnClickListener {
            enableButton(buttonSubmit)
        }
        rbNegativeWS.setOnClickListener {
            enableButton(buttonSubmit)
        }
        rbVoid.setOnClickListener {
            enableButton(buttonSubmit)
        }
        val dateValue = intent.getStringExtra(Constants.INTENT_DATE)
        val timeValue = intent.getStringExtra(Constants.INTENT_TIME)
        val theLFDImageURI = intent.getStringExtra(Constants.INTENT_LFD_IMAGE)
        val photoURI = Uri.parse(theLFDImageURI)
        val lfdImageName: String = intent.getStringExtra(Constants.INTENT_LFD_IMAGE_NAME)+Constants.IMAGE_FILE_EXTENSION
        val stream = ByteArrayOutputStream()
        var jsonContent = ""

        buttonSubmit.setOnClickListener {
            if (postRestApiResultsSuccess != Constants.SUCCESS_API_POST_CODE && postRestApiImageSuccess != Constants.SUCCESS_API_POST_CODE){
                val optionChecked = rgLFTOptions.checkedRadioButtonId
                val rbChecked = findViewById<RadioButton>(optionChecked)
                rbNegativeWOS.isEnabled = false
                rbPositive.isEnabled = false
                rbVoid.isEnabled = false
                rbNegativeWS.isEnabled = false
                val sharedPreference = SharedPreferences(this)
                jsonContent = "{\"date\": \"${dateValue}\"" +
                        ",\"deviceid\": \"${sharedPreference.getValueString(Constants.SHARED_PREF_DEVICE_ID)}\"" +
                        ",\"result\": \"${rbChecked.text}\"" +
                        ",\"time\": \"${timeValue}\"" +
                        ",\"filename\": \"${lfdImageName}\"" +
                        "}"

                // Access the image in folder content://uoc.ifm.dial.saamd.LFDApp/my_images/
                val inputStream: InputStream? = contentResolver.openInputStream(photoURI)
                val theLFDImage = BitmapFactory.decodeStream(inputStream)
                theLFDImage.compress(Bitmap.CompressFormat.JPEG, Constants.JPEG_COMPRESSION_FOR_EXTRA, stream)
                postRestApiImage(stream.toByteArray(), lfdImageName)
                postRestApiResults(jsonContent)
                photoSentTextView.text = Constants.IMAGE_UPLOAD_WAIT
                photoSentTextView.visibility = View.VISIBLE
                responseSentTextView.text = Constants.CHOICE_UPLOAD_WAIT
                responseSentTextView.visibility = View.VISIBLE
                disableButton(buttonSubmit)
            }  else {
                if (postRestApiResultsSuccess != Constants.SUCCESS_API_POST_CODE){
                    postRestApiResults(jsonContent)
                    responseSentTextView.text = Constants.CHOICE_UPLOAD_WAIT
                    disableButton(buttonSubmit)
                }
                if(postRestApiImageSuccess != Constants.SUCCESS_API_POST_CODE){
                    postRestApiImage(stream.toByteArray(), lfdImageName)
                    photoSentTextView.text = Constants.IMAGE_UPLOAD_WAIT
                    disableButton(buttonSubmit)
                }
            }

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
            .addFormDataPart("file", bitmapName, aBitmapBA.toRequestBody(Constants.REQUEST_BODY_IMAGE.toMediaTypeOrNull()))
            .build()
        val request = Request.Builder()
            .method(Constants.API_POST_METHOD, requestBody)
            .url(Constants.REST_API_IMAGE_URL)
            .build()
        val aCall : Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postRestApiImageSuccess = 0
                backgroundThreadDataSent(applicationContext,1, postRestApiImageSuccess)
            }
            override fun onResponse(call: Call, response: Response) {
                postRestApiImageSuccess = response.code
                backgroundThreadDataSent(applicationContext,1, postRestApiImageSuccess)
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
            .method(Constants.API_POST_METHOD, requestBody)
            .url(Constants.REST_API_URL)
            .build()
        val aCall : Call = okHttpClient.newCall(request)
        aCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postRestApiResultsSuccess = 0
                backgroundThreadDataSent(applicationContext,2, postRestApiResultsSuccess)
            }
            override fun onResponse(call: Call, response: Response) {
                postRestApiResultsSuccess = response.code
                backgroundThreadDataSent(applicationContext,2, postRestApiResultsSuccess)
            }
        })
    }

    private fun backgroundThreadDataSent(context: Context?,from: Int, resultSuccess: Int) {
        if (context != null) {
            Handler(Looper.getMainLooper()).post(Runnable {
                if(from == 1) {
                    if(resultSuccess == Constants.SUCCESS_API_POST_CODE){
                        photoSentTextView.text = Constants.IMAGE_UPLOAD_SUCCESSFUL
                        photoSent = true
                    } else {
                        photoSentTextView.text = Constants.IMAGE_UPLOAD_FAILED
                    }
                }
                if(from == 2){
                    if(resultSuccess == Constants.SUCCESS_API_POST_CODE){
                        responseSentTextView.text = Constants.CHOICE_UPLOAD_SUCCESSFUL
                        responseSent = true
                    } else {
                        responseSentTextView.text = Constants.CHOICE_UPLOAD_FAILED
                    }
                }
                if(responseSent && photoSent){
                    enableButton(buttonClose)
                } else {
                    if((postRestApiResultsSuccess > -1) && (postRestApiImageSuccess > -1)){
                        enableButton(buttonSubmit)
                    }
                }
            })
        }
    }

    private fun enableButton(aButton: Button){
        aButton.isEnabled = true
        aButton.setTextColor(ContextCompat.getColor(aButton.context, R.color.white))
        aButton.setBackgroundColor(ContextCompat.getColor(aButton.context, R.color.enabled_button))
    }

    private fun disableButton(aButton: Button){
        aButton.isEnabled = false
        aButton.setTextColor(ContextCompat.getColor(aButton.context, R.color.white))
        aButton.setBackgroundColor(ContextCompat.getColor(aButton.context, R.color.disabled_button))
    }

}