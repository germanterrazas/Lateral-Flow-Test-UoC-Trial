package uoc.ifm.dial.saamd.LFDApp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import uoc.ifm.dial.saamd.LFDApp.util.SharedPreferences

class UserRegistration : AppCompatActivity() {

    private lateinit var username: String
    private lateinit var consent: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_registration)

        val buttonConsent = findViewById<Button>(R.id.button_consent)
        val textViewConsent = findViewById<TextView>(R.id.textView4)
        val editTextUsername = findViewById<EditText>(R.id.editTextUserName)
        val editTextContact = findViewById<EditText>(R.id.editTextContact)

        textViewConsent.setMovementMethod(LinkMovementMethod.getInstance())

        val sharedPreference:SharedPreferences = SharedPreferences(this)
        username = sharedPreference.getValueString("USERNAME")
        consent = sharedPreference.getValueString("CONSENT_YES")
        password = sharedPreference.getValueString("PASSWORD")

        buttonConsent.setOnClickListener {
            if(username != null && consent != null && password != null){
                Log.d("username if", username)
                Log.d("password if", password)
                Log.d("consent if", consent)
            } else {
                sharedPreference.save("USERNAME", username)
                sharedPreference.save("CONSENT_YES", "YES")
                sharedPreference.save("PASSWORD", password)
                Log.d("username else", username)
                Log.d("password else", password)
                Log.d("consent else", consent)
                val startIntent = Intent(this, UserInstructions::class.java)
                startActivity(startIntent)
            }

        }
    }
}