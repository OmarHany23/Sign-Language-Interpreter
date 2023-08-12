package com.example.test000

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class OnBoardingSub2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding_sub2)

        val startAppBtn = findViewById<Button>(R.id.startAppBtn)
        startAppBtn.setOnClickListener {
            val int = Intent(applicationContext, TextVoice2SL::class.java)
            startActivity(int)
            finish()
        }
    }
}