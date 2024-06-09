package com.raavi.clockwerkstime
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.ParseException
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
class Categories : AppCompatActivity() {
    //Variables for Typecasting
    private lateinit var spinner: Spinner
    private lateinit var sessionsContainer: LinearLayout
    private lateinit var totalTimeTextView: TextView
    lateinit var  btnStartDate: Button
    lateinit var  btnEndDate: Button
    private var startDate: Long? = null
    private var endDate: Long? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        spinner = findViewById(R.id.spinner_Categories)
        sessionsContainer = findViewById(R.id.sessionsContainer)
        totalTimeTextView = findViewById(R.id.TextView_Categories_totalTime)
        btnStartDate = findViewById(R.id.button_Category_StartDate)
        btnEndDate = findViewById(R.id.button_Category_EndDate)
        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }
        setupCategorySpinner()
    }
    //Shows Menu For Selecting Dates
    private fun showDatePicker(start: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            val selectedDate = calendar.timeInMillis
            if (start) {
                startDate = selectedDate
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                btnStartDate.text = dateFormat.format(Date(selectedDate))
            } else {
                endDate = selectedDate
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                btnEndDate.text = dateFormat.format(Date(selectedDate))
            }
            spinner.selectedItem?.let { filterSessionsByCategory(it.toString()) }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    //Confugure's Default Values For Spinner
    private fun setupCategorySpinner() {
        val categories = arrayOf("Client Projects", "Designing", "Meetings", "Planning", "Work", "Administration", "Overtime")
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                filterSessionsByCategory(categories[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
    }
    //Allows User To View Tracked Sessions By Category Or Within Date Range
    private fun filterSessionsByCategory(category: String) {
        val database = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().currentUser?.uid.toString()).child("trackedSessions")
        database.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                sessionsContainer.removeAllViews()
                var totalSeconds = 0
                dataSnapshot.children.forEach { session ->
                    val trackModel = session.getValue(TrackModel::class.java)
                    if (trackModel != null && trackModel.taskCategory == category && trackModel.startDate != null) {
                        try {
                            val sessionDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(trackModel.startDate).time
                            val start = startDate ?: Long.MIN_VALUE
                            val end = endDate ?: Long.MAX_VALUE

                            if (sessionDate in start..end) {
                                addRectangleView(trackModel)
                                totalSeconds += parseTimeToSeconds(trackModel.timeSpentWorking ?: "00:00:00")
                            }
                            if (start>end){
                                showToast("Please set a valid date range")
                            }
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                    }
                }
                totalTimeTextView.text = "Total Time: ${formatSecondsToHHMMSS(totalSeconds)}"
            }
            override fun onCancelled(databaseError: DatabaseError) {
                showToast("Error loading sessions")
            }
        })
    }
    //Populates UI With Cards For Tracked Sessions
    private fun addRectangleView(trackModel: TrackModel?) {
        val cardView = LinearLayout(this@Categories).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply {
                setMargins(10, 20, 10, 10)
            }
            background = ContextCompat.getDrawable(this@Categories, R.drawable.rounded_rectangle)
            setPadding(20, 20, 20, 20)
        }
        val textInfoContainer = LinearLayout(this@Categories).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
        }
        val timeInfoContainer = LinearLayout(this@Categories).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 10, 10)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val startTimeView = TextView(this@Categories).apply {
            text = "Started: ${trackModel?.startTime}\n${trackModel?.startDate}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@Categories, R.color.bananaBrown))
        }
        val endTimeView = TextView(this@Categories).apply {
            text = "Ended: ${trackModel?.endTime}\n${trackModel?.endDate}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@Categories, R.color.bananaBrown))
        }
        val timeSpentView = TextView(this@Categories).apply {
            text = "Time worked: ${trackModel?.timeSpentWorking}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@Categories, R.color.bananaBrown))
        }
        val categoryView = TextView(this@Categories).apply {
            text = "Category: ${trackModel?.taskCategory}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@Categories, R.color.bananaBrown))
            setPadding(0, 10, 0, 0)
        }
        val descriptionView = TextView(this@Categories).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = trackModel?.taskDescription ?: "No Description"
            setTextColor(ContextCompat.getColor(this@Categories, R.color.bananaBrown))
            textSize = 14f
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 10, 0, 0)
        }
        timeInfoContainer.addView(startTimeView)
        timeInfoContainer.addView(timeSpentView)
        timeInfoContainer.addView(endTimeView)
        textInfoContainer.addView(timeInfoContainer)
        textInfoContainer.addView(categoryView)
        textInfoContainer.addView(descriptionView)
        val mediaView = ImageView(this@Categories).apply {
            layoutParams = LinearLayout.LayoutParams(225, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(10,10,10,10)
        }
        if (trackModel?.taskMedia == "Empty") {
            mediaView.setImageResource(R.drawable.no_image_available)
        } else {
            val imageBytes = Base64.decode(trackModel?.taskMedia, Base64.DEFAULT)
            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            mediaView.setImageBitmap(decodedImage)
        }
        cardView.addView(textInfoContainer)
        cardView.addView(mediaView)
        sessionsContainer.addView(cardView)
        sessionsContainer.setPadding(20,10,20,10)
    }
    //Converts Time Spent To Seconds
    fun parseTimeToSeconds(time: String): Int {
        val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
        return parts[0] * 3600 + parts[1] * 60 + parts[2]
    }
    //Converts Seconds To Hours, Minutes, Seconds
    fun formatSecondsToHHMMSS(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val sec = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, sec)
    }
    //Shows Notifications
    private fun showToast(message: String) {
        Toast.makeText(this@Categories, message, Toast.LENGTH_SHORT).show()
    }
}