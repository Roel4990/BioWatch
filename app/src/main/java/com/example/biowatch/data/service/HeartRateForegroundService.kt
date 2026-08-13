package com.example.biowatch.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.example.biowatch.R
import com.example.biowatch.data.datasource.HealthDataSource
import com.example.biowatch.data.analysis.ContinuousAnalysisManager
import com.example.biowatch.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HeartRateForegroundService : Service() {

    @Inject
    lateinit var healthDataSource: HealthDataSource

    @Inject
    lateinit var continuousAnalysisManager: ContinuousAnalysisManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        )
        Log.i(TAG, "Heart rate foreground service started")
        healthDataSource.connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START_CONTINUOUS_ANALYSIS -> {
                Log.i(TAG, "Continuous analysis start action received")
                continuousAnalysisManager.start()
                START_REDELIVER_INTENT
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stop action received")
                stopSelf()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        continuousAnalysisManager.stop()
        healthDataSource.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "Heart rate foreground service stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.health_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.health_notification_channel_description)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            OPEN_APP_REQUEST_CODE,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopServiceIntent = PendingIntent.getService(
            this,
            STOP_SERVICE_REQUEST_CODE,
            Intent(this, HeartRateForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = Notification.Action.Builder(
            null,
            getString(R.string.stop_measurement),
            stopServiceIntent
        ).build()

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.splash_icon)
            .setContentTitle(getString(R.string.health_notification_title))
            .setContentText(getString(R.string.health_notification_content))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(stopAction)
            .build()
    }

    companion object {
        private const val TAG = "HeartRateService"
        private const val NOTIFICATION_CHANNEL_ID = "heart_rate_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val OPEN_APP_REQUEST_CODE = 100
        private const val STOP_SERVICE_REQUEST_CODE = 101
        private const val ACTION_START = "com.example.biowatch.action.START_HEART_RATE"
        private const val ACTION_START_CONTINUOUS_ANALYSIS =
            "com.example.biowatch.action.START_CONTINUOUS_ANALYSIS"
        private const val ACTION_STOP = "com.example.biowatch.action.STOP_HEART_RATE"

        fun start(context: Context) {
            val intent = Intent(context, HeartRateForegroundService::class.java)
                .setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun startContinuousAnalysis(context: Context) {
            val intent = Intent(context, HeartRateForegroundService::class.java)
                .setAction(ACTION_START_CONTINUOUS_ANALYSIS)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HeartRateForegroundService::class.java))
        }
    }
}
