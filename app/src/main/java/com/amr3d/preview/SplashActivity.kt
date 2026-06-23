package com.amr3d.preview

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splashLogo)
        val titleText = findViewById<TextView>(R.id.splashTitle)
        val devText = findViewById<TextView>(R.id.splashDev)
        val progressBar = findViewById<ProgressBar>(R.id.splashProgress)
        val percentText = findViewById<TextView>(R.id.splashPercent)
        val glowLine = findViewById<View>(R.id.splashGlowLine)
        val wireframeBg = findViewById<WireframeBackgroundView>(R.id.wireframeBg)

        logo.alpha = 0f
        logo.scaleX = 0.3f
        logo.scaleY = 0.3f
        logo.translationY = 60f
        titleText.alpha = 0f
        titleText.translationY = 40f
        devText.alpha = 0f
        devText.translationY = 30f
        progressBar.alpha = 0f
        percentText.alpha = 0f
        glowLine.scaleX = 0f

        wireframeBg.fadeIn()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).setDuration(900),
                ObjectAnimator.ofFloat(logo, "scaleX", 0.3f, 1f).setDuration(1000),
                ObjectAnimator.ofFloat(logo, "scaleY", 0.3f, 1f).setDuration(1000),
                ObjectAnimator.ofFloat(logo, "translationY", 60f, 0f).setDuration(900)
            )
            interpolator = OvershootInterpolator(1.5f)
            startDelay = 300
        }.start()

        ObjectAnimator.ofFloat(glowLine, "scaleX", 0f, 1f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 1100
        }.start()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f).setDuration(600),
                ObjectAnimator.ofFloat(titleText, "translationY", 40f, 0f).setDuration(600)
            )
            interpolator = OvershootInterpolator(1.2f)
            startDelay = 1300
        }.start()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(devText, "alpha", 0f, 1f).setDuration(600),
                ObjectAnimator.ofFloat(devText, "translationY", 30f, 0f).setDuration(600)
            )
            interpolator = DecelerateInterpolator()
            startDelay = 1700
        }.start()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f).setDuration(400),
                ObjectAnimator.ofFloat(percentText, "alpha", 0f, 1f).setDuration(400)
            )
            startDelay = 2000
        }.start()

        ValueAnimator.ofInt(0, 100).apply {
            duration = SPLASH_DURATION - 600
            startDelay = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                progressBar.progress = value
                percentText.text = "$value%"
            }
        }.start()

        Handler(Looper.getMainLooper()).postDelayed({
            val rootView = findViewById<View>(android.R.id.content)
            ObjectAnimator.ofFloat(rootView, "alpha", 1f, 0f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }, 400)
        }, SPLASH_DURATION)
    }
}
