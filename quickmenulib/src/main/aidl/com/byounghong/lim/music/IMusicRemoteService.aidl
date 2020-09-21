package com.byounghong.lim.music;

import com.byounghong.lim.music.IMusicRemoteServiceCallback;

 interface IMusicRemoteService {

    // 이벤트 콜백을 등록하기 위함.
	boolean registerCallback(IMusicRemoteServiceCallback callback);

    // 이벤트 콜백을 해하기 위함.
	boolean unregisterCallback(IMusicRemoteServiceCallback callback);

    // 멈춘 음악을 재생을 한다.
    int commandPlayMusic();

    // 재생중인 음악을 일시 중지/다시 시작을 한다.
    int commandTogglePlayMusic();

    // 이전 음악을 재생을 한다.
    int commandPreviousMusic();

    // 다음 음악을 재생을 한다.
    int commandNextMusic();

    // 현재 음악이 재생중인지 체크를 한다.
    boolean commandIsPlayingMusic();

    // 음악의 이름 - 음악 이름
    String commandGetSongName();

    // 음악의 이름 - 가수 이름
    String commandGetSinger();

    // 플레이의 이미지
    String commandGetMusicImage();

    // 마지막 재생 시간
    long commandGetLastPlayTime();

}
