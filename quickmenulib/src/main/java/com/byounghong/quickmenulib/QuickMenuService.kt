package com.byounghong.quickmenulib

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleService
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.byounghong.quickmenulib.consts.*
import com.byounghong.quickmenulib.model.PlayerViewModel
import com.byounghong.quickmenulib.model.PlayerViewModel.*
import com.byounghong.quickmenulib.model.QuickMenuViewModel
import com.byounghong.quickmenulib.views.AlertSeekBar
import com.byounghong.quickmenulib.views.PressImageView
import com.byounghong.quickmenulib.views.QuickMenuLayout
import kotlinx.android.synthetic.main.quick_menu_service.view.*


/**
 * #
 *
 * @auther : byounghonglim
 * @since : 25/04/2019
 */

class QuickMenuService : LifecycleService(), Thread.UncaughtExceptionHandler{
    private val playerViewModel = PlayerViewModel(this)
    private val quickMenuViewModel = QuickMenuViewModel()
    private val seekBar = AlertSeekBar()
    lateinit var viewDataBinding:ViewDataBinding

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        Process.killProcess(Process.myPid())
    }

    enum class BarSize{
        MIN_SIZE, MAX_SIZE
    }

    private var mWindowManager: WindowManager? = null
    private var mFloatingView: View? = null
    var mQuickMenuLayout: QuickMenuLayout? = null
    private val mBinder = SystemUiServiceBinder()
    private lateinit var mSleepModeBtn:ImageView
    private lateinit var mDisturbModeBtn:ImageView

    inner class SystemUiServiceBinder : Binder() {
        val service: QuickMenuService
            get() = this@QuickMenuService
    }

    private val mContentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            isSleepMode().let { mSleepModeBtn.isSelected = it }
        }
    }

    fun clickDisturb() {
        updateDisturbModeBtn(setDisturb())
    }

    fun startYouTube(){
        //TODO.
        quickMenuViewModel.isCollapse.value = true
    }

    @SuppressLint("MissingSuperCall")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isCategory = intent?.categories?.contains("agent")
        isCategory?.let {
            if(isCategory) quickMenuViewModel.isCollapse.value = true
            seekBar.let { seekBar.hideSeekBar() }

        }
//            mQuickMenuLayout?.childViewState = QuickMenuLayout.ChildViewState.COLLAPSED

        return START_STICKY
        //        return super.onStartCommand(intent, flags, startId)
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

    @SuppressLint("MissingSuperCall")
    override fun onBind(intent: Intent): IBinder? {
        mQuickMenuLayout?.alpha = 0f
        return mBinder
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun updateFloatingViewLayout(size: Int) {
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                if (size == BarSize.MIN_SIZE.ordinal)
                    WindowManager.LayoutParams.WRAP_CONTENT
                else
                    WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        params.gravity = Gravity.TOP
        mWindowManager?.updateViewLayout(mFloatingView, params)

        isSleepMode().let { mSleepModeBtn.isSelected = it }
        isDisturb().let { mDisturbModeBtn.isSelected = it }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun initQuickMenuView() {
        mSleepModeBtn = mFloatingView!!.sleep_mode_btn
        mDisturbModeBtn = mFloatingView!!.disturb_mode_btn

        updateDisturbModeBtn(isDisturb())
//        updateSleepModeBtn()
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)

        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        mWindowManager?.addView(mFloatingView, params)

        mQuickMenuLayout = mFloatingView?.findViewById(R.id.quickmenu_layout)
        mQuickMenuLayout?.quickMenuViewModel = quickMenuViewModel
        mQuickMenuLayout?.addDragListener(object : QuickMenuLayout.DragListener {
            override fun onStateChanged(panel: View, previousState: QuickMenuLayout.ChildViewState, newState: QuickMenuLayout.ChildViewState) {
                if (newState == QuickMenuLayout.ChildViewState.COLLAPSED) {
                    mQuickMenuLayout!!.visibility = View.GONE
                    updateFloatingViewLayout(BarSize.MIN_SIZE.ordinal)
                }
            }
        })

        mFloatingView!!.findViewById<View>(R.id.fake_layout)
                .setOnTouchListener { v, event ->
                    if(isDeviceInit()) {
                        playerViewModel.onStartUI()
                        viewDataBinding.invalidateAll()
                        updateFloatingViewLayout(BarSize.MAX_SIZE.ordinal)
                        mQuickMenuLayout?.let {
                            it.visibility = View.VISIBLE
                            when (event.action) {
                                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                                    if ((event.x <= v.width && event.y <= v.height)) {
                                        it.visibility = View.GONE
                                        updateFloatingViewLayout(BarSize.MIN_SIZE.ordinal)
                                    } else {
                                        if (!it.onTouchEvent(event)) {
                                            it.visibility = View.GONE
                                            updateFloatingViewLayout(BarSize.MIN_SIZE.ordinal)
                                        }
                                    }
                                }
                                else -> it.onTouchEvent(event)
                            }
                            true
                        }
                    }
                    true
                }

//        mQuickMenuLayout!!.visibility = View.GONE
        updateFloatingViewLayout(BarSize.MAX_SIZE.ordinal)

        if(BuildConfig.DEBUG){
            mFloatingView!!.quick_menu_home_btn.setOnLongClickListener {
                Toast.makeText(this, BuildConfig.VERSION_NAME, Toast.LENGTH_LONG).show()
                true
            }
        }
    }

    fun startSystemApp(packageName:String){
//        startActivity(Intent(packageName)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun startApp(packageName:String){
//        startActivity(packageManager.getLaunchIntentForPackage(packageName)!!
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun clickTalkingGuide(){
//        val intent = Intent(Intent.ACTION_VIEW)
//        intent.data = Uri.parse(TALKING_GUIDE)
//        startActivity(intent)
    }

    fun clickSleepMode(){
        setSleepMode()
        quickMenuViewModel.isCollapse.value = true
//        mQuickMenuLayout?.childViewState = QuickMenuLayout.ChildViewState.COLLAPSED
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        quickMenuViewModel.isCollapse.value = true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("PrivateResource")
    override fun onCreate() {
        super.onCreate()
        Thread.currentThread().uncaughtExceptionHandler = this

        setNotificationChannel()

        initDataBinding()
        initQuickMenuView()
        initPlayerViewModel()

        unregisterRestartService()
    }

    private fun setNotificationChannel(){
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
                    "QuickMenuService","ForegroundServiceChannel")
            val build = NotificationCompat.Builder(this, channelId)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.abc_btn_radio_material)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build()
            startForeground(NOTIFICATION_ID, build)
        }
    }

    private fun initDataBinding(){
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        viewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(LayoutInflater.from(this),
                R.layout.quick_menu_service, null, false)
        viewDataBinding.setVariable(BR.service, this)
        viewDataBinding.setVariable(BR.quickMenuViewModel, quickMenuViewModel)
        viewDataBinding.setVariable(BR.playerViewModel, playerViewModel)
        viewDataBinding.setVariable(BR.seekbar, seekBar)

        mFloatingView = viewDataBinding.root
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initPlayerViewModel(){
        playerViewModel.addPlayerServices()

        playerViewModel.isPlayMusic.observeForever {
            updatePlayBtnImage(it) }

        playerViewModel.currentPlayApp.observeForever {
            if (it == PLAY.NONE) {
                mFloatingView!!.music_album.setImageDrawable(getDrawable(R.drawable.ic_music_quick_empty_default))
            }
        }

        playerViewModel.albumImagePath.observeForever {
            val requestOptions = RequestOptions()
            requestOptions.placeholder(android.R.color.transparent).transform(CenterCrop())
                    .transform(RoundedCorners(150))

            when(playerViewModel.currentPlayApp.value){
                PLAY.MUSIC   ->
                    Glide.with(this).load(it).diskCacheStrategy(DiskCacheStrategy.ALL)
                            .apply(requestOptions).into(mFloatingView!!.music_album)
                PLAY.PODCAST ->
                    Glide.with(this).load(it).diskCacheStrategy(DiskCacheStrategy.ALL)
                            .apply(requestOptions).into(mFloatingView!!.music_album)
                else         ->
                    mFloatingView!!.music_album
                            .setImageDrawable(getDrawable(R.drawable.ic_music_quick_empty_default))
            }
        }

        playerViewModel.isPlayPotcast.observeForever { updatePlayBtnImage(it) }

        playerViewModel.isPlayerBtnVisible.observeForever{ viewDataBinding?.invalidateAll() }
    }

    private fun updatePlayBtnImage(isVisible: Boolean){
        val resourceId: () -> Int = {
            if(isVisible) R.drawable.ic_music_quick_pause
            else  R.drawable.ic_music_quick_play
        }

        mFloatingView?.findViewById<PressImageView>(R.id.music_play_btn)
                ?.setImageResource(resourceId())
    }

    private fun updateSleepModeBtn(){
        contentResolver.registerContentObserver(SLEEP_MODE_URI, true, mContentObserver)
        isSleepMode().let { mSleepModeBtn.isSelected = it }
    }

    private fun updateDisturbModeBtn(isDisturb: Boolean){
        mDisturbModeBtn.isSelected = isDisturb
    }


    override fun onDestroy() {
        if (mFloatingView != null) mWindowManager!!.removeView(mFloatingView)
        //        sendBroadcast(Intent(this, RestartServiceReceiver::class.java))
        registerRestartService()
        // 음악 서비스를 해제 한다.
        playerViewModel.removePlayerServices()
        contentResolver.unregisterContentObserver(mContentObserver)
        super.onDestroy()
    }
}
