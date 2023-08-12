package com.example.test000

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class OnBoardingMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_main)

        val nextBoardBtn = findViewById<TextView>(R.id.nextBoardBtn)
        nextBoardBtn.setOnClickListener {
            val int = Intent(applicationContext, OnBoardingSub1::class.java)
            startActivity(int)
            finish()
        }
        val skipBoardBtn = findViewById<TextView>(R.id.skipBoardBtn)
        skipBoardBtn.setOnClickListener {
            val int = Intent(applicationContext, TextVoice2SL::class.java)
            startActivity(int)
            finish()
        }

    }
}