package com.raavi.clockwerkstime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class FakePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_page)
        //Acts As A Fakeout To Let User Quit App From NavMenu
        finishAffinity()
    }
}