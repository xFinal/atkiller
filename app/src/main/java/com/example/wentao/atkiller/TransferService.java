package com.example.wentao.atkiller;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.wentao.atkiller.data.ContactData;
import com.example.wentao.atkiller.data.ScanQRResult;
import com.example.wentao.atkiller.data.SocketManager;



public class TransferService extends IntentService {
    public static final String INTENT_EXTRA_QR_RESULT = "QRResult";
    public static final String BROADCAST_SOCKET_SERVER_CONNECTED = "com.example.wentao.atkiller.ServerConnected";
    public static final String BROADCAST_DISCONNECTED_BY_SOCKET_SERVER = "com.example.wentao.atkiller.DisconnectedByServer";

    private static final String LOG_TAG = "TransferService";
    private static final String NOTIFICATION_CHANNELID = "TRANSFER_NOTIFICATION";
    private static final int NOTIFICATION_ID = 419;

    private SocketManager socketManager;

    public TransferService() {
        super("TransferService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNELID)
                .setContentTitle(getString(R.string.notification_bar_title))
                .setContentText(getString(R.string.notification_bar_content))
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        socketManager = new SocketManager();
    }

    @Override
    public void onDestroy() {
        socketManager.stop();

        stopForeground(NOTIFICATION_ID);

        super.onDestroy();

        Log.d(LOG_TAG, "Service destroyed");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.hasExtra(INTENT_EXTRA_QR_RESULT)) {
            ScanQRResult qrResult = (ScanQRResult)intent.getSerializableExtra(INTENT_EXTRA_QR_RESULT);
            socketManager.setConnectionStateAndDataChanagedListener(new SocketManager.ConnectionStateAndDataChanagedListener() {
                @Override
                public void onConnected() {
                    Intent intent = new Intent(BROADCAST_SOCKET_SERVER_CONNECTED);
                    LocalBroadcastManager.getInstance(TransferService.this).sendBroadcast(intent);
                }

                @Override
                public void onDisconnectedByServer() {
                    Intent intent = new Intent(BROADCAST_DISCONNECTED_BY_SOCKET_SERVER);
                    LocalBroadcastManager.getInstance(TransferService.this).sendBroadcast(intent);
                }

                @Override
                public void onReadContact() {
                    ContactData.read(TransferService.this);
                }
            });

            socketManager.connect(qrResult.getPCAddress(), qrResult.getPCPortNum());
        }
    }
}
