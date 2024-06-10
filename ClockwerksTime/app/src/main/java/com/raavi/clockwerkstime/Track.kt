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
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
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
    private var baseTime: Long = 0L

    private val runnable = object : Runnable {
        override fun run() {
            // Calculate elapsed time and set the timer text
            val elapsedMillis = SystemClock.elapsedRealtime() - startTime + baseTime
            val hours = (elapsedMillis / 3600000).toInt()
            val minutes = ((elapsedMillis % 3600000) / 60000).toInt()
            val seconds = ((elapsedMillis % 60000) / 1000).toInt()
            val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            realtimeTimer.text = time
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track)
        authorisation = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")

        buttonDateStart = findViewById(R.id.button_Track_Date_Start)
        buttonDateEnd = findViewById(R.id.button_Track_Date_End)
        buttonTimeStart = findViewById(R.id.button_Track_Time_Start)
        buttonTimeEnd = findViewById(R.id.button_Track_Time_End)
        buttonAddMedia = findViewById(R.id.button_Track_Media_Add)
        buttonRemoveMedia = findViewById(R.id.button_Track_Media_Delete)
        realtimeTimer = findViewById(R.id.textView_Track_Time_Timer)
        mediaViewer = findViewById(R.id.imageView_Track_Media_1)
        editTextTaskDescription = findViewById(R.id.editTextTextMultiLine_Track_Task_Description)
        acTextViewTaskCategory = findViewById(R.id.spinner_Task_Category)

        val sessionId = intent.getStringExtra("SESSION_ID")
        if (sessionId != null) {
            buttonDateStart.text = intent.getStringExtra("START_DATE")
            buttonDateEnd.text = intent.getStringExtra("END_DATE")
            buttonTimeStart.text = intent.getStringExtra("START_TIME")
            buttonTimeEnd.text = intent.getStringExtra("END_TIME")
            editTextTaskDescription.setText(intent.getStringExtra("TASK_DESCRIPTION"))

            val taskCategory = intent.getStringExtra("TASK_CATEGORY")
            loadTrackCategorySpinner(taskCategory)

            val taskMedia = intent.getStringExtra("TASK_MEDIA")
            if (taskMedia != "Empty") {
                val imageBytes = Base64.decode(taskMedia, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                mediaViewer.setImageBitmap(decodedImage)
            }

            val timeSpentWorking = intent.getStringExtra("TIME_SPENT_WORKING")
            baseTime = parseTimeToMillis(timeSpentWorking!!)

            startTime = SystemClock.elapsedRealtime()
            handler.postDelayed(runnable, 0)
        } else {
            setCurrentTimeOnButtons()
            startTime = SystemClock.elapsedRealtime()
            handler.postDelayed(runnable, 0)
        }

        buttonTimeEnd.setOnClickListener { openTimePicker(endTimeListener) }
        buttonDateEnd.setOnClickListener { openDatePicker(endDateListener) }
        buttonDateStart.setOnClickListener { openDatePicker(startDateListener) }
        buttonTimeStart.setOnClickListener { openTimePicker(startTimeListener) }
        buttonRemoveMedia.setOnClickListener { currentTrack.taskMedia = "Empty" }
    }

    private fun loadTrackCategorySpinner(selectedCategory: String?) {
        val dropDownOptions = arrayListOf("Client Projects", "Designing", "Meetings", "Planning", "Work", "Administration", "Overtime")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dropDownOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        acTextViewTaskCategory.adapter = adapter
        acTextViewTaskCategory.setSelection(dropDownOptions.indexOf(selectedCategory))
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        saveToFirebase(createTrackModelFromInput())
        super.onDestroy()
    }

    private fun createTrackModelFromInput(): TrackModel {
        return TrackModel(
            id = intent.getStringExtra("SESSION_ID") ?: LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd:HH:mm:ss")),
            startDate = buttonDateStart.text.toString(),
            startTime = buttonTimeStart.text.toString(),
            endDate = buttonDateEnd.text.toString(),
            endTime = buttonTimeEnd.text.toString(),
            timeSpentWorking = realtimeTimer.text.toString(),
            taskDescription = editTextTaskDescription.text.toString(),
            taskCategory = acTextViewTaskCategory.selectedItem?.toString(),
            taskMedia = getCurrentImageBase64()
        )
    }

    private fun parseTimeToMillis(time: String): Long {
        val parts = time.split(":").map { it.toInt() }
        return (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
    }

    private fun getCurrentImageBase64(): String {
        val bitmap = (mediaViewer.drawable as? BitmapDrawable)?.bitmap
        if (bitmap != null) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }
        return "Empty"
    }

    private fun openDatePicker(dateSetListener: DatePickerDialog.OnDateSetListener) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun openTimePicker(timeSetListener: TimePickerDialog.OnTimeSetListener) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
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

    private fun setCurrentTimeOnButtons() {
        val currentTime = LocalDateTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val currentTimeFormatted = currentTime.format(timeFormatter)
        buttonTimeStart.text = currentTimeFormatted.toString()
        val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val currentDateFormatted = currentTime.format(dayFormatter)
        buttonDateStart.text = currentDateFormatted.toString()
    }

    // Save Data of Current Tracked Session to Firebase
    fun saveToFirebase(currentTrack: TrackModel) {
        val currentUser = authorisation.currentUser?.uid
        val key = currentTrack.id ?: return

        val startTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val endTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val endDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val startTimeCalendar = Calendar.getInstance().apply {
            time = startTimeFormatter.parse(buttonTimeStart.text.toString())
        }
        val startDateCalendar = Calendar.getInstance().apply {
            time = startDateFormatter.parse(buttonDateStart.text.toString())
        }

        val endTimeCalendar = Calendar.getInstance().apply {
            time = endTimeFormatter.parse(buttonTimeEnd.text.toString())
        }
        val endDateCalendar = Calendar.getInstance().apply {
            time = endDateFormatter.parse(buttonDateEnd.text.toString())
        }

        val timeSpentWorkingParts = realtimeTimer.text.toString().split(":")
        val hoursWorked = timeSpentWorkingParts[0].toInt()
        val minutesWorked = timeSpentWorkingParts[1].toInt()
        val secondsWorked = timeSpentWorkingParts[2].toInt()

        startTimeCalendar.add(Calendar.HOUR_OF_DAY, hoursWorked)
        startTimeCalendar.add(Calendar.MINUTE, minutesWorked)
        startTimeCalendar.add(Calendar.SECOND, secondsWorked)

        endDateCalendar.add(Calendar.HOUR_OF_DAY, hoursWorked)
        endDateCalendar.add(Calendar.MINUTE, minutesWorked)
        endDateCalendar.add(Calendar.SECOND, secondsWorked)

        val endTimeString = endTimeFormatter.format(startTimeCalendar.time)
        val endDateString = endDateFormatter.format(endDateCalendar.time)

        currentTrack.apply {
            startTime = buttonTimeStart.text.toString()
            startDate = buttonDateStart.text.toString()
            endTime = endTimeString
            endDate = endDateString
            timeSpentWorking = realtimeTimer.text.toString()
            taskCategory = acTextViewTaskCategory.selectedItem.toString()
            taskDescription = editTextTaskDescription.text.toString()
            if (taskMedia == null) taskMedia = "Empty"
        }

        database.child(currentUser.toString()).child("trackedSessions").child(key).setValue(currentTrack)
            .addOnSuccessListener {
                showToast("Successfully saved!")
            }
            .addOnFailureListener {
                showToast("Failed to save data! Please check your internet connection.")
            }
    }

    // Captures Image From Camera Or Allows Upload From Gallery
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

    private fun handleImageResult(image: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        currentTrack.taskMedia = base64Image
        mediaViewer.setImageBitmap(image)
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val GALLERY_REQUEST_CODE = 101
    }

    fun CaptureImage(view: View) {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
                2 -> { /* Cancel, do nothing */ }
            }
        }
        builder.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    private fun populateDropdown() {
        val dropDownOptions = arrayListOf("Client Projects", "Designing", "Meetings", "Planning", "Work", "Administration", "Overtime")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dropDownOptions)
        acTextViewTaskCategory.adapter = adapter
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}