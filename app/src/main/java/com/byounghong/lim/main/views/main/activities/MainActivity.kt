package com.byounghong.lim.main.views.main.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.byounghong.notiservicelib.NotiService
import com.byounghong.quickmenulib.QuickMenuService

/**
 * #
 * @auther : byounghonglim
 * @since : 30/04/2019
 */
class MainActivity : AppCompatActivity() {
    companion object{
        const val SYSTEM_ALERT_WINDOW_PERMISSION = 2084
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) askPermission()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val notiAlertIntent = Intent(this, NotiService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(notiAlertIntent)
            else startService(notiAlertIntent)

            val intent = Intent(this, QuickMenuService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)


        } else if (Settings.canDrawOverlays(this)) {
            val notiAlertIntent = Intent(this, NotiService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(notiAlertIntent)
            else startService(notiAlertIntent)

            val intent = Intent(this, QuickMenuService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } else { askPermission() }
        finish()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun askPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
        startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION)
    }
}