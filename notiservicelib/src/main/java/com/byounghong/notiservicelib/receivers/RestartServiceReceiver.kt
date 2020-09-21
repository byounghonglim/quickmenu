package com.byounghong.notiservicelib.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat.startForegroundService
import com.byounghong.notiservicelib.NotiService

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-07-11
 */
class RestartServiceReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent?) {
        val newIntent = Intent(context, NotiService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(context, newIntent)
        } else{
            context.startService(newIntent)
        }
    }

}