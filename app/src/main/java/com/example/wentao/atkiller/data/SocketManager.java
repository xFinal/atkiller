package com.example.wentao.atkiller.data;

import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class SocketManager {
    private static final String LOG_TAG = "SocketManager";
    public static final int SOCKET_DATA_BUFFER_SIZE = 2;

    private Selector selector;
    private SocketChannel socketChannel;

    private volatile boolean stop = false;
    private volatile boolean lastConnectionClosed = true;

    private ByteBuffer inputBuffer = ByteBuffer.allocate(SOCKET_DATA_BUFFER_SIZE);
    private ByteBuffer outputBuffer = ByteBuffer.allocate(SOCKET_DATA_BUFFER_SIZE);

    private ConnectionStateAndDataChanagedListener connectionStateAndDataChanagedListener;
    public interface ConnectionStateAndDataChanagedListener {
        void onConnected();
        void onDisconnectedByServer();
        void handleData();
    }
    public void setConnectionStateAndDataChanagedListener(ConnectionStateAndDataChanagedListener connectionStateAndDataChanagedListener){
        if(connectionStateAndDataChanagedListener != null) {
            this.connectionStateAndDataChanagedListener = connectionStateAndDataChanagedListener;
        }
    }

    public SocketManager() {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN);
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void connect(String pcAddress, int pcPortNum) {
        try {
            while (!lastConnectionClosed) {
                Thread.sleep(500);
                Log.d(LOG_TAG, "Wait for last connection closed");
            }

            stop = false;
            lastConnectionClosed = false;

            Log.d(LOG_TAG, "Connecting");

            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            socketChannel.connect(new InetSocketAddress(pcAddress, pcPortNum));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            while (!stop) {
                selector.select(500);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    // Connected
                    if (key.isConnectable() && socketChannel.finishConnect()) {
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        Log.d(LOG_TAG, "Connected");

                        // Informing the caller connection has been established
                        if (connectionStateAndDataChanagedListener != null) {
                            connectionStateAndDataChanagedListener.onConnected();
                        }
                    }

                    // Read the input data
                    if (socketChannel.isConnected() && key.isReadable()) {
                        while (!stop) {
                            inputBuffer.clear();
                            int readSize = socketChannel.read(inputBuffer);

                            // Handle the input data
                            if (readSize > 0) {
                                inputBuffer.flip();

                                // Informing the caller
                                if (connectionStateAndDataChanagedListener != null) {
                                    connectionStateAndDataChanagedListener.handleData();
                                }
                            }

                            // Connection has been closed by server
                            else if (readSize == -1 || !socketChannel.isConnected()) {
                                stop = true;

                                // Informing the caller
                                if (connectionStateAndDataChanagedListener != null) {
                                    connectionStateAndDataChanagedListener.onDisconnectedByServer();
                                }

                                break;
                            }

                            // No data
                            else {
                                break;
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            // Informing the caller
            if (connectionStateAndDataChanagedListener != null) {
                connectionStateAndDataChanagedListener.onDisconnectedByServer();
            }

            e.printStackTrace();
        } finally {
            try {
                selector.close();
                socketChannel.close();
                selector = null;
                socketChannel = null;
                lastConnectionClosed = true;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        Log.d(LOG_TAG, "Socket closed");
    }

    public boolean copyInputBufferRemaining(ByteBuffer dst) {
        boolean result = false;
        if (dst.remaining() >= inputBuffer.remaining()) {
            dst.put(inputBuffer);
            result = true;
        }

        return result;
    }

    public long writeString(String data) {
        return writeBytes(data.getBytes(), data.length());
    }

    public void clearWriteBuffer() {
        outputBuffer.clear();
    }

    /**
     * Will not fill the buffer if the remaining size is not enough
     *
     * @return The remaining size of the write buffer,
     * -1 if there is not enough space
     */
    public int fillWriteBuffer(byte[] data, int length) {
        int remaining = -1;
        if (outputBuffer.remaining() >= length) {
            outputBuffer.put(data, outputBuffer.position(), length);

            remaining = outputBuffer.remaining();
        }

        return remaining;
    }

    public long writeBytes(byte[] data, int length) {
        long writtenBytes = -1;

        if (socketChannel.isConnected()) {
            try {
                outputBuffer.clear();
                if (outputBuffer.remaining() >= length) {
                    outputBuffer.put(data);
                    outputBuffer.flip();

                    while (true) {
                        writtenBytes = socketChannel.write(outputBuffer);

                        if (writtenBytes >0 || !socketChannel.isConnected()) {
                            break;
                        }

                        if (writtenBytes == 0) {
                            Thread.sleep(500);
                        }
                    }
                }
            } catch(Exception e) {
            }
        }

        return writtenBytes;
    }

    public void stop() {
        stop = true;
    }
}
