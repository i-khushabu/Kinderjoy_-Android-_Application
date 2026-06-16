package com.yourname.kidslearning

import android.app.ActivityManager
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.firebase.database.FirebaseDatabase

object AppSessionManager {
    private var countdownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var remainingMillis: Long = 0
    private var appContext: Context? = null

    private const val DB_URL = "https://kinderjoy-372c6-default-rtdb.asia-southeast1.firebasedatabase.app"

    var remainingTimeText by mutableStateOf("")

    fun init(context: Context, totalMinutes: Int) {
        if (isTimerRunning) return  // Don't start again if already running

        appContext = context.applicationContext
        isTimerRunning = true
        remainingMillis = totalMinutes * 60 * 1000L

        countdownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                remainingTimeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                Log.d("DEBUG", "Timer finished. Locking and closing app.")
                isTimerRunning = false
                remainingTimeText = "00:00"

                val lockUntil = System.currentTimeMillis() + (5 * 60 * 60 * 1000) // 5 hours lock
                FirebaseDatabase.getInstance(DB_URL)
                    .getReference("ScreenLock/lockUntil")
                    .setValue(lockUntil)

                // 🚫 Close the app
                appContext?.let {
                    val am = it.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    am.appTasks.forEach { task -> task.finishAndRemoveTask() }
                }
            }

        }.start()
    }

    fun getSessionDurationInMinutes(): Int {
        return ((remainingMillis / 1000) / 60).toInt()
    }

    fun cancelSession() {
        countdownTimer?.cancel()
        countdownTimer = null
        isTimerRunning = false
        remainingTimeText = ""
    }
}
