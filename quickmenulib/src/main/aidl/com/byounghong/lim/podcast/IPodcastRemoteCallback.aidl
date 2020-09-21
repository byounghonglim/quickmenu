package com.byounghong.lim.podcast;

/**
 * IPodcastRemoteCallback 서비스에서 이벤트를 클라이언트로 보낸다.
 * one-way - 서버는 클라이언트를 위한 블럭하여 기다리지 않는다. (async)
 */
interface IPodcastRemoteCallback {

    // 팟캐스트 이벤트를 클라이언트로 전달을 한다.
    void onPodcastEvent(boolean playWhenReady, int playbackState);

    // 방송 이름, 에피소드 이름, 이미지 정보가 변경이 되었을 경우
    void onChangePlayInfo(String strPodcastTitle, String strEpisodeTitle, String strImagePath);
}