package com.dspot.declex.test.action;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.dspot.declex.actions.NotificationActionHolder_;
import com.dspot.declex.test.R;

import static junit.framework.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({ActionNotification_.class, NotificationActionHolder_.class})
public class TestActionNotification {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testActionMockNotification() {
        NotificationActionHolder_ holder = mock(NotificationActionHolder_.class);
        doNothing().when(holder).init(4);
        doNothing().when(holder).build(isNull(Runnable.class));
        doNothing().when(holder).execute();

        holder.init(4);
        holder.build(isNull(Runnable.class));
        holder.execute();

        verify(holder).init(4);
        verify(holder).build(isNull(Runnable.class));
        verify(holder).execute();
    }

    @Test
    public void testActionNotification() {
        NotificationActionHolder_ holder = NotificationActionHolder_.getInstance_(RuntimeEnvironment.application);
        holder.init(4);
        assertNotNull(holder.builder());
    }

    @Test
    public void testActionNotificationPropertiesString() {
        String contentTitle = "Testing";
        String contentInfo = "Info Finish";
        String contentText = "Long Text";

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText(contentTitle);
        style.setBigContentTitle(contentInfo);
        style.setSummaryText(contentText);

        NotificationActionHolder_ holder = mock(NotificationActionHolder_.class);
        doNothing().when(holder).init(4);
        doNothing().when(holder).build(isNull(Runnable.class));
        doNothing().when(holder).execute();

        holder.init(4);
        holder.contentTitle(contentTitle);
        holder.contentInfo(contentInfo);
        holder.contentText(contentText);
        holder.style(style);
        holder.build(isNull(Runnable.class));
        holder.execute();

        verify(holder).init(4);
        verify(holder).build(isNull(Runnable.class));
        verify(holder).execute();
    }

    @Test
    public void testActionNotificationOtherProperties() {
        Bitmap bitmap = BitmapFactory.decodeResource(Resources.getSystem(), R.mipmap.ic_launcher);
        Uri uriSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationActionHolder_ holder = mock(NotificationActionHolder_.class);
        doNothing().when(holder).init(4);
        doNothing().when(holder).build(isNull(Runnable.class));
        doNothing().when(holder).execute();

        holder.init(4);
        holder.largeIcon(bitmap);
        holder.sound(uriSound);
        holder.build(isNull(Runnable.class));
        holder.execute();

        verify(holder).init(4);
        verify(holder).build(isNull(Runnable.class));
        verify(holder).execute();
    }
}