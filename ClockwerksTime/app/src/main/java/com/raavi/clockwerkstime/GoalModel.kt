package com.raavi.clockwerkstime
//Class Defining Goals
data class GoalModel(
    val id: String? = null,
    val minGoalHours: Double = 0.0,
    val maxGoalHours: Double = 0.0,
    val goalReached: Boolean = false
)