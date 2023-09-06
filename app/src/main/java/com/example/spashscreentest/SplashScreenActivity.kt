package com.example.spashscreentest

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.TextView

@Suppress("DEPRECATION")
class SplashScreenActivity : AppCompatActivity(R.layout.activity_splash_screen) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val splashText: TextView = findViewById(R.id.splash_text)
        val fadeInAmination = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        splashText.startAnimation(fadeInAmination)
        Handler().postDelayed({
            startActivity(Intent(this, PreviewActivity::class.java))
            finish()
        }, 3000)
    }
}