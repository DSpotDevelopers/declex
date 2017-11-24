package com.dspot.declex.test.action;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.dspot.declex.test.R;

import static com.dspot.declex.actions.Action.*;

import org.androidannotations.annotations.EBean;

@EBean
public class ActionNotification {

    protected String contentTitle = "Testing";
    protected String contentInfo = "Info Finish";
    protected String contentText = "Long Text";

    public void notificationFinish() {
        $Notification(430)
                .contentTitle(contentTitle)
                .contentInfo(contentInfo)
                .contentText(contentText);
    }

    public void notificationDownloadCancel() {
        $Notification(530)
                .contentTitle(contentTitle)
                .contentInfo(contentInfo)
                .contentText(contentText)
                .autoCancel(true)
                .cancelPrevious(true);
    }

    public void notificationWhen() {
        Bitmap bitmap = BitmapFactory.decodeResource(Resources.getSystem(), R.mipmap.ic_launcher);

        $Notification(630)
                .contentTitle(contentTitle)
                .contentInfo(contentInfo)
                .contentText(contentText)
                .when(300)
                .onlyAlertOnce(true)
                .largeIcon(bitmap);
    }

    public void notificationSound() {
        Uri uriSound= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        $Notification(730)
                .contentTitle(contentTitle)
                .contentInfo(contentInfo)
                .contentText(contentText)
                .sound(uriSound);
    }

    public void notificationStyle() {
        NotificationCompat.BigTextStyle  style = new NotificationCompat.BigTextStyle();
        style.bigText(contentTitle);
        style.setBigContentTitle(contentInfo);
        style.setSummaryText(contentText);

        $Notification(830)
                .style(style);
    }
}
