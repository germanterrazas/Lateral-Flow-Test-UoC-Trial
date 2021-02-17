package uoc.ifm.dial.saamd.LFDApp

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uoc.ifm.dial.saamd.LFDApp.util.SharedPreferences

class UserRegistration : AppCompatActivity() {

    private lateinit var username: String
    private lateinit var consent: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_registration)

        val buttonConsent = findViewById<Button>(R.id.button_consent)
        val textViewConsent = findViewById<TextView>(R.id.textView4)
        textViewConsent.setMovementMethod(LinkMovementMethod.getInstance())

        val sharedPreference:SharedPreferences = SharedPreferences(this)
        username = sharedPreference.getValueString("USERNAME")
        consent = sharedPreference.getValueString("CONSENT_YES")

        // @TODO under development
        buttonConsent.setOnClickListener {
            if(username.length > 0 && consent.length > 0){
                Log.d("username if", username)
                Log.d("consent if", consent)
                val startIntent = Intent(this, MainActivity::class.java)
                startActivity(startIntent)
            } else {
//                sharedPreference.save("USERNAME", editTextUsername.text.toString())
                sharedPreference.save("USERNAME", "gt401")
                sharedPreference.save("CONSENT_YES", "YES")
                val startIntent = Intent(this, MainActivity::class.java)
                startActivity(startIntent)
            }

        }
    }
}