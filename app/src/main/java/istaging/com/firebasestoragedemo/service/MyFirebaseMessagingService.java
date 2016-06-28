package istaging.com.firebasestoragedemo.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import istaging.com.firebasestoragedemo.MainActivity;
import istaging.com.firebasestoragedemo.R;

/**
 * Created by iStaging on 2016/6/27.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public final static String EXTRA_MESSAGE = ".service.MyFirebaseMessagingService";
    private final String TAG = "MessagingService";

    public static final int NOTIFICATION_ID = 1;
    private long[] mVibratePattern = { 0, 200, 200, 300 };

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        // Log.d(TAG, "From: " + remoteMessage.getFrom());
        // Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());
        String contentText = remoteMessage.getNotification().getBody();
        Log.d(TAG, "Notification Message Body: " + contentText);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(EXTRA_MESSAGE, contentText);

        // Because clicking the notification opens a new activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the Notification
        Notification.Builder notificationBuilder = new Notification.Builder(
                getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentTitle("My notification")
                .setContentText(contentText)
                .setAutoCancel(true)
                .setVibrate(mVibratePattern)
                .setContentIntent(resultPendingIntent);

        // Pass the Notification to the NotificationManager
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID,
                notificationBuilder.build());
    }
}
