package com.example.wentao.atkiller.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

class DcimData {
    private final static String LOG_TAG = "DcimData";

    public void listAll(Context context, SocketManager socketManager) {
        String searchPath = MediaStore.Files.FileColumns.DATA + " LIKE '%" + "/DCIM/Camera/" + "%' ";
        Uri uri = MediaStore.Files.getContentUri("external");
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.TITLE},
                searchPath,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String imageID = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                String imageName = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE));
                Log.d(LOG_TAG, imageName);
            } while (cursor.moveToNext());
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }
    }
}
