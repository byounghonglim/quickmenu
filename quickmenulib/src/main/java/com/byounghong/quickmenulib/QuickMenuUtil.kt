package com.byounghong.quickmenulib

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.byounghong.quickmenulib.consts.SLEEP_MODE_URI
import com.byounghong.quickmenulib.consts.KEY_SLEEP_MODE


/**
 * #
 * @auther : byounghonglim
 * @since : 2019-09-16
 */

fun Context.getBrightness(): Int {
    val cResolver = applicationContext.contentResolver
    return Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
}

fun Context.setBrightness(brightness: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (Settings.System.canWrite(this)) {
            val cResolver = applicationContext.contentResolver
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}

fun Context.setVolume(progress: Int) {
    val audioManager = getSystemService(Service.AUDIO_SERVICE) as AudioManager
    audioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME, progress, AudioManager.FLAG_SHOW_UI)
    audioManager.setStreamVolume(STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND)
}


fun Context.getVolume(): Int {
    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    return audioManager.getStreamVolume(STREAM_MUSIC)
}

fun Context.isSleepMode(): Boolean {
    val cursor = contentResolver.run {
        query(SLEEP_MODE_URI, null, null, null, null) }
    cursor?.let {
        if (it.moveToNext()){
            val isStatus =  it.getString(it.getColumnIndexOrThrow(KEY_SLEEP_MODE)) == "on"
            it.close()
            return isStatus
        }
        it.close()
    }
    return false
}

fun Context.setSleepMode(){
    var modeStatus = "off"
    if(!isSleepMode()){ modeStatus = "on" }
    val values = ContentValues()
    values.put(KEY_SLEEP_MODE, modeStatus)
    contentResolver.update(SLEEP_MODE_URI, values, null, null)
    //    mQuickMenuLayout?.childViewState = QuickMenuLayout.ChildViewState.COLLAPSED
}

@SuppressLint("PrivateApi")
fun Context.isDisturb() = "true" ==
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java, String::class.java)
                .invoke(this, "sys.silent.mode", null) as String

@SuppressLint("PrivateApi")
fun Context.isDeviceInit(): Boolean {
    var isDeviceInit = false
    val projection = arrayOf("value")
    val selection = arrayOf("init_setup_done")
    val cursor =
            contentResolver.query(Uri.parse("content://com.byounghong.lim.settings/local_content")
                    ,projection, "key=?", selection, null)
    cursor?.let {
        if(cursor.moveToNext())
            isDeviceInit = cursor.getString(cursor.getColumnIndex("value")) == "true" }
    cursor?.close()
    return isDeviceInit
}

@SuppressLint("PrivateApi")
fun Context.getBrightValue(): Int {
    var brightValue = "0"
    val projection = arrayOf("value")
    val selection = arrayOf("device_brightness")
    val cursor =
            contentResolver.query(Uri.parse("content://com.byounghong.lim.settings/local_content")
                    ,projection, "key=?", selection, null)
    cursor?.let {
        if(cursor.moveToNext())
            brightValue = cursor.getString(cursor.getColumnIndex("value")) }
    cursor?.close()
    return brightValue.toInt()
}


@SuppressLint("PrivateApi")
fun Context.setBrightnessSetting(value: Int) {
    val uri = Uri.parse("content://com.byounghong.lim.settings/local_content")
    val cv = ContentValues()
    cv.put("key", "device_brightness")
    cv.put("value", value.toString())
    contentResolver.update(uri, cv, null, null)
}


fun Context.setDisturb():Boolean{
    val isDisturb = isDisturb()
    sendBroadcast(Intent("charbyounghong.intent.action.SILENT_MODE")
            .putExtra("charbyounghong.intent.extra.SILENT_MODE", !isDisturb))
    return !isDisturb
}

fun Context.registerRestartService() {
    val intent = Intent(this, com.byounghong.quickmenulib.receiver.RestartServiceReceiver::class.java)
    intent.action = "android.intent.action.RESTART"
    (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                    1*100, PendingIntent.getBroadcast(this, 0, intent, 0))
}

fun Context.unregisterRestartService() {
    val intent = Intent(this, com.byounghong.quickmenulib.receiver.RestartServiceReceiver::class.java)
    intent.action = "android.intent.action.RESTART"
    (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(PendingIntent.getBroadcast(this, 0, intent, 0))
}



