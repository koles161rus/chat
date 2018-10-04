package app.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerSocketThread extends Thread {

    private ServerSocketThreadListener eventListener;
    private int port;
    private int timeout;

    public ServerSocketThread(String name, ServerSocketThreadListener eventListener, int port, int timeout){
        super(name);
        this.eventListener = eventListener;
        this.port = port;
        this.timeout = timeout;
        start();
    }

    @Override
    public void run() {
        eventListener.onStartServerThread(this);
        try(ServerSocket serverSocket = new ServerSocket(port)){
            serverSocket.setSoTimeout(timeout);
            eventListener.onCreateServerSocket(this, serverSocket);
            while(!isInterrupted()){
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (SocketTimeoutException e){
                    eventListener.onTimeOutSocket(this, serverSocket);
                    continue;
                }
                eventListener.onAcceptedSocket(this, socket); //за освоблждение сокета отвечает листнер
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        } finally {
            eventListener.onStopServerThread(this);
        }
    }
}
