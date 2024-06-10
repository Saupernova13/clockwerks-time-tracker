package com.raavi.clockwerkstime.ui.home

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.ParseException
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.raavi.clockwerkstime.R
import com.raavi.clockwerkstime.Track
import com.raavi.clockwerkstime.TrackModel
import com.raavi.clockwerkstime.databinding.FragmentHomeBinding
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var button_StartDate: Button
    private lateinit var button_EndDate: Button
    private var startDate: Long? = null
    private var endDate: Long? = null
    private lateinit var sessionsContainer: LinearLayout
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance().getReference("users")
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var bar_Label: TextView
    private lateinit var pieChart_Label: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.textHome.visibility = View.VISIBLE
        binding.imageViewBG.visibility = View.VISIBLE
        button_StartDate = binding.buttonHomeStartDate
        button_EndDate = binding.buttonHomeEndDate
        barChart = binding.barChartHoursWorked
        pieChart = binding.pieChartTaskCategories
        pieChart_Label = binding.headingPieChart
        bar_Label = binding.headingBarChart

        button_StartDate.setOnClickListener { showDatePicker(true) }
        button_EndDate.setOnClickListener { showDatePicker(false) }

        if (user != null) {
            binding.textHome.text = "Welcome back ${user.email}!"
            binding.imageViewBG.setImageResource(R.drawable.loggedinscreen_bg_emptyspace)
            database.child(user.uid).child("trackedSessions").get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        hideViews()
                        sessionsContainer = binding.linearLayoutSessionsList
                        sessionsContainer.visibility = View.VISIBLE
                        binding.buttonHomeEndDate.visibility = View.VISIBLE
                        binding.buttonHomeStartDate.visibility = View.VISIBLE
                        barChart.visibility = View.VISIBLE
                        pieChart.visibility = View.VISIBLE
                        pieChart_Label.visibility = View.VISIBLE
                        bar_Label.visibility = View.VISIBLE
                        loadSessions()
                    } else {
                        binding.textHome.text = "No sessions found. Please close app to refresh after adding first session."
                    }
                }
                .addOnFailureListener {
                    showToast("Failed to load sessions.")
                }
        } else {
            binding.textHome.text = "No user data found. Please log in"
            binding.imageViewBG.setImageResource(R.drawable.notloggedin_screen)
        }
        return root
    }

    private fun loadSessions() {
        user?.let { user ->
            database.child(user.uid).child("trackedSessions")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        sessionsContainer.removeAllViews()
                        var totalSeconds = 0
                        val barEntries = mutableListOf<BarEntry>()
                        val pieEntries = mutableListOf<PieEntry>()
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val sessionsByDate = mutableMapOf<String, Int>()
                        val categoriesMap = mutableMapOf<String, Int>()
                        val dates = mutableListOf<String>()

                        for (sessionSnapshot in dataSnapshot.children) {
                            val trackModel = sessionSnapshot.getValue(TrackModel::class.java)
                            if (trackModel != null && trackModel.startDate != null) {
                                try {
                                    val sessionDate = dateFormat.parse(trackModel.startDate)?.time ?: continue
                                    if (startDate != null && endDate != null && sessionDate > endDate!!) {
                                        continue
                                    }
                                    if (sessionDate in (startDate ?: startDateDeafult().time) until (endDate ?: Long.MAX_VALUE)) {
                                        val dateStr = dateFormat.format(sessionDate)
                                        dates.add(dateStr)

                                        addRectangleView(trackModel)
                                        val timeSpentSec = parseTimeToSeconds(trackModel.timeSpentWorking ?: "00:00:00")
                                        sessionsByDate[dateStr] = sessionsByDate.getOrDefault(dateStr, 0) + timeSpentSec

                                        val category = trackModel.taskCategory ?: "Unknown"
                                        categoriesMap[category] = categoriesMap.getOrDefault(category, 0) + timeSpentSec
                                    }
                                } catch (e: ParseException) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        var index = 0
                        for ((dateStr, totalSecs) in sessionsByDate.toSortedMap(compareBy { dates.indexOf(it) })) {
                            val hours = totalSecs / 3600f
                            barEntries.add(BarEntry(index.toFloat(), hours))
                            index++
                        }

                        for ((category, totalSecs) in categoriesMap) {
                            val hours = totalSecs / 3600f
                            pieEntries.add(PieEntry(hours, category))
                        }
                        val barDataSet = BarDataSet(barEntries, "Hours Worked").apply {
                            color = ContextCompat.getColor(requireContext(), R.color.colourAccent)
                            valueTextSize = 12f
                        }
                        val barData = BarData(barDataSet)
                        barChart.data = barData
                        barChart.xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            valueFormatter = IndexAxisValueFormatter(dates)
                            granularity = 1f
                            isGranularityEnabled = true
                            textColor = ContextCompat.getColor(requireContext(), R.color.white)
                            textSize = 10f
                        }
                        barChart.axisLeft.apply {
                            axisMinimum = 0f
                            valueFormatter = barChart.axisLeft.valueFormatter
                            textColor = ContextCompat.getColor(requireContext(), R.color.white)
                            textSize = 10f
                        }
                        barChart.axisRight.isEnabled = false
                        barChart.axisLeft.textSize = 12f
                        barChart.description = Description().apply { text = "" }
                        barChart.invalidate()
                        barChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.white)
                        barChart.animateXY(200, 200)

                        val pieDataSet = PieDataSet(pieEntries, "").apply {
                            colors = listOf(
                                ContextCompat.getColor(requireContext(), R.color.bar1),
                                ContextCompat.getColor(requireContext(), R.color.bar2),
                                ContextCompat.getColor(requireContext(), R.color.bar3),
                                ContextCompat.getColor(requireContext(), R.color.bar4),
                                ContextCompat.getColor(requireContext(), R.color.bar5),
                                ContextCompat.getColor(requireContext(), R.color.bar6),
                                ContextCompat.getColor(requireContext(), R.color.bar7)
                            )
                            //valueTextSize = 14f
                            valueTextSize = 0f
                            valueTextColor = Color.TRANSPARENT
                            //valueTextColor = ContextCompat.getColor(requireContext(), R.color.white)
                        }
                        val pieData = PieData(pieDataSet)
                        pieChart.data = pieData
                        pieChart.data.setValueTextColor(Color.TRANSPARENT)
                        pieChart.description = Description().apply { text = "" }
                        pieChart.setHoleColor(Color.TRANSPARENT)
                        pieChart.holeRadius = 50f
                        pieChart.transparentCircleRadius = 55f
                        pieChart.setCenterTextSize(18f)
                        pieChart.setUsePercentValues(true)
                        pieChart.invalidate()
                        pieChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.white)
                        pieChart.animateXY(200, 200)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        showToast("Error loading data!")
                    }
                })
        }
    }

    private fun startDateDeafult(): Date {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse("01/01/1970")
    }

    private fun hideViews() {
        val fadeOutAnimation = AnimationUtils.loadAnimation(context, R.anim.anime_fade_out)
        val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.anime_fade_in)
        binding.textHome.startAnimation(fadeOutAnimation)
        binding.imageViewBG.startAnimation(fadeOutAnimation)
        binding.buttonHomeEndDate.startAnimation(fadeInAnimation)
        binding.buttonHomeStartDate.startAnimation(fadeInAnimation)
        fadeInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                binding.buttonHomeEndDate.visibility = View.VISIBLE
                binding.buttonHomeStartDate.visibility = View.VISIBLE
            }
        })
        fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                binding.textHome.text="Filter Date Range:"
                binding.imageViewBG.visibility = View.GONE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addRectangleView(trackModel: TrackModel?) {
        val cardView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply {
                setMargins(10, 20, 10, 10)
            }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_rectangle)
            setPadding(20, 20, 20, 20)
        }

        val textInfoContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
        }

        val timeInfoContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 10, 10)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val startTimeView = TextView(context).apply {
            text = "Started: ${trackModel?.startTime}\n${trackModel?.startDate}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.bananaBrown))
        }
        val endTimeView = TextView(context).apply {
            text = "Ended: ${trackModel?.endTime}\n${trackModel?.endDate}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.bananaBrown))
        }
        val timeSpentView = TextView(context).apply {
            text = "Time worked: ${trackModel?.timeSpentWorking}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.bananaBrown))
        }
        val categoryView = TextView(context).apply {
            text = "Category: ${trackModel?.taskCategory}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.bananaBrown))
            setPadding(0, 10, 0, 0)
        }
        val descriptionView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = trackModel?.taskDescription ?: "No Description"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.bananaBrown))
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

        val mediaView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(225, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(10, 10, 10, 10)
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
        sessionsContainer.setPadding(20, 10, 20, 10)

        cardView.setOnClickListener {
            val intent = Intent(context, Track::class.java).apply {
                putExtra("SESSION_ID", trackModel?.id)
                putExtra("START_TIME", trackModel?.startTime)
                putExtra("START_DATE", trackModel?.startDate)
                putExtra("END_TIME", trackModel?.endTime)
                putExtra("END_DATE", trackModel?.endDate)
                putExtra("TIME_SPENT_WORKING", trackModel?.timeSpentWorking)
                putExtra("TASK_DESCRIPTION", trackModel?.taskDescription)
                putExtra("TASK_CATEGORY", trackModel?.taskCategory)
                putExtra("TASK_MEDIA", trackModel?.taskMedia)
            }
            startActivity(intent)
        }
    }

    private fun showDatePicker(start: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            val selectedDate = calendar.timeInMillis
            if (start) {
                startDate = selectedDate
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                button_StartDate.text = dateFormat.format(Date(selectedDate))
            } else {
                endDate = selectedDate
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                button_EndDate.text = dateFormat.format(Date(selectedDate))
            }
            loadSessions()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun parseTimeToSeconds(time: String): Int {
        val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
        return parts[0] * 3600 + parts[1] * 60 + parts[2]
    }
}