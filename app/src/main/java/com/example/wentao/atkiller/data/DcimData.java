package com.example.wentao.atkiller.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.example.wentao.atkiller.Json.JsonCommonData;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

class DcimData {
    private final static String LOG_TAG = "DcimData";

    public void listAll(Context context, SocketManager socketManager) {
        String searchPath = MediaStore.Files.FileColumns.DATA + " LIKE '%" + "/DCIM/Camera/" + "%' ";
        Uri uri = MediaStore.Files.getContentUri("external");
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(
                uri,
                new String[]{MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.TITLE},
                searchPath,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            Gson gson = new Gson();
            JsonCommonData jsonCommonData = new JsonCommonData();

            do {
                String imageID = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                String imageName = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE));

                Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                        contentResolver,
                        Long.parseLong(imageID),
                        MediaStore.Images.Thumbnails.MICRO_KIND,
                        new BitmapFactory.Options());

                if (bitmap != null) {
                    byte[] bitmapData = Bitmap2Bytes(bitmap);

                    jsonCommonData.setAllData("ListAll", "photo", imageID, imageName, bitmapData.length);
                    String jsonString = gson.toJson(jsonCommonData);
                    Log.d(LOG_TAG, jsonString);
                    if (socketManager.writeString(jsonString) == -1) {
                        break;
                    }

                    if (socketManager.writeBytes(bitmapData, bitmapData.length) == -1) {
                        break;
                    }
                }

            } while (cursor.moveToNext());
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }
    }

    private String Bitmap2Base64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
        if (bitmap.hasAlpha()) {
            compressFormat = Bitmap.CompressFormat.PNG;
        }
        bitmap.compress(compressFormat, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private byte[] Bitmap2Bytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
        if (bitmap.hasAlpha()) {
            compressFormat = Bitmap.CompressFormat.PNG;
        }
        bitmap.compress(compressFormat, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
