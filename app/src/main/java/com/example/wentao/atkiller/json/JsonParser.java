package com.example.wentao.atkiller.json;

import android.content.Context;
import android.util.Log;

import com.example.wentao.atkiller.data.DcimData;
import com.example.wentao.atkiller.data.SocketManager;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.lang.String;

import static com.example.wentao.atkiller.data.SocketManager.SOCKET_DATA_BUFFER_SIZE;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class JsonParser {
    private static final String LOG_TAG = "JsonParser";
    private static final int HEAD_DATA_LEN_SIZE = 4;
    private static final int END_FLAG = 0x0a0d0a0d;

    private long remainingExtraDataSize = 0;

    ByteBuffer dataByteBuffer = ByteBuffer.allocate(SOCKET_DATA_BUFFER_SIZE * 2);
    ByteBuffer backupDataBuffer = ByteBuffer.allocate(SOCKET_DATA_BUFFER_SIZE * 2);
    private boolean bufferOverflow = false;

    private DcimData dcimData = new DcimData();

    public JsonParser() {
        dataByteBuffer.order(LITTLE_ENDIAN);
        backupDataBuffer.order(LITTLE_ENDIAN);
    }

    public void parse(Context context, SocketManager socketManager) {

        if (bufferOverflow) {
            dataByteBuffer.clear();
        }

        if (socketManager.copyInputBufferRemaining(dataByteBuffer)) {
            dataByteBuffer.flip();

            byte[] extraData = getExtraData();
            if (extraData != null) {
                // TBD, write data
            }

            while (dataByteBuffer.remaining() > HEAD_DATA_LEN_SIZE) {
                int jsonSize = dataByteBuffer.getInt();
                if (jsonSize == END_FLAG) {
                    continue;
                }
                else if (dataByteBuffer.remaining() >= jsonSize) {
                    byte[] jsonBytes = new byte[jsonSize];
                    dataByteBuffer.get(jsonBytes);
                    String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
                    Log.d(LOG_TAG, jsonString);
                } else {
                    break;
                }
            }

            // Data is not complete in the current data segment.
            // Put the remaining data to the front of the buffer, preparing for the next data segment
            if (dataByteBuffer.hasRemaining()) {
                if (dataByteBuffer.position() == 0) {
                    dataByteBuffer.position(dataByteBuffer.limit());
                    dataByteBuffer.limit(dataByteBuffer.capacity());
                } else {
                    byte[] remainingData = new byte[dataByteBuffer.remaining()];
                    dataByteBuffer.get(remainingData);
                    dataByteBuffer.clear();
                    dataByteBuffer.put(remainingData);
                }
            }
            else {
                dataByteBuffer.clear();
            }
        } else {
            //TBD, returns error to server
            bufferOverflow = true;
            dataByteBuffer.getInt();
            socketManager.copyInputBufferRemaining(backupDataBuffer);
            dataByteBuffer.clear();
        }
    }

    private long getExtraDataSize() {
        return 0;
    }

    private byte[] getExtraData() {
        byte[] extraData = null;

        if (remainingExtraDataSize == 0) {
            remainingExtraDataSize = getExtraDataSize();
        }

        if (remainingExtraDataSize > 0) {
            long dataSize = remainingExtraDataSize;
            if (remainingExtraDataSize > dataByteBuffer.remaining())
            {
                dataSize = dataByteBuffer.remaining();
            }

            remainingExtraDataSize -= dataSize;

            extraData = new byte[(int)dataSize];
            dataByteBuffer.get(extraData);


        }

        return extraData;
    }
}
