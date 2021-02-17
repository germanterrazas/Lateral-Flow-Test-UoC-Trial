package uoc.ifm.dial.saamd.LFDApp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup

class Options : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

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

        buttonSubmit.setOnClickListener {
            val optionChecked = rgLFTOptions.checkedRadioButtonId
            val rbChecked = findViewById<RadioButton>(optionChecked)
            Log.d("value", rbChecked.text.toString())
        }

    }
}