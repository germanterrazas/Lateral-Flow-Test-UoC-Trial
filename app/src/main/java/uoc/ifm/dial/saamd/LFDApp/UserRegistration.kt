package uoc.ifm.dial.saamd.LFDApp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import uoc.ifm.dial.saamd.LFDApp.util.Constants

class UserRegistration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_registration)

        val buttonConsent = findViewById<Button>(R.id.button_consent)
        val textViewConsent = findViewById<TextView>(R.id.textView4)
        textViewConsent.setMovementMethod(LinkMovementMethod.getInstance())

        buttonConsent.setOnClickListener {
            val startIntent = Intent(this, UserInstructions::class.java)
            startActivity(startIntent)
        }
    }
}