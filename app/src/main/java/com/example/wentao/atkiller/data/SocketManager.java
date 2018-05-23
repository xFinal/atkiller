package com.example.wentao.atkiller.data;

import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


public class SocketManager {
    private static final String LOG_TAG = "SocketManager";
    private static final int HEAD_BUFFER_SIZE = 4;
    private static final int BUFFER_SIZE = 10240;

    private Selector selector;
    private SocketChannel socketChannel;

    private volatile boolean stop = false;
    private volatile boolean lastConnectionClosed = true;

    private ByteBuffer readContentBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private ByteBuffer writeContentBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private ConnectionStateAndDataChanagedListener connectionStateAndDataChanagedListener;
    public interface ConnectionStateAndDataChanagedListener {
        void onConnected();
        void onDisconnectedByServer();
        void onReadJson(String jsonString);
    }
    public void setConnectionStateAndDataChanagedListener(ConnectionStateAndDataChanagedListener connectionStateAndDataChanagedListener){
        if(connectionStateAndDataChanagedListener != null) {
            this.connectionStateAndDataChanagedListener = connectionStateAndDataChanagedListener;
        }
    }

    public void connect(String pcAddress, int pcPortNum) {
        try {
            while (!lastConnectionClosed) {
                Thread.sleep(500);
                Log.d(LOG_TAG, "Wait for last connection closed");
            }

            stop = false;
            lastConnectionClosed = true;

            Log.d(LOG_TAG, "Connecting");

            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            socketChannel.connect(new InetSocketAddress(pcAddress, pcPortNum));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

//            ByteBuffer[] bufferArray = new ByteBuffer[]{headBuffer, readContentBuffer};

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

                    // Reading
                    if (key.isReadable()) {
                        readContentBuffer.clear();

                        while (!stop) {
                            int readSize = socketChannel.read(readContentBuffer);

                            if (readSize > 0) {
                                readContentBuffer.flip();
                                String result = StandardCharsets.US_ASCII.decode(readContentBuffer).toString();
                                Log.d(LOG_TAG, result);

                                if (connectionStateAndDataChanagedListener != null) {
                                    connectionStateAndDataChanagedListener.onReadJson(result);
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
                            // No content
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
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        selector = null;
        socketChannel = null;

        Log.d(LOG_TAG, "Socket closed");
    }

    public long writeString(String data) {
        return writeBytes(data.getBytes(), data.length());
    }

    public void clearWriteBuffer() {
        writeContentBuffer.clear();
    }

    /**
     * Will not fill the buffer if the remaining size is not enough
     *
     * @return The remaining size of the write buffer,
     * -1 if there is not enough space
     */
    public int fillWriteBuffer(byte[] data, int length) {
        int remaining = -1;
        if (writeContentBuffer.remaining() >= length) {
            writeContentBuffer.put(data, writeContentBuffer.position(), length);

            remaining = writeContentBuffer.remaining();
        }

        return remaining;
    }

    public long writeBytes(byte[] data, int length) {
        long writtenBytes = -1;

        if (socketChannel.isConnected()) {
            try {
                writeContentBuffer.clear();
                if (writeContentBuffer.remaining() >= length) {
                    writeContentBuffer.put(data);
                    writeContentBuffer.flip();

                    while (true) {
                        writtenBytes = socketChannel.write(writeContentBuffer);

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
