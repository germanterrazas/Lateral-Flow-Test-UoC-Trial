package uoc.ifm.dial.saamd.LFDApp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import uoc.ifm.dial.saamd.LFDApp.util.Constants
import uoc.ifm.dial.saamd.LFDApp.util.SharedPreferences

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        val sharedPreference = SharedPreferences(this)
        val username = sharedPreference.getValueString(Constants.SHARED_PREF_DEVICE_ID)
        val consent = sharedPreference.getValueString(Constants.SHARED_PREF_CONSENT)
        val startConsent = findViewById<Button>(R.id.button_start)

        startConsent.setOnClickListener {
            if(username.isNotEmpty() && consent.isNotEmpty()) {
                val startIntent = Intent(this, MainActivity::class.java)
                startActivity(startIntent)
            } else {
                val startIntent = Intent(this, UserRegistration::class.java)
                startActivity(startIntent)
            }
        }
    }
}