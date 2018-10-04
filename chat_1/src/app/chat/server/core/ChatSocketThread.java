package app.chat.server.core;

import app.network.SocketThread;
import app.network.SocketThreadListener;

import java.net.Socket;

class ChatSocketThread extends SocketThread {

    private String nick;
    private boolean authorized;
    private boolean reconnected;

    ChatSocketThread(String name, SocketThreadListener eventListener, Socket socket) {
        super(name, eventListener, socket);
    }

    boolean authorized() {
        return authorized;
    }

    void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    String getNick() {
        return nick;
    }

    void setNick(String nick) {
        this.nick = nick;
    }

    void setReconnected(boolean reconnected) {
        this.reconnected = reconnected;
    }

    boolean reconnected() {
        return reconnected;
    }
}
