package com.byounghong.quickmenulib.model

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.byounghong.quickmenulib.consts.*
import com.byounghong.lim.music.IMusicRemoteService
import com.byounghong.lim.music.IMusicRemoteServiceCallback
import com.byounghong.lim.podcast.IPodcastRemoteCallback
import com.byounghong.lim.podcast.IPodcastRemoteService

/**
 * #
 * @auther : byounghonglim
 * @since : 2019-10-16
 */
class PlayerViewModel(val context:Context) : ViewModel(){
    var playerTitle
            = MutableLiveData<String>().apply { this.value = "" }
    var playerText
            = MutableLiveData<String>().apply { this.value = "" }
    var albumImagePath
            = MutableLiveData<String>().apply { this.value = "" }
    var isPlayerBtnVisible
            = MutableLiveData<Boolean>().apply { this.value = false }
    var isPlayMusic
            = MutableLiveData<Boolean>().apply { this.value = false }
    var isPlayPotcast
            = MutableLiveData<Boolean>().apply { this.value = false }
    var currentPlayApp
            = MutableLiveData<PLAY>().apply { this.value = PLAY.NONE }

    private lateinit var mMusicRemoteService: IMusicRemoteService
    private lateinit var mPodcastRemoteService: IPodcastRemoteService

    private var isInitedAnotherService = false

    enum class PLAY{
        NONE, MUSIC, PODCAST
    }

    fun addPlayerServices(){
        registerMusicService()
        registerPodcastService()
    }

    fun removePlayerServices(){
        removeMusicService()
        removePodcastService()
    }

    fun updateMusicStatus(){
        isPlayerBtnVisible.postValue(true)
        isPlayMusic.postValue( mMusicRemoteService.commandIsPlayingMusic())
    }

    fun updatePodcastStatus(){
        isPlayerBtnVisible.postValue(true)
        isPlayPotcast.postValue( mPodcastRemoteService.commandIsPlayingPodcast())
    }




    fun playMusic(){
        if(currentPlayApp.value == PLAY.MUSIC){
            if(isPlayMusic.value!!) mMusicRemoteService.commandTogglePlayMusic()
            else mMusicRemoteService.commandPlayMusic()
        } else if(currentPlayApp.value == PLAY.PODCAST){
            if(isPlayPotcast.value!!) mPodcastRemoteService.commandTogglePlayPodcast()
            else mPodcastRemoteService.commandPlayPodcast()
        }
    }

    fun nextMusic(){
        if(currentPlayApp.value == PLAY.MUSIC){
            mMusicRemoteService.commandNextMusic()
        } else if(currentPlayApp.value == PLAY.PODCAST){
            mPodcastRemoteService.commandNextPodcast()
        }
    }

    fun prevMusic(){
        if(currentPlayApp.value == PLAY.MUSIC){
            mMusicRemoteService.commandPreviousMusic()
        } else if(currentPlayApp.value == PLAY.PODCAST){
            mPodcastRemoteService.commandPreviousPodcast()
        }
    }


    /**
     * 서비스가 연결이 되면 UI를 업데이트 한다.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun onStartUI() {
        val lastMusicPlayedTime = mMusicRemoteService.commandGetLastPlayTime()
        val lastPodcastPlayedTime = mPodcastRemoteService.commandGetLastPlayTime()


        if(lastPodcastPlayedTime < 0 && lastMusicPlayedTime < 0){
            currentPlayApp.postValue(PLAY.NONE)
            isPlayerBtnVisible.postValue(false)
        } else if(lastPodcastPlayedTime > lastMusicPlayedTime ){
            currentPlayApp.postValue(PLAY.PODCAST)
            isPlayerBtnVisible.postValue(true)
            playerTitle.postValue(mPodcastRemoteService.commandGetPodcastName())
            playerText.postValue(mPodcastRemoteService.commandGetEpisodeName())
            albumImagePath.postValue(mPodcastRemoteService.commandGetPodcastImage())
            updatePodcastStatus()

        }else if(lastPodcastPlayedTime < lastMusicPlayedTime ){
            currentPlayApp.postValue(PLAY.MUSIC)
            isPlayerBtnVisible.postValue(true)
            playerTitle.postValue(mMusicRemoteService.commandGetSongName())
            playerText.postValue(mMusicRemoteService.commandGetSinger())
            albumImagePath.postValue(mMusicRemoteService.commandGetMusicImage())
            updateMusicStatus()

        }
    }

    /**
     * 서버에서 클라이언트로 이벤트 보낼 때 사용
     */
    private val mPodcastRemoteCallback = object : IPodcastRemoteCallback.Stub() {

        /**
         * 팟캐스트 재생 이벤트 발생 시 서비스에서 액티비티로 전달
         *
         * @param playWhenReady     플레이 준비 중
         * @param playbackState     이벤트 상태
         * @throws RemoteException
         */
        @Throws(RemoteException::class)
        override fun onPodcastEvent(playWhenReady: Boolean, playbackState: Int) {
            currentPlayApp.postValue(PLAY.PODCAST)
            updatePodcastStatus()
        }

        @Throws(RemoteException::class)
        override fun onChangePlayInfo(strPodcastTitle: String, strEpisodeTitle: String, strImagePath: String) {
            currentPlayApp.postValue(PLAY.PODCAST)
            playerTitle.postValue(strPodcastTitle)
            playerText.postValue(strEpisodeTitle)
            albumImagePath.postValue(strImagePath)
            isPlayerBtnVisible.postValue(true)
        }
    }

    /**
     * 서버에서 클라이언트로 이벤트를 보내려고 할때 사용을 한다.
     */
    private val mMusicRemoteServiceCallback
            = object : IMusicRemoteServiceCallback.Stub() {
        override fun onChangePlayInfo(strSongName: String?, strSinger: String?, strImagePath: String?) {
            currentPlayApp.postValue(PLAY.MUSIC)
            strSongName?.let { playerTitle.postValue(it) }
            strSinger?.let { playerText.postValue(it) }
            strImagePath?.let { albumImagePath.postValue(it) }
            isPlayerBtnVisible.postValue(true)
        }

        /**
         * 음악을 플레이 할 경우 이벤트 발생시 서비스에서 액티비티로 전달을 한다.
         *
         * @param playWhenReady 플레이 준비 중
         * @param playbackState 이벤트 상태
         * @throws RemoteException 예외처리 시
         */
        @Throws(RemoteException::class)
        override fun onMusicEvent(playWhenReady: Boolean, playbackState: Int) {
            currentPlayApp.postValue(PLAY.MUSIC)
            updateMusicStatus()
        }
    }

    private var mMusicServiceConn:ServiceConnection? = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            mMusicRemoteService.let {
                mMusicRemoteService.unregisterCallback(mMusicRemoteServiceCallback)
            }
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mMusicRemoteService = IMusicRemoteService.Stub.asInterface(service)

            try {
                // 서비스에 음악 콜백을 연결을 한다.
                mMusicRemoteService.registerCallback(mMusicRemoteServiceCallback)

                // 서비스가 연결이 되면 UI를 업데이트 한다.
                if(isInitedAnotherService) onStartUI()
                else isInitedAnotherService = true

            } catch (e: RemoteException) {

                e.printStackTrace()
            }

        }
    }
    private var mPodcastServiceConn: ServiceConnection? = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName) {
            mPodcastRemoteService?.let {
                mPodcastRemoteService.unregisterCallback(mPodcastRemoteCallback)
            }
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mPodcastRemoteService = IPodcastRemoteService.Stub.asInterface(service)

            try {
                // 서비스에 음악 콜백을 연결을 한다.mPodcastServiceConn
                mPodcastRemoteService.registerCallback(mPodcastRemoteCallback)

                // 서비스가 연결이 되면 UI를 업데이트 한다.
                //                mPodcastServiceConn.let { onStartUI() }
                if(isInitedAnotherService)
                    onStartUI()
                else
                    isInitedAnotherService = true
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    private fun registerMusicService() {
        val serviceIntent = Intent()
                .setComponent(ComponentName("com.byounghong.lim.music",
                        "com.byounghong.lim.music.service.music.player.MusicRemoteService"))
        // ACTION_INIT - 테스트를 위한 큐의 초기화을 함.
        serviceIntent.action = ACTION_INIT
        // android 8.0 이상부터 forground service를 사용하기 위함.
        //        applicationContext.startForegroundService(serviceIntent)
        context.bindService(serviceIntent, mMusicServiceConn, Context.BIND_AUTO_CREATE)
    }
    private fun registerPodcastService() {
        val serviceIntent = Intent()
                .setComponent(ComponentName("com.byounghong.lim.podcast",
                        "com.byounghong.lim.podcast.service.podcast.PodcastRemoteService"))
        // ACTION_INIT - 테스트를 위한 큐의 초기화을 함.
        serviceIntent.action = ACTION_PLAY
        // android 8.0 이상부터 forground service를 사용하기 위함.
        //        applicationContext.startForegroundService(serviceIntent)
        context.bindService(serviceIntent, mPodcastServiceConn, Context.BIND_AUTO_CREATE)
    }

    private fun removePodcastService(){
        if (mPodcastServiceConn != null) {
            mPodcastServiceConn = null
        }
        mPodcastServiceConn?.let{ context.unbindService(it) }
        mPodcastServiceConn = null
    }

    private fun removeMusicService(){
        if (mMusicServiceConn != null) {
            mMusicServiceConn = null
        }
        mMusicServiceConn?.let{ context.unbindService(it) }
        mMusicServiceConn = null
    }
}