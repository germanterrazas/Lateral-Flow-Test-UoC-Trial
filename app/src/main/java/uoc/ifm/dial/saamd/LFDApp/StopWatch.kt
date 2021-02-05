package uoc.ifm.dial.saamd.LFDApp

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_stop_watch.*
import uoc.ifm.dial.saamd.LFDApp.util.Constants
import java.text.SimpleDateFormat
import java.util.*


class StopWatch : AppCompatActivity(){

    private var timeStarted: Boolean = false;
    private var pauseAt: Long = 0
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_watch)

        val buttonTimer = findViewById<Button>(R.id.button_start)
        val buttonNext = findViewById<Button>(R.id.button_stop)
        val chronometer = findViewById<Chronometer>(R.id.stopwatch)
        var chronometerStart = Date()
        var exitActivity: Date = Date()
        var stopwatchValue: String = "no_value"

        buttonNext.setVisibility(View.GONE);
        val alert: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(applicationContext, alert)

        buttonTimer.setOnClickListener {
            if(timeStarted){
                mediaPlayer.pause()
                buttonNext.setVisibility(View.GONE);
                stopwatch.stop()
                timeStarted = false
//                val totalTime = SystemClock.elapsedRealtime() - stopwatch.base
                buttonTimer.setText("START")
            } else {
                buttonNext.setVisibility(View.GONE);
                chronometerStart = Date()
                timeStarted = true
                stopwatch.base = SystemClock.elapsedRealtime() - pauseAt
                stopwatch.start()
                buttonTimer.setText("STOP")
            }
        }

        buttonNext.setOnClickListener {
            stopwatch.stop()
            // TODO comment this for production deployment
            mediaPlayer.pause()
            // TODO uncomment this for production deployment
            // mediaPlayer.stop()
            exitActivity = Date()
            stopwatchValue = chronometer?.text as String
            Log.d("starttime", SimpleDateFormat(Constants.PHOTO_TIMESTAMP_FORMAT, Locale.UK).format(chronometerStart))
            Log.d("stoptime", SimpleDateFormat(Constants.PHOTO_TIMESTAMP_FORMAT, Locale.UK).format(exitActivity))
            Log.d("stopwatchval", stopwatchValue)
            val startIntent = Intent(this, MainActivity::class.java)
            startIntent.putExtra(Constants.STOPWATCH_START_TIME, SimpleDateFormat(Constants.PHOTO_TIMESTAMP_FORMAT, Locale.UK).format(chronometerStart))
            startIntent.putExtra(Constants.EXIT_STOPWATCH_ACTIVITY, SimpleDateFormat(Constants.PHOTO_TIMESTAMP_FORMAT, Locale.UK).format(exitActivity))
            startIntent.putExtra(Constants.STOPWATCH_VALUE, stopwatchValue)
            startActivity(startIntent)
        }

        chronometer.setOnChronometerTickListener {
            when (chronometer?.text) {
                "30:00" -> {
                    Log.d("30:00", "it is 30 minutes")
                }
                "00:03" -> {
                    Log.d("00:03", "it is 03 seconds")
                    mediaPlayer.start()
                    buttonNext.setVisibility(View.VISIBLE);
                }
            }
        }
    }

}