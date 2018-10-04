package app.network;

import java.net.ServerSocket;
import java.net.Socket;

public interface ServerSocketThreadListener {

    void onStartServerThread(ServerSocketThread thread);
    void onStopServerThread(ServerSocketThread thread);

    void onCreateServerSocket(ServerSocketThread thread, ServerSocket serverSocket);
    void onAcceptedSocket(ServerSocketThread thread, Socket socket);
    void onTimeOutSocket(ServerSocketThread thread, ServerSocket serverSocket);
}
