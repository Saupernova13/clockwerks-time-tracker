    package com.raavi.clockwerkstime

    import android.icu.text.SimpleDateFormat
    import android.net.ParseException
    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle
    import android.widget.Button
    import android.widget.EditText
    import android.widget.LinearLayout
    import android.widget.TextView
    import android.widget.Toast
    import androidx.core.content.ContextCompat
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.DataSnapshot
    import com.google.firebase.database.DatabaseError
    import com.google.firebase.database.FirebaseDatabase
    import com.google.firebase.database.ValueEventListener
    import java.util.Locale
    class Goals : AppCompatActivity() {
        //Variables for Typecasting
        private lateinit var Edit_MinGoal: EditText
        private lateinit var Edit_MaxGoal: EditText
        private lateinit var Button_SaveGoals: Button
        private lateinit var LinearLayout_Goals: LinearLayout
        //Get Current User From Firebase
        private val user = FirebaseAuth.getInstance().currentUser
        //Firebase Realtime Database Up To Users Node
        private val database = FirebaseDatabase.getInstance().getReference("users")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_goals)
            Edit_MinGoal = findViewById(R.id.editText_Goals_MinGoals)
            Edit_MaxGoal = findViewById(R.id.editText_Goals_MaxGoals)
            Button_SaveGoals = findViewById(R.id.button_Goals_SaveGoals)
            LinearLayout_Goals = findViewById(R.id.linearLayout_Goals_List)
            Button_SaveGoals.setOnClickListener {
                saveGoals()
            }
            loadGoals()
        }
        //Capture's Goal Info And Saves It To Firebase
        private fun saveGoals() {
            val minGoal = Edit_MinGoal.text.toString().toDoubleOrNull()
            val maxGoal = Edit_MaxGoal.text.toString().toDoubleOrNull()
            if (minGoal != null && maxGoal != null && minGoal < maxGoal) {
                user?.let { user ->
                    val goalRef = database.child(user.uid).child("goals").push()
                    val goal = GoalModel(goalRef.key!!, minGoal, maxGoal, false)
                    goalRef.setValue(goal)
                        .addOnSuccessListener {
                            showToast("Goal saved successfully")
                        }
                        .addOnFailureListener {
                            showToast("Failed to save goal")
                        }
                }
            } else {
                showToast("Please enter valid goals (minimum < maximum)")
            }
        }
        //Loads Goal Info From Firebase To Program
        private fun loadGoals() {
            user?.let { user ->
                database.child(user.uid).child("goals")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            LinearLayout_Goals.removeAllViews()
                            for (goalSnapshot in dataSnapshot.children) {
                                val goalModel = goalSnapshot.getValue(GoalModel::class.java)
                                if (goalModel != null) {
                                    try {
                                            addGoalCard(goalModel)
                                    } catch (e: ParseException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        override fun onCancelled(databaseError: DatabaseError) {
                            showToast("Error Loading Goal Data!")
                        }
                    })
            }
        }
        //Populates UI With Cards For Goals
        private fun addGoalCard(goal: GoalModel) {
            val currentTime = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val currentDate = sdf.format(currentTime)
            val minGoalTimeMillis = goal.minGoalHours * 60 * 60 * 1000
            val maxGoalTimeMillis = goal.maxGoalHours * 60 * 60 * 1000
            val goalReached = currentTime >= minGoalTimeMillis && currentTime <= maxGoalTimeMillis
            val cardView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 10)
                }
                background = ContextCompat.getDrawable(this@Goals, R.drawable.rounded_rectangle)
                setPadding(20, 20, 20, 20)
            }
            val goalLabel = TextView(this).apply {
                val yesNo = if (goalReached) "Yes" else "No"
                text = "Goal:\nMinimum Work Time: ${goal.minGoalHours} Hours\nMaximum Work Time: ${goal.maxGoalHours} Hours\nCompleted: ${yesNo}"
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@Goals, R.color.bananaBrown))
            }
            cardView.addView(goalLabel)
            LinearLayout_Goals.addView(cardView)
            if (goalReached) {
                showToast("Congratulations! You've reached your daily goal for $currentDate")
            }
        }
        //Shows Notifications
        private fun showToast(message: String) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }