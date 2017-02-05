/**
 * Copyright (C) 2016 DSpot Sp. z o.o
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dspot.declex.api.action.builtin;

import java.io.IOException;

import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.dspot.declex.api.action.annotation.ActionFor;
import com.dspot.declex.api.action.annotation.FormattedExpression;
import com.dspot.declex.api.action.annotation.StopOn;
import com.squareup.picasso.Picasso;

/**
 * An Action that represents how a persistent notification is to be presented to
 * the user using the {@link android.app.NotificationManager}.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For a guide to creating notifications, read the
 * <a href="{@docRoot}guide/topics/ui/notifiers/notifications.html">Status Bar Notifications</a>
 * developer guide.</p>
 * </div>
 */

@ActionFor("Notification")
public class NotificationActionHolder {

    NotificationCompat.Builder builder;

    @RootContext
    Context context;

    @SystemService
    NotificationManager manager;

    private boolean cancelPrevious = true;

    private int notificationId;

    private Runnable Shown;

    /**
     * @param notificationId An identifier for this notification unique within your
     *        application.
     */
    void init(int notificationId) {
        builder = new NotificationCompat.Builder(context); 
        this.notificationId = notificationId;
    }

    /**
     * @param Shown <i><b>(default)</b></i> This Action Selector will be executed when the Notification is shown 
     * with the NotificationManager
     */
    void build(final Runnable Shown) {
        if (cancelPrevious) {
            manager.cancel(notificationId);
        }
        this.Shown = Shown;
    };

    void execute() {
        manager.notify(notificationId, builder.build());
        this.Shown.run();
    }

    /**
     * @return Android Builder layer to access the underlying Notifications Builder object
     * 
     * @see android.support.v4.app.NotificationCompat.Builder Notifications Builder
     */
    @StopOn("build")
    public NotificationCompat.Builder builder() {
        return this.builder;
    }

    /**
     * Add a timestamp pertaining to the notification (usually the time the event occurred).
     * It will be shown in the notification content view by default; use
     * {@link #showWhen(boolean) showWhen} to control this.
     *
     * @see Notification#when
     */
    public NotificationActionHolder when(long when) {
        builder.setWhen(when);
        return this;
    }

    /**
     * Show the {@link Notification#when} field as a stopwatch.
     *
     * Instead of presenting <code>when</code> as a timestamp, the notification will show an
     * automatically updating display of the minutes and seconds since <code>when</code>.
     *
     * Useful when showing an elapsed time (like an ongoing phone call).
     *
     * @see android.widget.Chronometer
     * @see Notification#when
     */
    public NotificationActionHolder usesChronometer(boolean usesChronometer) {
        builder.setUsesChronometer(usesChronometer);
        return this;
    }

    /**
     * Set the small icon resource, which will be used to represent the notification in the
     * status bar.
     *

     * The platform template for the expanded view will draw this icon in the left, unless a
     * {@link #largeIcon(Bitmap) large icon} has also been specified, in which case the small
     * icon will be moved to the right-hand side.
     *

     * @param icon
     *            A resource ID in the application's package of the drawable to use.
     * @see Notification#icon
     */
    public NotificationActionHolder smallIcon(@DrawableRes int icon) {
        builder.setSmallIcon(icon);
        return this;
    }

    /**
     * A variant of {@link #smallIcon(int) smallIcon(int)} that takes an additional
     * level parameter for when the icon is a {@link android.graphics.drawable.LevelListDrawable
     * LevelListDrawable}.
     *
     * @param icon A resource ID in the application's package of the drawable to use.
     * @param level The level to use for the icon.
     *
     * @see Notification#icon
     * @see Notification#iconLevel
     */
    public NotificationActionHolder smallIcon(@DrawableRes int icon, int level) {
        builder.setSmallIcon(icon, level);
        return this;
    }

    /**
     * Set the first line of text in the platform notification template.
     */
    public NotificationActionHolder contentTitle(@FormattedExpression String contentTitle) {
        builder.setContentTitle(contentTitle);
        return this;
    }

    /**
     * Set the second line of text in the platform notification template.
     */
    public NotificationActionHolder contentText(@FormattedExpression String contentText) {
        builder.setContentTitle(contentText); 
        return this;
    }

    /**
     * Set the third line of text in the platform notification template.
     * Don't use if you're also using {@link #progress(int, int, boolean)}; they occupy the
     * same location in the standard template.
     */
    public NotificationActionHolder subText(@FormattedExpression String subText) {
        builder.setContentTitle(subText);
        return this;
    }

    /**
     * Set the large number at the right-hand side of the notification.  This is
     * equivalent to setContentInfo, although it might show the number in a different
     * font size for readability.
     */
    public NotificationActionHolder number(int number) {
        builder.setNumber(number);
        return this;
    }

    /**
     * A small piece of additional information pertaining to this notification.
     *
     * The platform template will draw this on the last line of the notification, at the far
     * right (to the right of a smallIcon if it has been placed there).
     */
    public NotificationActionHolder contentInfo(@FormattedExpression String contentInfo) {
        builder.setContentInfo(contentInfo);
        return this;
    }

    /**
     * Set the progress this notification represents.
     *
     * The platform template will represent this using a {@link ProgressBar}.
     */
    public NotificationActionHolder progress(int max, int progress, boolean indeterminate) {
        builder.setProgress(max, progress, indeterminate);
        return this;
    }

    /**
     * Supply a custom RemoteViews to use instead of the platform template.
     *
     * @see Notification#contentView
     */
    public NotificationActionHolder content(RemoteViews content) {
        builder.setContent(content);
        return this;
    }

    /**
     * Set the "ticker" text which is sent to accessibility services.
     *
     * @see Notification#tickerText
     */
    public NotificationActionHolder ticker(@FormattedExpression String ticker) {
        builder.setTicker(ticker);
        return this;
    }

    /**
     * Add a large icon to the notification content view.
     *
     * In the platform template, this image will be shown on the left of the notification view
     * in place of the {@link #smallIcon(Icon) small icon} (which will be placed in a small
     * badge atop the large icon).
     */
    public NotificationActionHolder largeIcon(Bitmap icon) {
        builder.setLargeIcon(icon);
        return this;
    }

    /**
     * Add a large icon to the notification content view.
     *
     * In the platform template, this image will be shown on the left of the notification view
     * in place of the {@link #smallIcon(Icon) small icon} (which will be placed in a small
     * badge atop the large icon).
     */
    public NotificationActionHolder largeIcon(@FormattedExpression String icon) {
        if (icon == null || icon.isEmpty()) return this;

        try {
            Bitmap bitmap = Picasso.with(context).load(icon).get();
            builder.setLargeIcon(bitmap);
        } catch (IOException ignored) {}

        return this;
    }

    /**
     * Set the sound to play.
     *
     * It will be played using the {@link #AUDIO_ATTRIBUTES_DEFAULT default audio attributes}
     * for notifications.
     *
     * <p>
     * A notification that is noisy is more likely to be presented as a heads-up notification.
     * </p>
     *
     * @see Notification#sound
     */
    public NotificationActionHolder sound(Uri sound) {
        builder.setSound(sound);
        return this;
    }

    /**
     * Set the vibration pattern to use.
     *
     * See {@link android.os.Vibrator#vibrate(long[], int)} for a discussion of the
     * <code>pattern</code> parameter.
     *
     * <p>
     * A notification that vibrates is more likely to be presented as a heads-up notification.
     * </p>
     *
     * @see Notification#vibrate
     */
    public NotificationActionHolder vibrate(long[] pattern) {
        builder.setVibrate(pattern);
        return this;
    }

    /**
     * Set the desired color for the indicator LED on the device, as well as the
     * blink duty cycle (specified in milliseconds).
     *

     * Not all devices will honor all (or even any) of these values.
     *

     * @see Notification#ledARGB
     * @see Notification#ledOnMS
     * @see Notification#ledOffMS
     */
    public NotificationActionHolder lights(@ColorInt int argb, int onMs, int offMs) {
        builder.setLights(argb, onMs, offMs);
        return this;
    }

    /**
     * Set whether this is an "ongoing" notification.
     *

     * Ongoing notifications cannot be dismissed by the user, so your application or service
     * must take care of canceling them.
     *

     * They are typically used to indicate a background task that the user is actively engaged
     * with (e.g., playing music) or is pending in some way and therefore occupying the device
     * (e.g., a file download, sync operation, active network connection).
     *

     * @see Notification#FLAG_ONGOING_EVENT
     * @see Service#setForeground(boolean)
     */
    public NotificationActionHolder ongoing(boolean ongoing) {
        builder.setOngoing(ongoing);
        return this;
    }

    /**
     * Set this flag if you would only like the sound, vibrate
     * and ticker to be played if the notification is not already showing.
     *
     * @see Notification#FLAG_ONLY_ALERT_ONCE
     */
    public NotificationActionHolder onlyAlertOnce(boolean onlyAlertOnce) {
        builder.setOnlyAlertOnce(onlyAlertOnce);
        return this;
    }

    /**
     * Make this notification automatically dismissed when the user touches it. The
     * PendingIntent set with {@link #deleteIntent} will be sent when this happens.
     *
     * @see Notification#FLAG_AUTO_CANCEL
     */
    public NotificationActionHolder autoCancel(boolean autoCancel) {
        builder.setAutoCancel(autoCancel);
        return this;
    }

    /**
     * Set which notification properties will be inherited from system defaults.
     * <p>
     * The value should be one or more of the following fields combined with
     * bitwise-or:
     * {@link #DEFAULT_SOUND}, {@link #DEFAULT_VIBRATE}, {@link #DEFAULT_LIGHTS}.
     * <p>
     * For all default values, use {@link #DEFAULT_ALL}.
     */
    public NotificationActionHolder defaults(int defaults) {
        builder.setDefaults(defaults);
        return this;
    }

    /**
     * Set the priority of this notification.
     *
     * @see Notification#priority
     */
    public NotificationActionHolder priority(int priority) {
        builder.setPriority(priority);
        return this;
    }

    public NotificationActionHolder cancelPrevious(boolean cancelPrevious) {
        this.cancelPrevious = cancelPrevious;
        return this;
    }

    /**
     * Supply a {@link PendingIntent} to be sent when the notification is clicked.
     *
     * As of {@link android.os.Build.VERSION_CODES#HONEYCOMB}, if this field is unset and you
     * have specified a custom RemoteViews with {@link #setContent(RemoteViews)}, you can use
     * {@link RemoteViews#setOnClickPendingIntent RemoteViews.setOnClickPendingIntent(int,PendingIntent)}
     * to assign PendingIntents to individual views in that custom layout (i.e., to create
     * clickable buttons inside the notification view).
     *
     * @see Notification#contentIntent Notification.contentIntent
     */
    public NotificationActionHolder contentIntent(PendingIntent intent) {
        builder.setContentIntent(intent);
        return this;
    }

    /**
     * Supply a {@link PendingIntent} to send when the notification is cleared explicitly by the user.
     *
     * @see Notification#deleteIntent
     */
    public NotificationActionHolder deleteIntent(PendingIntent intent) {
        builder.setDeleteIntent(intent);
        return this;
    }

    /**
     * An intent to launch instead of posting the notification to the status bar.
     * Only for use with extremely high-priority notifications demanding the user's
     * <strong>immediate</strong> attention, such as an incoming phone call or
     * alarm clock that the user has explicitly set to a particular time.
     * If this facility is used for something else, please give the user an option
     * to turn it off and use a normal notification, as this can be extremely
     * disruptive.
     *
     * <p>
     * The system UI may choose to display a heads-up notification, instead of
     * launching this intent, while the user is using the device.
     * </p>
     *
     * @param intent The pending intent to launch.
     * @param highPriority Passing true will cause this notification to be sent
     *          even if other notifications are suppressed.
     *
     * @see Notification#fullScreenIntent
     */
    public NotificationActionHolder fullScreenIntent(PendingIntent intent, boolean highPriority) {
        builder.setFullScreenIntent(intent, highPriority);
        return this;
    }
}
