package com.braincloud.basic_java;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService
{
    public static String FirebaseTokenID;

    private static final String TAG = "MessagingService";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("NEW_TOKEN", s);
        // Do whatever you want with your token now
        // i.e. store it on SharedPreferences or DB
        // or directly send it to server
        FirebaseTokenID = s;
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        // Used for displaying old school GCM messages
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            Notification notification = new NotificationCompat.Builder(this, "Messages")
                    .setContentText(remoteMessage.getData().get("message"))
                    .setContentTitle(remoteMessage.getData().get("title"))
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                    .build();
            manager.notify(0, notification);

        }
    }
}
