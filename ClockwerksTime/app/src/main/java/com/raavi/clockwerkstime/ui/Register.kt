package com.raavi.clockwerkstime.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.raavi.clockwerkstime.R
import com.raavi.clockwerkstime.UserModel
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.raavi.clockwerkstime.commonMethods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL

class Register : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    // Firebase Auth Variable
    private lateinit var authorisation: FirebaseAuth

    // Firebase Realtime Database Variable
    private lateinit var database: DatabaseReference

    // Variables for Typecasting
    private lateinit var editText_Name: EditText
    private lateinit var editText_Surname: EditText
    private lateinit var editText_Email: EditText
    private lateinit var editText_Password: EditText
    private lateinit var editText_ConfirmPassword: EditText
    private lateinit var button_SignUp: Button
    private lateinit var button_GoogleAuth: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        assignData()
        FirebaseApp.initializeApp(this)
        authorisation = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        button_GoogleAuth.setOnClickListener {
            signInWithGoogle()
        }

        signUpLogic()
    }

    // Shows Notifications
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Directs User To Login Screen
    fun sendToLogin(view: View) {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
    }

    // Typecasting
    private fun assignData() {
        editText_Name = findViewById(R.id.editText_SignUp_Name)
        editText_Surname = findViewById(R.id.editText_SignUp_Surname)
        editText_Email = findViewById(R.id.editText_SignUp_Email)
        editText_Password = findViewById(R.id.editText_SignUp_Password)
        editText_ConfirmPassword = findViewById(R.id.editText_SignUp_ConfirmPassword)
        button_SignUp = findViewById(R.id.button_SignUp_SignUp)
        button_GoogleAuth = findViewById(R.id.button_SignUp_SignUp_Google)
    }

    // Takes In User Credentials And Creates Them An Account. Also Populates Default Values For User Specific Categories And Profile Photo.
    private fun signUpLogic() {
        val defaultCategoryOptions = arrayOf("Client Projects", "Designing", "Meetings", "Planning", "Work", "Administration", "Overtime")

        button_SignUp.setOnClickListener {
            val name = editText_Name.text.toString().trim()
            val surname = editText_Surname.text.toString().trim()
            val email = editText_Email.text.toString().trim()
            val password = editText_Password.text.toString().trim()
            val confirmPassword = editText_ConfirmPassword.text.toString().trim()
            // Base64 String For Default Profile Photo
            val defaultPFP = "<Default_Profile_Picture_Base64>"

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                showToast("All fields are required. Please do not leave any fields empty.")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showToast("Inputted passwords do not match.")
                return@setOnClickListener
            }

            authorisation.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val key = authorisation.currentUser?.uid
                        val user = UserModel(name, surname, email, defaultPFP)
                        database.child(key!!).setValue(user)
                            .addOnFailureListener { exception ->
                                showToast("Failed to register user: ${exception.message}")
                            }
                        showToast("User registered successfully! Please log in to continue.")
                        val intent = Intent(this@Register, Login::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        showToast("Failed to register user: ${task.exception?.message}")
                    }
                }
        }
    }

    // Handle Google Sign-In
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account!!.idToken!!)
        } catch (e: ApiException) {
            showToast("Google sign in failed: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        authorisation.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val currentUser = authorisation.currentUser
                    if (currentUser != null) {
                        checkIfUserExistsOrRegister(currentUser)
                    }
                } else {
                    showToast("Google sign in failed: ${task.exception?.message}")
                }
            }
    }

    private fun checkIfUserExistsOrRegister(currentUser: FirebaseUser) {
        val database = FirebaseDatabase.getInstance().getReference("users")
        val userId = currentUser.uid

        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    showToast("User is already registered. Proceed to login.")
                    val intent = Intent(this@Register, Login::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    saveNewUser(currentUser)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database error: ${error.message}")
            }
        })
    }

    private fun saveNewUser(currentUser: FirebaseUser) {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            val name = account.displayName ?: "No Name"
            val surname = ""
            val email = account.email ?: "No Email"
            val photoUrl = account.photoUrl?.toString() ?: ""

            GlobalScope.launch(Dispatchers.IO) {
                val profilePicBase64 = convertImageToBase64(photoUrl)
                withContext(Dispatchers.Main) {
                    val user = UserModel(name, surname, email, profilePicBase64)
                    val userId = currentUser.uid

                    val database = FirebaseDatabase.getInstance().getReference("users")
                    database.child(userId).setValue(user)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showToast("User registered successfully with Google account.")
                                val intent = Intent(this@Register, Login::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                showToast("Failed to save user details: ${task.exception?.message}")
                            }
                        }
                }
            }
        }
    }

    private fun convertImageToBase64(imageUrl: String): String {
        return try {
            val inputStream = URL(imageUrl).openStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            "Empty"
        }
    }
}