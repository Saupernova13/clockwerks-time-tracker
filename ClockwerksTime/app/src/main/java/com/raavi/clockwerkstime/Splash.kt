package com.raavi.clockwerkstime

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.bumptech.glide.Glide
import android.widget.ImageView

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        //Start GIFs
        Glide.with(this)
            .load(R.drawable.gears_splash)
            .into(findViewById(R.id.clockWerkGIF0))
        //Start Splash
  Handler().postDelayed({val intent = Intent(this, NavDrawer::class.java)
            startActivity(intent)
            finish()
        }, 3000)    }
}