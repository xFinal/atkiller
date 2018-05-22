package com.example.wentao.atkiller.data;

import android.content.Context;

public class JsonParser {
    private DcimData dcimData = new DcimData();

    public void parse(Context context, SocketManager socketManager, String jsonString) {
        if (jsonString.equalsIgnoreCase("list dcim")) {
            dcimData.listAll(context, socketManager);
        }
    }
}
