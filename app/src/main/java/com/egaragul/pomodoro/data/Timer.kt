package com.egaragul.pomodoro.data

data class Timer(
    val id : Int,
    var globalTime : Long = 0L,
    val startTime : Long,
    var currentMs : Long,
    var isStarted : Boolean
)