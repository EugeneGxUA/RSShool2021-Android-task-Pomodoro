package com.egaragul.pomodoro.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Timer(
    @SerialName("id")
    val id : Int,
    @SerialName("globalTime")
    var globalTime : Long = 0L,
    @SerialName("startTime")
    val startTime : Long,
    @SerialName("currentMs")
    var currentMs : Long,
    @SerialName("isStarted")
    var isStarted : Boolean
)