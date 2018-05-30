package com.example.wentao.atkiller;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.wentao.atkiller.data.ScanQRResult;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

import static com.example.wentao.atkiller.TransferService.BROADCAST_DISCONNECTED_BY_SOCKET_SERVER;
import static com.example.wentao.atkiller.TransferService.BROADCAST_SOCKET_SERVER_CONNECTED;
import static com.example.wentao.atkiller.TransferService.INTENT_EXTRA_QR_RESULT;

public class MainActivity extends BaseActivity {

    private final static int PERMSISSION_REQUEST_CODE = 522;

    private boolean permissionGranted = true;

    private Intent intentService;
    private boolean socketConnected = false;

    private LocalReceiver localReceiver;
    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == BROADCAST_SOCKET_SERVER_CONNECTED) {
                onSocketConnected();
            }
            else if (action == BROADCAST_DISCONNECTED_BY_SOCKET_SERVER) {
                onSocketDisconnected();
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopTransferService();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);

        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();

        // Preparing receiving broadcast from service
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_SOCKET_SERVER_CONNECTED);
        intentFilter.addAction(BROADCAST_DISCONNECTED_BY_SOCKET_SERVER);
        localReceiver = new LocalReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null && result.getContents() != null) {
            startTransferService(result.getContents());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMSISSION_REQUEST_CODE:
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissions[i])) {
                            requestPermission();

                            // Return directly for avoiding open app setting page multi times
                            return;
                        } else {
                            permissionGranted = false;
                        }
                    }
                }

                break;

            default:
                break;
        }

        // Guide user to open app setting page for enabling permissions
        if (!permissionGranted) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_message_open_app_setting_page)
                    .setNegativeButton(R.string.dialog_button_text_go, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            openAppSettingPage();

                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(R.string.dialog_button_text_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
            dialog.show();
        }
    }

    public void onScanClick(View v) {
        if (!socketConnected) {
            stopTransferService();

//            new IntentIntegrator(this)
//                    .setCaptureActivity(ScanQRActivity.class)
//                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
//                    .setPrompt(getString(R.string.scan_qr_tip))
//                    .setCameraId(0)
//                    .setBeepEnabled(false)

            startTransferService("https://www.imobie.com/anytrans/download-android-win-apk.htm?arg={\"IP\":\"192.168.2.253\",\"Port\":58419}");
        } else {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_message_stop_connection)
                    .setNegativeButton(R.string.dialog_button_text_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopTransferService();
                            onSocketDisconnected();

                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(R.string.dialog_button_text_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
            dialog.show();
        }
    }

    private void requestPermission() {
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        List<String> deniedPermissionList = new ArrayList<>();

        for (String permission:permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissionList.add(permission);
            }
        }

        if (!deniedPermissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, deniedPermissionList.toArray(new String[deniedPermissionList.size()]), PERMSISSION_REQUEST_CODE);
        }
    }

    private void openAppSettingPage() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void startTransferService(String contents) {
        ScanQRResult scanQRResult = new ScanQRResult(contents);
        if (scanQRResult.isValid()) {
            intentService = new Intent(this, TransferService.class);
            intentService.putExtra(INTENT_EXTRA_QR_RESULT, scanQRResult);
            startService(intentService);
        } else {
            Toast.makeText(this, getString(R.string.toast_invalid_qr), Toast.LENGTH_LONG).show();
        }
    }

    private void stopTransferService() {
        if (intentService != null) {
            stopService(intentService);
            intentService = null;
        }
    }

    private void onSocketDisconnected() {
        socketConnected = false;
        intentService = null;

        Button scanButton = findViewById(R.id.ScanButton);
        scanButton.setText(R.string.button_text_scan);
    }

    private void onSocketConnected() {
        socketConnected = true;

        Button scanButton = findViewById(R.id.ScanButton);
        scanButton.setText(R.string.button_text_stop_connection);
    }
}
