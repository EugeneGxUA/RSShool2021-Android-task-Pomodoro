package com.egaragul.pomodoro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.egaragul.pomodoro.adapters.TimerAdapter
import com.egaragul.pomodoro.data.Timer
import com.egaragul.pomodoro.databinding.ActivityMainBinding
import com.egaragul.pomodoro.services.ForegroundService
import com.egaragul.pomodoro.utils.COMMAND_ID
import com.egaragul.pomodoro.utils.COMMAND_START
import com.egaragul.pomodoro.utils.COMMAND_STOP
import com.egaragul.pomodoro.utils.STARTED_TIMER_TIME_MS
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity(R.layout.activity_main), TimerControlListener {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = requireNotNull(_binding)

    private val json = Json { encodeDefaults = true }

    private val timerAdapter = TimerAdapter(
        this
    )

    private val timers = mutableListOf<Timer>()
    private var nextTimerId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            savedInstanceState.getString("timers")?.let {
                val restoredTimers = json.decodeFromString<List<Timer>>(it)
                timers.addAll(restoredTimers)
            }
        }

        provideAdapter()
        provideButtonClickListener()

    }

    private fun provideAdapter() {
        binding.rvTimers.adapter = timerAdapter
    }

    private fun provideButtonClickListener() {
        binding.btnAddTimer.setOnClickListener {
            val minutes = binding.etMinutes.text.toString().toLongOrNull()
            if (minutes == null || minutes <= 0) {
                Toast.makeText(this, "Wow... Easy... Check minutes field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ms: Long = minutes * 1000 * 60
            if (ms > 1000L * 60L * 60L * 48L) {
                Toast.makeText(
                    this,
                    "Wow... Easy... I don't think you need timer more than 2 days use calendar...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            timers.add(
                Timer(
                    id = nextTimerId++,
                    startTime = ms,
                    currentMs = ms,
                    isStarted = false
                )
            )

            timerAdapter.submitList(timers.toList())

            binding.etMinutes.setText("")
        }
    }

    private fun startTimer(id: Int) {
        controlTimers(id, true)
    }

    private fun stopTimer(id: Int) {
        controlTimers(id, false)
    }

    private fun controlTimers(id: Int, isStarted : Boolean) {
        timers.forEach {
            if (it.id == id) {
                it.isStarted = isStarted
            } else {
                it.isStarted = false
            }
        }

        timerAdapter.submitList(timers.toList())
    }

    override fun start(id: Int) {
        controlTimers(id, true)
    }

    override fun stop(id: Int) {
        controlTimers(id, false)
    }

    override fun delete(id: Int) {
        binding.rvTimers.findViewHolderForItemId(id.toLong())?.let { holder ->
            (holder as? TimerAdapter.TimerViewHolder)?.removeTimer()
        }
        timers.remove(timers.find { it.id == id })
        timerAdapter.submitList(timers.toList())
    }

    override fun onStop() {
        super.onStop()
        timers.find { it.isStarted }?.let { timer ->
            val startServiceIntent = Intent(this, ForegroundService::class.java)
            startServiceIntent.putExtra(STARTED_TIMER_TIME_MS, timer.currentMs)
            startServiceIntent.putExtra(COMMAND_ID, COMMAND_START)
            startService(startServiceIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        val stopServiceIntent = Intent(this, ForegroundService::class.java)
        stopServiceIntent.putExtra(COMMAND_ID, COMMAND_STOP)
        startService(stopServiceIntent)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("timers", json.encodeToString(timers))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getString("timers")?.let {
            val restoredTimers = json.decodeFromString<List<Timer>>(it)
            timers.addAll(restoredTimers)
        }
    }

}