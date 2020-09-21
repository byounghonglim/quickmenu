package com.byounghong.notiservicelib

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.byounghong.notiservicelib.model.NotiLayoutViewModel
import com.byounghong.notiservicelib.receivers.RestartServiceReceiver
import kotlinx.android.synthetic.main.noti_alert_service.view.*
import java.util.*


/**
 * #
 *
 * @auther : byounghonglim
 * @since : 25/04/2019
 */

class NotiService : Service(), Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private val NOTIFICATION_ID = 346
    private val NOTI_DURATION = 60
    private val ALERT_VIEW_WIDTH = 960
    private val ALERT_VIEW_HEIGHT = 132

    private var mWindowManager: WindowManager? = null
    private var mAlertView: View? = null
    private val mBinder = NotiServiceBinder()

    private val timer = Timer()
    private var timerTask:TimerTask? = null

    lateinit var viewDataBinding : ViewDataBinding
    private val alertLayoutModel = NotiLayoutViewModel()

    inner class NotiServiceBinder : Binder() {
        val service: NotiService
            get() = this@NotiService
    }

    val AUTHORITY = "com.byounghong.lim.personalsector"
    val SERVICE_TYPE_DELETE = "/DELETE"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        unregisterRestartAlarm()

        val bundle = intent?.extras

        bundle?.let {
            if(bundle.containsKey(Intent.EXTRA_STREAM)){
                alertLayoutModel.updateNotificationLayout(intent.getStringExtra("type"))
                viewDataBinding.setVariable(BR.title, intent.getStringExtra("title"))
                viewDataBinding.setVariable(BR.text, intent.getStringExtra("text"))
                viewDataBinding.setVariable(BR.status, intent.getStringExtra("status"))
                viewDataBinding.setVariable(BR.date, intent.getStringExtra("date"))
                viewDataBinding.setVariable(BR.time, intent.getStringExtra("time"))

                if(intent.getStringExtra("type") == "schedule" &&
                        intent.getStringExtra("time").isNullOrEmpty())
                    viewDataBinding.root.noti_schedule_time.visibility = GONE
                else
                    viewDataBinding.root.noti_schedule_time.visibility = VISIBLE

                viewDataBinding.invalidateAll()
                alertLayoutModel.notiID.postValue(intent.getIntExtra("notiId", -1))
                alertLayoutModel.agreementUri.postValue(intent.getStringExtra("url"))
                alertLayoutModel.agreementCode.postValue(intent.getStringExtra("agreement"))
                alertLayoutModel.isShowApp.postValue(intent.getBooleanExtra("isShow", true))

                RingtoneManager.getRingtone(applicationContext,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play()

                showNotification(BitmapFactory.decodeStream(contentResolver.openInputStream((
                        bundle.getParcelable(Intent.EXTRA_STREAM) as Uri))))
            }
        }

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(chan)
        return channelId
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun addAlertView() {
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        viewDataBinding = DataBindingUtil.inflate(LayoutInflater
                .from(this), R.layout.noti_alert_service, null, false)
        viewDataBinding.setVariable(BR.service, this)
        viewDataBinding.setVariable(BR.lm, alertLayoutModel)
        mAlertView = viewDataBinding.root

        val params = WindowManager.LayoutParams(dpToPx(ALERT_VIEW_WIDTH),
                dpToPx(ALERT_VIEW_HEIGHT),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        params.gravity = Gravity.TOP

        mWindowManager?.addView(mAlertView, params)
        closeNotification(false)
    }

    @SuppressLint("PrivateResource")
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + this.packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel(
                    "NotificationService","ForegroundServiceChannel")
            val build = NotificationCompat.Builder(this, channelId)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.abc_btn_radio_material)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build()
            startForeground(NOTIFICATION_ID, build)
        } else { }

        Thread.currentThread().uncaughtExceptionHandler = this
        unregisterRestartAlarm()
        addAlertView()
    }

    override fun onDestroy() {
        registerRestartAlarm()
        if (mAlertView != null) mWindowManager!!.removeView(mAlertView)
        super.onDestroy()
    }

    private fun registerRestartAlarm() {
        val intent = Intent(this, RestartServiceReceiver::class.java)
        intent.action = "android.intent.action.RESTART"
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                        1*100, PendingIntent.getBroadcast(this, 0, intent, 0))
    }

    fun unregisterRestartAlarm() {
        val intent = Intent(this, RestartServiceReceiver::class.java)
        intent.action = "android.intent.action.RESTART"
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .cancel(PendingIntent.getBroadcast(this, 0, intent, 0))
    }

    fun closeNotification(isClick:Boolean){
        stopCloseTimer()
        mAlertView?.visibility  = GONE
        if(isClick) {
            sendNotiIdToLauncher()
            alertLayoutModel.alertAppPackage.value?.let {
                if(it == SETTINGS) startSystemApp(it)
                if(it == AGREEMENT) startAgreementApp()
                else startApp(it)
            }

        }
    }

    private fun startSystemApp(packageName:String){
        startActivity(Intent(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun startAgreementApp(){
        startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse(alertLayoutModel.agreementUri.value))
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("agreement_code",
                        alertLayoutModel.agreementCode.value))
    }

    private fun startApp(packageName:String){
        startActivity(packageManager.getLaunchIntentForPackage(packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun sendNotiIdToLauncher(){
        val uri = Uri.parse("content://$AUTHORITY$SERVICE_TYPE_DELETE")
        val cr = contentResolver
        val cv = ContentValues()
        cv.put("id", alertLayoutModel.notiID.value)
        cr.update(uri, cv, null, null)
    }

    private fun showNotification(bitmap: Bitmap?){
        bitmap?.let{ (mAlertView?.noti_popup_img as ImageView).setImageBitmap(bitmap) }
        mAlertView?.visibility  = VISIBLE
        startCloseTimer()
    }

    private fun startCloseTimer(){
        stopCloseTimer()

        timerTask = object:TimerTask(){
            var count = NOTI_DURATION
            override fun run() {
                if(--count == 0)  {
                    val handler = Handler(applicationContext.mainLooper)
                    handler.post{ mAlertView?.visibility = GONE }
                }
            }
        }
        timer.schedule(timerTask, 0, 1000)
    }

    private fun stopCloseTimer(){
        timerTask?.let {
            it.cancel()
            timerTask = null
        }
    }

    private fun dpToPx(dp:Int):Int{
        val metrics = DisplayMetrics()
        mWindowManager?.defaultDisplay?.getMetrics(metrics)
        return (dp * metrics.density).toInt()
    }
}
