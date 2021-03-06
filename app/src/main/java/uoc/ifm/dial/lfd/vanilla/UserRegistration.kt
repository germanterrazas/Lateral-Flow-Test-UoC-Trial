package uoc.ifm.dial.lfd.vanilla

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uoc.ifm.dial.lfd.vanilla.util.Constants
import uoc.ifm.dial.lfd.vanilla.util.SharedPreferences

class UserRegistration : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_registration)

        val buttonConsent = findViewById<Button>(R.id.button_consent)
        val textViewConsent = findViewById<TextView>(R.id.textView4)
        textViewConsent.movementMethod = LinkMovementMethod.getInstance()

        buttonConsent.setOnClickListener {
            val sharedPreference = SharedPreferences(this)
            sharedPreference.save(Constants.SHARED_PREF_DEVICE_ID, getRandomString(Constants.SHARED_PREF_RANDOM_DEVICE_ID_LENGHT))
            sharedPreference.save(Constants.SHARED_PREF_CONSENT, Constants.SHARED_PREF_CONSENT_VALUE)
            val startIntent = Intent(this, MainActivity::class.java)
            startActivity(startIntent)
        }
    }

    private fun getRandomString(length: Int) : String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }
}