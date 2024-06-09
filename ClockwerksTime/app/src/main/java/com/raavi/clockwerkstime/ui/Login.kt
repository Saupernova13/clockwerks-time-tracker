package com.raavi.clockwerkstime.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.raavi.clockwerkstime.MainActivity
import com.raavi.clockwerkstime.R
import com.raavi.clockwerkstime.SuccessfulLogin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.raavi.clockwerkstime.UserModel
import com.raavi.clockwerkstime.commonMethods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL

class Login : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    // Firebase Auth Variable
    private lateinit var authorisation: FirebaseAuth

    // Variables for Typecasting
    lateinit var editText_Email: EditText
    lateinit var editText_Password: EditText
    lateinit var button_Login: Button
    lateinit var button_GoogleAuth: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        assignData()
        FirebaseApp.initializeApp(this)
        authorisation = FirebaseAuth.getInstance()

        button_Login.setOnClickListener {
            val email = editText_Email.text.toString().trim()
            val pass = editText_Password.text.toString().trim()
            loginUser(email, pass, editText_Email, editText_Password)
            return@setOnClickListener
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        button_GoogleAuth.setOnClickListener {
            signInWithGoogle()
        }
    }

    //Takes in login details and form components. If credentials are incorrect, focuses on the components.
    fun loginUser(email: String, password: String, emailField: TextView, passwordField: TextView) {
        if (TextUtils.isEmpty(email)) {
            showToast("The E-mail field can't be empty.")
            emailField.requestFocus()
        } else if (TextUtils.isEmpty(password)) {
            showToast("The password field can't be empty.")
            passwordField.requestFocus()
        } else {
            authorisation.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        showToast("You have successfully logged in!")
                        val intent = Intent(this@Login, SuccessfulLogin::class.java)
                        startActivity(intent)
                        showToast("The program will now close to refresh your changes.")
                    } else {
                        showToast("Incorrect credentials provided. Please try again.")
                        emailField.requestFocus()
                    }
                }
        }
    }

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
            // Signed in successfully, now authenticate with Firebase
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
                    // Sign in success, check if user exists in the database
                    val currentUser = authorisation.currentUser
                    if (currentUser != null) {
                        checkIfUserExists(currentUser)
                    }
                } else {
                    showToast("Google sign in failed: ${task.exception?.message}")
                }
            }
    }

    private fun checkIfUserExists(currentUser: FirebaseUser) {
        val database = FirebaseDatabase.getInstance().getReference("users")
        val userId = currentUser.uid

        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User exists, proceed to the next activity
                    showToast("Sign in successful!")
                    val intent = Intent(this@Login, SuccessfulLogin::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // User doesn't exist, save user details
                    saveNewUser(currentUser)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database error: ${error.message}")
            }
        })
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
                                showToast("Typecasting Details saved successfully!")
                                val intent = Intent(this@Login, SuccessfulLogin::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                showToast("Failed to save details: ${task.exception?.message}")
                            }
                        }
                }
            }
        }
    }

    // Shows Notifications
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@Login, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Typecasting
    private fun assignData() {
        editText_Email = findViewById(R.id.editText_Login_Email)
        editText_Password = findViewById(R.id.editText_Login_Password)
        button_Login = findViewById(R.id.button_Login_LogIn)
        button_GoogleAuth = findViewById(R.id.button_Login_LogIn_Google)
    }
    // Directs User To Register Screen
    fun sendToRegister(view: View) {
        val intent = Intent(this, Register::class.java)
        startActivity(intent)
    }
}