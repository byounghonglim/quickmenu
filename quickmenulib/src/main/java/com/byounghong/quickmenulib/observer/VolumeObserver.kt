package com.byounghong.quickmenulib.observer

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Message


/**
 * #
 * @auther : byounghonglim
 * @since : 2019-07-24
 */
class VolumeObserver(context: Context, private val handler: Handler) : ContentObserver(handler) {
    private val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

    override fun deliverSelfNotifications(): Boolean { return false }

    override fun onChange(selfChange: Boolean) {
        val msg = Message()
        msg.what = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        handler.sendMessage(msg)
    }
}