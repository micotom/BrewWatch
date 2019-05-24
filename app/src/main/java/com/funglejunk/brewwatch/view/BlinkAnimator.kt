package com.funglejunk.brewwatch.view

import android.os.Handler
import android.view.View

class BlinkAnimator(private val interval: Long) {

    private val handler = Handler()

    private var runnable: BlinkRunnable? = null

    private class BlinkRunnable(private val view: View,
                                private val interval: Long,
                                private val handler: Handler) : Runnable {

        private var alphaValue = 0f

        override fun run() {
            if (alphaValue >= Float.MAX_VALUE) {
                alphaValue = 0f
            }
            view.alpha = alphaValue++ % 2
            handler.postDelayed(this, interval)
        }

    }

    fun startAnimation(view: View) {
        handler.run {
            runnable = BlinkRunnable(view, interval, this)
            postDelayed(runnable, interval)
        }
    }

    fun stopAnimation(view: View) {
        view.alpha = 1f
        runnable?.let {
            handler.removeCallbacks(it)
        }
    }

}