package com.raavi.clockwerkstime

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.raavi.clockwerkstime.databinding.ActivityNavDrawerBinding
import com.raavi.clockwerkstime.ui.Login
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
class NavDrawer : AppCompatActivity() {
    //Variables for Typecasting
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityNavDrawerBinding
    private lateinit var userPFP: ImageView
    private lateinit var navTitle: TextView
    private lateinit var navSubTitle: TextView
    //Get Current User From Firebase
    private val user = FirebaseAuth.getInstance().currentUser
    //Firebase Realtime Database Up To Users Node
    private val database = FirebaseDatabase.getInstance().getReference("users")
    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarNavDrawer.toolbar)
        binding.appBarNavDrawer.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        // Initialize Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.appBarNavDrawer.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val floatingActionButton = findViewById<FloatingActionButton>(R.id.fab)
        floatingActionButton.setOnClickListener { openNewTrackScreen() }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_nav_drawer)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        assignData()
        loadUserData(navView)
        userOptionSelection()
    }
//Removes Options Menu From Drawer (3-Dots Menu)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //menuInflater.inflate(R.menu.nav_drawer, menu)
        // return true
        return false
    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_nav_drawer)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    //Show Notification
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    //More Typecasting
    private fun assignData() {
        val navHeaderView = binding.navView.getHeaderView(0)
        userPFP = navHeaderView.findViewById(R.id.imageView_NavHead)
        navTitle = navHeaderView.findViewById(R.id.textView_NavTitle)
        navSubTitle = navHeaderView.findViewById(R.id.textView_NavSubTitle)
    }
//Pulls Data Of Logged In User From Firebase And Displays To NavDrawer Header
    private fun loadUserData(navView: NavigationView) {
        if (user == null) {
            userPFP.setImageResource(R.drawable.clockwerk_logo_icon)
            navTitle.text = "Clockwerks: Time Tracker"
            navSubTitle.text = "Sauraav Jayrajh ST10024620"
            navView.menu.findItem(R.id.nav_account).title = "Login/Sign Up"
        } else {
            database.child(user.uid).get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val userModel = dataSnapshot.getValue(UserModel::class.java)
                    navTitle.text = "${userModel?.name} ${userModel?.surname}"
                    navSubTitle.text = user.email
                    val PFPBase64 = userModel?.profilePic
                    val PFPBytes =
                        android.util.Base64.decode(PFPBase64, android.util.Base64.DEFAULT)
                    val PFPbitmap =
                        android.graphics.BitmapFactory.decodeByteArray(PFPBytes, 0, PFPBytes.size)
                    userPFP.setImageBitmap(PFPbitmap)
                }
            }.addOnFailureListener {
                showToast("Error fetching user data: ${it.message}")
            }
            navView.menu.findItem(R.id.nav_account).title = "Log Out/Close"
        }
    }

    //Handles Click Events And Opens Respective Intents Tied To NavDrawer Items
    private fun userOptionSelection() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            val intent = when (menuItem.itemId) {
                R.id.nav_account, R.id.nav_about, R.id.nav_track,
                R.id.nav_categories, R.id.nav_goals
                //    , R.id.nav_alarms
                -> {
                    if (user == null) {
                        if (menuItem.itemId == R.id.nav_account) {
                            Intent(this@NavDrawer, Login::class.java)
                        } else {
                            Toast.makeText(
                                this,
                                "Please log in to perform this action.",
                                Toast.LENGTH_SHORT
                            ).show()
                            Intent(this@NavDrawer, Login::class.java)
                        }
                    } else {
                        when (menuItem.itemId) {
                            R.id.nav_account -> {
                                signOut()
                                Intent(this@NavDrawer, FakePage::class.java)
                            }
                            R.id.nav_about -> Intent(this@NavDrawer, About::class.java)
                            R.id.nav_track -> Intent(this@NavDrawer, Track::class.java)
                            R.id.nav_categories -> Intent(this@NavDrawer, Categories::class.java)
                            R.id.nav_goals -> Intent(this@NavDrawer, Goals::class.java)
                            //R.id.nav_alarms -> Intent(this@NavDrawer, Alarms::class.java)
                            else -> null
                        }
                    }
                }
                else -> null
            }
            intent?.let {
                startActivity(it)
                true
            } ?: false
        }
    }
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) {
            showToast("Signed out successfully")
        }
    }
    //Opens New Track Screen If User Is Logged In
    private fun openNewTrackScreen() {
        if (user == null) {
            Toast.makeText(
                this,
                "Please log in to start tracking your time!",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val intent = Intent(this, Track::class.java)
            startActivity(intent)
        }
    }
}
