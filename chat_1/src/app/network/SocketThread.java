package app.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketThread extends Thread {

    private SocketThreadListener eventListener;
    private Socket socket;
    private DataOutputStream out;

    public SocketThread(String name, SocketThreadListener eventListener, Socket socket){
        super(name);
        this.socket = socket;
        this.eventListener = eventListener;
        start();
    }

    @Override
    public void run() {
        try {
            eventListener.onStartSocketThread(this, socket);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            eventListener.onSocketIsReady(this, socket);
            while(!isInterrupted()){
                String msg = in.readUTF();
                eventListener.onReceiveString(this, socket, msg);
            }
        } catch (IOException e){
            eventListener.onException(this, socket, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                eventListener.onException(this, socket, e);
            }
            eventListener.onStopSocketThread(this, socket);
        }
    }

    public synchronized boolean sendMsg(String msg){
        try {
            out.writeUTF(msg);
            out.flush();
            return true;
        } catch (IOException e) {
            eventListener.onException(this, socket, e);
            close();
            return false;
        }
    }

    public synchronized void close(){
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.onException(this, socket, e);
        }
    }
}
