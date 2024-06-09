package com.raavi.clockwerkstime
data class TrackModel(
    var id: String? = null,
    var startTime: String? = null,
    var startDate: String? = null,
    var endTime: String? = null,
    var endDate : String? = null,
    var timeSpentWorking : String? = null,
    var taskDescription : String? = null,
    var taskCategory : String? = null,
    var taskMedia : String? = null
)