package com.funglejunk.brewwatch.view

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.funglejunk.brewwatch.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var backgroundAnimation: AnimationDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        startBackgroundAnimation()
    }

    private fun startBackgroundAnimation() {
        backgroundAnimation.run {
            when (this) {
                null -> initAnimationDrawable()
                else -> this
            }
        }.apply {
            start()
        }
    }

    private fun initAnimationDrawable(): AnimationDrawable {
        return (root_layout.background as AnimationDrawable).apply {
            setEnterFadeDuration(10)
            setExitFadeDuration(5000)
        }
    }

    override fun onPause() {
        backgroundAnimation?.stop()
        super.onPause()
    }

}
