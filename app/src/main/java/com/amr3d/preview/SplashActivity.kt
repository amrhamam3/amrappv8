package com.amr3d.preview

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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

        // Fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splashLogo)
        val titleText = findViewById<TextView>(R.id.splashTitle)
        val devText = findViewById<TextView>(R.id.splashDev)
        val progressBar = findViewById<ProgressBar>(R.id.splashProgress)
        val percentText = findViewById<TextView>(R.id.splashPercent)
        val glowLine = findViewById<View>(R.id.splashGlowLine)
        val wireframeBg = findViewById<WireframeBackgroundView>(R.id.wireframeBg)

        // كل العناصر تبدأ شفافة
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

        // خلفية الـ wireframe تظهر أول حاجة
        wireframeBg.fadeIn()

        // اللوجو يتحرك من تحت للمنتصف مع bounce
        val logoAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).setDuration(900),
                ObjectAnimator.ofFloat(logo, "scaleX", 0.3f, 1f).setDuration(1000),
                ObjectAnimator.ofFloat(logo, "scaleY", 0.3f, 1f).setDuration(1000),
                ObjectAnimator.ofFloat(logo, "translationY", 60f, 0f).setDuration(900)
            )
            interpolator = OvershootInterpolator(1.5f)
            startDelay = 300
        }

        // خط برتقالي
        val lineAnim = ObjectAnimator.ofFloat(glowLine, "scaleX", 0f, 1f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 1100
        }

        // نص الترحيب
        val titleAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f).setDuration(600),
                ObjectAnimator.ofFloat(titleText, "translationY", 40f, 0f).setDuration(600)
            )
            interpolator = OvershootInterpolator(1.2f)
            startDelay = 1300
        }

        // نص المطور
        val devAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(devText, "alpha", 0f, 1f).setDuration(600),
                ObjectAnimator.ofFloat(devText, "translationY", 30f, 0f).setDuration(600)
            )
            interpolator = DecelerateInterpolator()
            startDelay = 1700
        }

        // Progress Bar
        val progressAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f).setDuration(400),
                ObjectAnimator.ofFloat(percentText, "alpha", 0f, 1f).setDuration(400)
            )
            startDelay = 2000
        }

        logoAnim.start()
        lineAnim.start()
        titleAnim.start()
        devAnim.start()
        progressAnim.start()

        // Progress animation مع نسبة مئوية
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

        // انتقال للـ MainActivity
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
