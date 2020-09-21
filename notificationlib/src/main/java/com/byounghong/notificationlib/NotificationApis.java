package com.byounghong.notificationlib;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * #
 *
 * @auther : byounghonglim
 * @since : 2019-10-10
 */
public class NotificationApis {
    public static void sendItemsNotification(Context context, int notiId, int imageId, String title, String text, String status){
        Intent intent = new Intent();
        intent.putExtra("type", "lim");
        intent.putExtra("text", text);
        intent.putExtra("status", status);

        sendNotification(context, intent, notiId, imageId, title);
    }

    /*
     ** 디바이스에서 호출되는 API
     */
    public static void sendSystemNotification(Context context, int notiId, int imageId,
                                              String title, String status, Boolean isShowApp){
        Intent intent = new Intent();
        intent.putExtra("type", "system");
        intent.putExtra("status", status);
        intent.putExtra("isShow", isShowApp);

        sendNotification(context, intent, notiId, imageId, title);
    }

    /*
     ** 디바이스에서 호출되는 API(스키마)
     */
    public static void sendAgreementNotification(Context context, int notiId, int imageId,
                                                 String title, String status,
                                                 String url, String agreementCode){
        Intent intent = new Intent();
        intent.putExtra("type", "agreement");
        intent.putExtra("status", status);
        intent.putExtra("url", url);
        intent.putExtra("agreement", agreementCode);

        sendNotification(context, intent, notiId, imageId, title);
    }

    /*
     ** 일정에서 호출되는 API
     */
    public static void sendScheduleNotification(Context context, int notiId, int imageId, String title, String text, String date, String time){
        Intent intent = new Intent();
        intent.putExtra("type", "schedule");
        intent.putExtra("text", text);
        intent.putExtra("date", date);
        intent.putExtra("time", time);

        sendNotification(context, intent, notiId, imageId, title);
    }

    /*
     ** 알람에서 호출되는 API
     */
    public static void sendAlarmNotification(Context context, int notiId, int imageId, String title, String date){
        Intent intent = new Intent();
        intent.putExtra("type", "alarm");
        intent.putExtra("date", date);

        sendNotification(context, intent, notiId, imageId, title);
    }

    public static void sendNormalNotification(Context context, int notiId, int imageId, String title){
        Intent intent = new Intent();
        intent.putExtra("type", "normal");

        sendNotification(context, intent, notiId, imageId, title);
    }

    private static Uri getResourceToUri(Context context,int resID){
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+"://"
                + context.getResources().getResourcePackageName(resID)+"/"
                + context.getResources().getResourceTypeName(resID)+"/"
                + context.getResources().getResourceEntryName(resID));
    }

    private static void sendNotification(Context context,  Intent intent, int notiId, int imageId, String title){
        intent.setAction("com.byounghong.notipopuplib.NotiPopUpService");
        intent.setPackage("com.byounghong.lim.quickmenu");
        intent.putExtra(Intent.EXTRA_STREAM, getResourceToUri(context, imageId));
        intent.putExtra("notiId", notiId);
        intent.putExtra("title", title);
        context.startService(intent);
    }

}
