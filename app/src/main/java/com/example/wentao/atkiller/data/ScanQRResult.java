package com.example.wentao.atkiller.data;

import java.io.Serializable;

public class ScanQRResult implements Serializable {
    private static final String IP_TAG = "\"IP\":\"";
    private static final String PORT_TAG ="\"Port\":";

    private boolean valid = false;
    private String pcAddress;
    private int pcPortNum = 0;

    public ScanQRResult(String qrString) {
        pcAddress = "";
        pcPortNum = 0;

        boolean ipValid = false;
        int beginIndex = -1;
        int endIndex = -1;

        beginIndex = qrString.indexOf(IP_TAG);
        if (beginIndex != -1 && beginIndex + IP_TAG.length() < qrString.length()) {
            endIndex = qrString.indexOf("\"", beginIndex + IP_TAG.length());
            if (endIndex != -1 && endIndex > beginIndex) {
                pcAddress = qrString.substring(beginIndex + IP_TAG.length(), endIndex);
                ipValid = true;
            }
        }

        if (ipValid) {
            endIndex = -1;
            beginIndex = qrString.indexOf(PORT_TAG);
            if (beginIndex != -1 && beginIndex + PORT_TAG.length() < qrString.length()) {
                endIndex = qrString.indexOf("}", beginIndex + 1);
                if (endIndex != -1 && endIndex > beginIndex) {
                    pcPortNum = Integer.valueOf(qrString.substring(beginIndex + PORT_TAG.length(), endIndex)).intValue();
                    valid = true;
                }
            }
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getPCAddress() {
        return pcAddress;
    }

    public int getPCPortNum() {
        return pcPortNum;
    }
}