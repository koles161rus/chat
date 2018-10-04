package app.network;

import java.net.Socket;

public interface SocketThreadListener {

    void onStartSocketThread(SocketThread socketThread, Socket socket);
    void onStopSocketThread(SocketThread socketThread, Socket socket);

    void onSocketIsReady(SocketThread socketThread, Socket socket);
    void onReceiveString(SocketThread socketThread, Socket socket, String value);

    void onException(SocketThread socketThread, Socket socket, Exception e);
}
