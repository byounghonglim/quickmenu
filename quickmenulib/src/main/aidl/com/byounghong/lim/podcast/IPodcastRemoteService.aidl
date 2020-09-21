package com.byounghong.lim.podcast;

import com.byounghong.lim.podcast.IPodcastRemoteCallback;

interface IPodcastRemoteService {

    // 이벤트 콜백을 등록하기 위함.
	boolean registerCallback(IPodcastRemoteCallback callback);

    // 이벤트 콜백을 해하기 위함.
	boolean unregisterCallback(IPodcastRemoteCallback callback);

    // 멈춘 팟캐스트 재생을 한다.
    int commandPlayPodcast();

    // 재생중인 팟캐스트를 일시 중지/다시 시작을 한다.
    int commandTogglePlayPodcast();

    // 이전 팟캐스트 재생을 한다.
    int commandPreviousPodcast();

    // 다음 팟캐스트 재생을 한다.
    int commandNextPodcast();

    // 현재 팟캐스트가 재생중인지 체크를 한다.
    boolean commandIsPlayingPodcast();

    // 팟캐스트 방송명
    String commandGetPodcastName();

    // 팟캐스트 에피소드명
    String commandGetEpisodeName();

    // 팟캐스트 이미지
    String commandGetPodcastImage();

    // 마지막 재생 시간
    long commandGetLastPlayTime();

}
