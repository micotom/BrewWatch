package com.funglejunk.brewwatch

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (root_layout.background as AnimationDrawable).run {
            setEnterFadeDuration(10)
            setExitFadeDuration(5000)
            start()
        }
    }

}
