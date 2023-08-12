package com.example.test000

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class OnBoardingSub1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding_sub1)

        val nextBoardBtn = findViewById<TextView>(R.id.nextBoardBtn)
        nextBoardBtn.setOnClickListener {
            val int = Intent(applicationContext, OnBoardingSub2::class.java)
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