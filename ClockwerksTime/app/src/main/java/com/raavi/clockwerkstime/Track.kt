package com.raavi.clockwerkstime

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Calendar
import java.util.Locale

class Track : AppCompatActivity() {
    private lateinit var authorisation: FirebaseAuth
    lateinit var buttonDateStart: Button
    lateinit var buttonDateEnd: Button
    lateinit var buttonTimeStart: Button
    lateinit var buttonTimeEnd: Button
    lateinit var buttonAddMedia: Button
    lateinit var buttonRemoveMedia: Button
    lateinit var mediaViewer: ImageView
    lateinit var realtimeTimer: TextView
    // lateinit var acTextViewTaskCategory: AutoCompleteTextView
    lateinit var acTextViewTaskCategory: Spinner
    lateinit var editTextTaskDescription: EditText
    private var StartDate: Calendar? = null
    private var EndDate: Calendar? = null
    private var StartTime: Calendar? = null
    private var EndTime: Calendar? = null
    private lateinit var database: DatabaseReference
    private val currentTrack = TrackModel()
    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    //Allowa For Continuous Running Of The Timer
    private val runnable = object : Runnable {
        override fun run() {
            val elapsedMillis = SystemClock.elapsedRealtime() - startTime
            val hours = (elapsedMillis / 3600000)
            val minutes = (elapsedMillis / 60000) % 60
            val seconds = (elapsedMillis / 1000) % 60
            val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            realtimeTimer.text = time
            handler.postDelayed(this, 50)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track)
        //Firebase Initialisation
        authorisation = FirebaseAuth.getInstance()
        //Firebase Realtime Database Up To Users Node
        database = FirebaseDatabase.getInstance().reference.child("users")
//        val initialHours = 0
//        val initialMinutes = 11
//        val initialSeconds = 33
//        val initialTimeMillis = ((initialHours * 3600) + (initialMinutes * 60) + initialSeconds) * 1000

        //More Typecasting
        buttonDateStart = findViewById(R.id.button_Track_Date_Start)
        buttonDateEnd = findViewById(R.id.button_Track_Date_End)
        buttonTimeStart = findViewById(R.id.button_Track_Time_Start)
        buttonTimeEnd = findViewById(R.id.button_Track_Time_End)
        buttonAddMedia = findViewById(R.id.button_Track_Media_Add)
        buttonRemoveMedia = findViewById(R.id.button_Track_Media_Delete)
        realtimeTimer = findViewById(R.id.textView_Track_Time_Timer)
        mediaViewer = findViewById(R.id.imageView_Track_Media_1)
        editTextTaskDescription = findViewById(R.id.editTextTextMultiLine_Track_Task_Description)
        //acTextViewTaskCategory = findViewById(R.id.autoCompleteTextView_Task_Category)
        acTextViewTaskCategory = findViewById(R.id.spinner_Task_Category)
        populateDropdown()
        setCurrentTimeOnButtons()
        startTime = SystemClock.elapsedRealtime()
        handler.postDelayed(runnable, 0)
        //OnCLick Methods For Setting Dates And Times
        buttonTimeEnd.setOnClickListener {
            openTimePicker(endTimeListener)
        }
        buttonDateEnd.setOnClickListener {
            openDatePicker(endDateListener)
        }
        buttonDateStart.setOnClickListener {
            openDatePicker(startDateListener)
        }
        buttonTimeStart.setOnClickListener {
            openTimePicker(startTimeListener)
        }
        buttonRemoveMedia.setOnClickListener {
            currentTrack.taskMedia = "Empty"
        }
    }
    //Attempts To Prevent User From Leaving
    override fun onDestroy() {
        if (currentTrack.endDate.toString().isEmpty()) {
            showToast("Please set an end date first!")
        } else if (currentTrack.endTime.toString().isEmpty()) {
            showToast("Please set an end time first!")
        } else if (currentTrack.startDate.toString().isEmpty()) {
            showToast("Please set a start date first!")
        } else if (currentTrack.startTime.toString().isEmpty()) {
            showToast("Please set a start time first!")
        } else {
            handler.removeCallbacks(runnable)
            saveToFirebase(currentTrack)
            super.onDestroy()
        }
    }
    //Methods That Opem Menus For Setting Dates And Times
    private fun openDatePicker(dateSetListener: DatePickerDialog.OnDateSetListener) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(this, dateSetListener, year, month, day)
        datePickerDialog.show()
    }
    private fun openTimePicker(timeSetListener: TimePickerDialog.OnTimeSetListener) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(this, timeSetListener, hour, minute, true)
        timePickerDialog.show()
    }
    private val startDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.set(year, month, day)
        StartDate = selectedCalendar
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        buttonDateStart.text = dateFormat.format(selectedCalendar.time)
        currentTrack.startDate = buttonDateStart.text.toString()
    }
    private val startTimeListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
        val selectedCalendar = Calendar.getInstance()
        StartTime ?: run { StartTime = selectedCalendar }
        StartTime?.set(Calendar.HOUR_OF_DAY, hour)
        StartTime?.set(Calendar.MINUTE, minute)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        buttonTimeStart.text = timeFormat.format(StartTime?.time)
        currentTrack.startTime = buttonTimeStart.text.toString()
    }
    private val endDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.set(year, month, day)
        EndDate = selectedCalendar
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        buttonDateEnd.text = dateFormat.format(selectedCalendar.time)
        currentTrack.endDate = buttonDateEnd.text.toString()
    }
    private val endTimeListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
        val selectedCalendar = Calendar.getInstance()
        EndTime ?: run { EndTime = selectedCalendar }
        EndTime?.set(Calendar.HOUR_OF_DAY, hour)
        EndTime?.set(Calendar.MINUTE, minute)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        buttonTimeEnd.text = timeFormat.format(EndTime?.time)
        currentTrack.endTime = buttonTimeEnd.text.toString()
    }
    //Method To Send Dates To Text On Buttons
    private fun setCurrentTimeOnButtons() {
        val currentTime = LocalDateTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val currentTimeFormatted = currentTime.format(timeFormatter)
        buttonTimeStart.text = currentTimeFormatted.toString()
        val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val currentDateFormatted = currentTime.format(dayFormatter)
        buttonDateStart.text = currentDateFormatted.toString()
    }
    //Saves Data Of Current Tracked Session To Firebase
    fun saveToFirebase(currentTrack: TrackModel) {
        val currentUser = authorisation.currentUser?.uid
        //Current Time Is Unique To Each Session, Per User. Makes a good Uniqe Identifier
        val currentTimeFormatted =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd:HH:mm:ss"))
        currentTrack.id = currentTimeFormatted.toString()
        val startTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTimeCalendar = Calendar.getInstance().apply {
            time = startTimeFormatter.parse(buttonTimeStart.text.toString())
        }
        val startDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDateCalendar = Calendar.getInstance().apply {
            time = startDateFormatter.parse(buttonDateStart.text.toString())
        }
        //Autocalculation Of EndDate
        val timeSpentWorkingParts = realtimeTimer.text.toString().split(":")
        val hoursWorked = timeSpentWorkingParts[0].toInt()
        val minutesWorked = timeSpentWorkingParts[1].toInt()
        val secondsWorked = timeSpentWorkingParts[2].toInt()
        startTimeCalendar.add(Calendar.HOUR_OF_DAY, hoursWorked)
        startTimeCalendar.add(Calendar.MINUTE, minutesWorked)
        startTimeCalendar.add(Calendar.SECOND, secondsWorked)
        val endTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val endTimeString = endTimeFormatter.format(startTimeCalendar.time)
        val endDateCalendar = startDateCalendar.clone() as Calendar
        endDateCalendar.add(Calendar.HOUR_OF_DAY, hoursWorked)
        endDateCalendar.add(Calendar.MINUTE, minutesWorked)
        endDateCalendar.add(Calendar.SECOND, secondsWorked)
        val endDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val endDateString = endDateFormatter.format(endDateCalendar.time)
        //Assignment Of Anything That Hasn't Been Assigned
        currentTrack.startTime = buttonTimeStart.text.toString()
        currentTrack.startDate = buttonDateStart.text.toString()
        currentTrack.endTime = endTimeString
        currentTrack.endDate = endDateString
        currentTrack.timeSpentWorking = realtimeTimer.text.toString()
        currentTrack.taskCategory = acTextViewTaskCategory.selectedItem.toString()
        currentTrack.taskDescription = editTextTaskDescription.text.toString()
        if (currentTrack.taskMedia == null) {
            currentTrack.taskMedia = "Empty"
        }
        //Push To Firebase
        val key = database.child(currentUser.toString()).child("trackedSessions")
            .child(currentTrack.id.toString()).push().key
        if (key != null) {
            database.child(currentUser.toString()).child("trackedSessions")
                .child(currentTrack.id.toString()).setValue(currentTrack)
                .addOnSuccessListener {
                    showToast("Successfully saved!")
                }
                .addOnFailureListener() {
                    showToast("Failed to save data! Please check your internet connection.")
                }
        }
    }
    //Captures Image From Camera Or Allows Upload From Gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val image = data?.extras?.get("data") as? Bitmap
                    if (image != null) {
                        handleImageResult(image)
                    } else {
                        showToast("Failed to capture image")
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    val selectedImage = data?.data
                    if (selectedImage != null) {
                        try {
                            val inputStream = contentResolver.openInputStream(selectedImage)
                            val image = BitmapFactory.decodeStream(inputStream)
                            handleImageResult(image)
                        } catch (e: Exception) {
                            showToast("Failed to load image from gallery")
                            e.printStackTrace()
                        }
                    } else {
                        showToast("No image selected")
                    }
                }
            }
        }
    }
    //Converts Snapped Image To Base64
    private fun handleImageResult(image: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray())
        currentTrack.taskMedia = base64Image
        mediaViewer.setImageBitmap(image)
    }
    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val GALLERY_REQUEST_CODE = 101
    }
    //Opens Menu To Select Camera Or Gallery (Or Quit)
    fun CaptureImage(view: View) {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
                2 -> { /* Cancel, do nothing */
                }
            }
        }
        builder.show()
    }
    //Opens Camera
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }
    //Opens Gallery
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }
    //    private fun saveDropdown(inputString: String) {
//        val user = FirebaseAuth.getInstance().currentUser
//        val db = FirebaseDatabase.getInstance().getReference("users")
//        val currentUser = user?.uid
//        val key = database.child(currentUser.toString()).child("userDropDownOptions").push().key
//        if (key != null) {
//            database.child(currentUser.toString()).child("userDropDownOptions").setValue(inputString)
//                .addOnSuccessListener {
//                    showToast("Successfully saved!")
//                }
//                .addOnFailureListener() {
//                    showToast("Failed to save data! Please check your internet connection.")
//                }
//        }
//    }

    //Populates Dropdown Menu With Default And User Generated Categories
    private fun populateDropdown() {
        val dropDownOptions = ArrayList<String>()
        if (!dropDownOptions.contains("Client Projects")) dropDownOptions.add("Client Projects")
        if (!dropDownOptions.contains("Designing")) dropDownOptions.add("Designing")
        if (!dropDownOptions.contains("Meetings")) dropDownOptions.add("Meetings")
        if (!dropDownOptions.contains("Planning")) dropDownOptions.add("Planning")
        if (!dropDownOptions.contains("Work")) dropDownOptions.add("Work")
        if (!dropDownOptions.contains("Administration")) dropDownOptions.add("Administration")
        if (!dropDownOptions.contains("Overtime")) dropDownOptions.add("Overtime")
//        val user = FirebaseAuth.getInstance().currentUser
//        database.child(user?.uid.toString()).child("userDropDownOptions").get().addOnSuccessListener { dataSnapshot ->
//            if (dataSnapshot.exists()) {
//                val dropDownItem = dataSnapshot.getValue(String::class.java)
//                if (dropDownItem != null) {
//                    if (!dropDownOptions.contains(dropDownItem)) {
//                        dropDownOptions.add(dropDownItem)
//                    }
//                }
//            }
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dropDownOptions)
        acTextViewTaskCategory.setAdapter(adapter)
//        }.addOnFailureListener {
//            showToast("Error fetching user data: ${it.message}")
//        }
    }
    //Shows Notification
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}