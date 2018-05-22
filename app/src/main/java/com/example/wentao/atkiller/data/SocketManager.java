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

    private ConnectionStateAndDataChanagedListener connectionStateAndDataChanagedListener;
    public interface ConnectionStateAndDataChanagedListener {
        void onConnected();
        void onDisconnectedByServer();

        void onReadContact();
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

            ByteBuffer headBuffer = ByteBuffer.allocate(HEAD_BUFFER_SIZE);
            ByteBuffer readContentBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            ByteBuffer[] bufferArray = new ByteBuffer[]{headBuffer, readContentBuffer};

            while (!stop) {
                selector.select(500);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    SocketChannel sc = (SocketChannel) key.channel();

                    // Connected
                    if (key.isConnectable() && sc.finishConnect()) {
                        sc.register(selector, SelectionKey.OP_READ);
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
                            int readSize = sc.read(readContentBuffer);
                            if (readSize > 0) {
                                readContentBuffer.flip();
                                String result = StandardCharsets.US_ASCII.decode(readContentBuffer).toString();
                                Log.d(LOG_TAG, result);

                                String writeContent = "S7 Batman";
                                readContentBuffer.clear();
                                readContentBuffer.put(writeContent.getBytes());
                                readContentBuffer.flip();
                                sc.write(readContentBuffer);
                            } if (readSize == -1 || !sc.isConnected()) { // Connection has been closed by server
                                stop = true;

                                // Informing the caller
                                if (connectionStateAndDataChanagedListener != null) {
                                    connectionStateAndDataChanagedListener.onDisconnectedByServer();
                                }

                                break;
                            } else {  // No content
                                break;
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
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

    public void stop() {
        stop = true;
    }

    public void writeData() {

    }
}
