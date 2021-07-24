package com.egaragul.pomodoro.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.egaragul.pomodoro.MainActivity
import com.egaragul.pomodoro.R
import com.egaragul.pomodoro.utils.*
import kotlinx.coroutines.*

//https://ziginsider.github.io/Foreground-Service/

class ForegroundService : Service() {

    private companion object {

        private const val CHANNEL_ID = "channel_id_1"
        private const val NOTIFICATION_ID = 111
        private const val INTERVAL = 1000L
    }

    private var isServiceStarted = false
    private var notificationManager: NotificationManager? = null
    private var job: Job? = null
    private var countDownTimer : CountDownTimer? = null

    private val builder by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro Timer")
            .setGroup("Pomodoro Timer")
            .setGroupSummary(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setSilent(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        processCommand(intent)

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun processCommand(intent: Intent?) {
        when(intent?.extras?.getString(COMMAND_ID) ?: INVALID) {
            COMMAND_START -> {
                val startTime = intent?.extras?.getLong(STARTED_TIMER_TIME_MS) ?: return
                commandStart(startTime)
            }
            COMMAND_STOP -> {
                commandStop()
            }
            INVALID -> return
        }
    }

    private fun commandStart(startTime : Long) {
        if (isServiceStarted) {
            return
        }

        try {
            moveToStartedState()
            startForegroundServiceAndShowNotification()
            continueTimer(startTime)

        } finally {
            isServiceStarted = true
        }
    }

    private fun continueTimer(startTime: Long) {
//        job = CoroutineScope(Dispatchers.Default).launch {
        job = GlobalScope.launch(Dispatchers.Main) {
            var time = startTime
            countDownTimer = object : CountDownTimer(startTime, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    time -= 1000L
                    notificationManager?.notify(
                        NOTIFICATION_ID,
                        getNotification(
                            time.displayTime()
                        )
                    )
                }

                override fun onFinish() {
                    builder.setSilent(false)
                    notificationManager?.notify(NOTIFICATION_ID, getNotification("Timer is end"))
                }

            }

            countDownTimer?.start()
        }
    }

    private fun commandStop() {
        if (!isServiceStarted) {
            return
        }

        try {
            countDownTimer?.cancel()
            job?.cancel()
            stopForeground(true)
            stopSelf()
        } finally {
            isServiceStarted = false
        }
    }

    private fun moveToStartedState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, ForegroundService::class.java))
        } else {
            startService(Intent(this, ForegroundService::class.java))
        }
    }

    private fun startForegroundServiceAndShowNotification() {
        createChannel()
        val notification = getNotification("content")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getNotification(content: String) = builder.setContentText(content).build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "pomodoro"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                "channel_id_1", channelName, importance
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun getPendingIntent() : PendingIntent? {
        val resultIntent = Intent(this, MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT)
    }


}