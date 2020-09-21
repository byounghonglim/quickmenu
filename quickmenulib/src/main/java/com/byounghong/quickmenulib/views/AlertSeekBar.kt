package com.byounghong.quickmenulib.views

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import com.byounghong.quickmenulib.*
import com.byounghong.quickmenulib.observer.VolumeObserver
import kotlinx.android.synthetic.main.alert_brightness_seekbar.view.*
import kotlinx.android.synthetic.main.alert_sound_seekbar.view.*

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-10-17
 */
class AlertSeekBar {
    private var alertDialog:AlertDialog? = null
    private var isHwKey:Boolean = false

    private fun Context.controlHwVolumeKey(seekBar: SeekBar, dialog: AlertDialog) {
        val volumeObserver = VolumeObserver(this, Handler(Handler.Callback {
            isHwKey = true
            seekBar.progress = it.what
            true
        }))
        applicationContext.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)

        dialog.setOnDismissListener {
            applicationContext.contentResolver.unregisterContentObserver(volumeObserver)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun showAlertVolumeSeekBar(context: Context, viewGroup: ViewGroup) {
        val builder = AlertDialog.Builder(context)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val seekBarView = inflater.inflate(R.layout.alert_sound_seekbar, viewGroup, false)
        builder.setView(seekBarView)

        val seekBar: SeekBar = seekBarView.alert_volume_seek_bar
        seekBar.max = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        seekBar.progress = context.getVolume()
        if (seekBar.progress == 0) seekBarView.alert_seek_bar_sound_img.setImageDrawable(context.getDrawable(R.drawable.btn_sound_mute))
        seekBarView.alert_volume_seek_bar_txt.text = context.getVolume().toString()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(!isHwKey) context.setVolume(progress)
                isHwKey = false
                seekBarView.alert_volume_seek_bar_txt.text = progress.toString()
                if (progress == 0) seekBarView.alert_seek_bar_sound_img.setImageDrawable(context.getDrawable(R.drawable.btn_sound_mute))
                else seekBarView.alert_seek_bar_sound_img.setImageDrawable(context.getDrawable(R.drawable.btn_sound_alert))
            }
        })

        alertDialog = builder.create()
        alertDialog?.let {
            showDialog(it)
            context.controlHwVolumeKey(seekBar, it)
        }
    }

    fun hideSeekBar(){
        alertDialog?.let {
            it.dismiss()
            alertDialog = null
        }
    }

    fun showBrightnessSeekBar(context: Context, viewGroup: ViewGroup) {
        val builder = AlertDialog.Builder(context)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val seekBarView = inflater.inflate(R.layout.alert_brightness_seekbar, viewGroup, false)

        builder.setView(seekBarView)

        val seekBar: SeekBar = seekBarView.alert_brightness_seek_bar
        seekBar.max = 10
        seekBar.progress = context.getBrightValue()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                context.setBrightnessSetting(progress)
            }
        })
        alertDialog = builder.create()
        alertDialog?.let { showDialog(it) }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun showDialog(dialog: AlertDialog) {
        dialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}
