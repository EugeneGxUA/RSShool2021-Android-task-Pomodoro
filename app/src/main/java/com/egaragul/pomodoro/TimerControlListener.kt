package com.egaragul.pomodoro

interface TimerControlListener {

    fun start(id : Int)

    fun stop(id : Int)

    fun delete(id : Int)
}