package com.raavi.clockwerkstime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth

class SuccessfulLogin : AppCompatActivity() {
lateinit var imageBG : ImageView
    private val user = FirebaseAuth.getInstance().currentUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Typecasting
        setContentView(R.layout.activity_successful_login)
        imageBG = findViewById(R.id.imageView_BG)
        //Lets User Clsoe App From Smiley Face Press
        imageBG.setOnClickListener{
            finishAffinity()
        }
    }
}