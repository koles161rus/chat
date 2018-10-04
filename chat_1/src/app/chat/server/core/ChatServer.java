package app.chat.server.core;

import app.chat.Cmd;
import app.network.ServerSocketThread;
import app.network.ServerSocketThreadListener;
import app.network.SocketThread;
import app.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private ServerSocketThread serverSocketThread;
    private final Vector<SocketThread> clients = new Vector<>();

    public void start(int port) {
        if (serverSocketThread != null && serverSocketThread.isAlive()) {
            System.out.println("Сервер уже запущен.");
            return;
        }
        serverSocketThread = new ServerSocketThread("ServerSocketThread", this, port, 3000);
        SQLClient.connect();
        System.out.println("nick = " + SQLClient.getNick("login_1", "pass1"));
    }

    public void stop() {
        if (serverSocketThread == null || !serverSocketThread.isAlive()) {
            System.out.println("Сервер не запущен.");
            return;
        }
        serverSocketThread.interrupt();
        SQLClient.disconnect();
    }

    //События ServerSocketThread в потоке ServerSocketThread
    @Override
    public void onStartServerThread(ServerSocketThread thread) {
        putLog(thread, "started.");
    }

    @Override
    public void onStopServerThread(ServerSocketThread thread) {
        putLog(thread, "stopped.");
    }

    @Override
    public void onCreateServerSocket(ServerSocketThread thread, ServerSocket serverSocket) {
        putLog(thread, "onCreateServerSocket");
    }

    @Override
    public void onAcceptedSocket(ServerSocketThread thread, Socket socket) {
        putLog(thread, "Client connected: " + socket);
        String threadName = "Socket thread: " + socket.getInetAddress() + ":" + socket.getPort();
        new ChatSocketThread(threadName, this, socket);
    }

    @Override
    public void onTimeOutSocket(ServerSocketThread thread, ServerSocket serverSocket) {
//        putLog(thread, "onTimeOutSocket");
    }

    private synchronized void putLog(Thread thread, String msg) {
        System.out.println(thread.getName() + ": " + msg);
    }

    //События SocketThread'ов в соответствующих потоках
    @Override
    public synchronized void onStartSocketThread(SocketThread socketThread, Socket socket) {
        putLog(socketThread, "started.");
    }

    @Override
    public synchronized void onStopSocketThread(SocketThread socketThread, Socket socket) {
        putLog(socketThread, "stopped.");
        ChatSocketThread client = (ChatSocketThread) socketThread;
        clients.remove(client);
        if (client.authorized()) {
            if(client.reconnected()){
                sendBroadCastMsg(client.getNick() + ": reconnected.", true);
            } else {
                sendBroadCastMsg(client.getNick() + ": disconnected.", true);
            }
            sendBroadCastMsg(getAllUsersMsg(), false);
        }
    }

    @Override
    public synchronized void onSocketIsReady(SocketThread socketThread, Socket socket) {
        putLog(socketThread, "onSocketIsReady");
        clients.add(socketThread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread socketThread, Socket socket, String value) {
        ChatSocketThread client = (ChatSocketThread) socketThread;
        if (!client.authorized()) {
            handleNonAuthorizedMsg(client, value);
            return;
        }

        sendBroadCastMsg(client.getNick() + ": " + value, true);
    }

    private void handleNonAuthorizedMsg(ChatSocketThread newClient, String value) {
        String[] arr = value.split(Cmd.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Cmd.AUTH)) {
            newClient.sendMsg("Authorization message format error.");
            newClient.close();
            return;
        }
        String nick = SQLClient.getNick(arr[1], arr[2]);
        if (nick == null) {
            newClient.sendMsg("Incorrect login/password.");
            newClient.close();
            return;
        }
        ChatSocketThread client = findClientByNick(nick);
        if (client != null) {
            client.sendMsg("Повторная авторизация.");
            client.setReconnected(true);
            client.close();
        }
        newClient.setNick(nick);
        newClient.setAuthorized(true);
        newClient.sendMsg(Cmd.NICK + Cmd.DELIMITER + nick);
        sendBroadCastMsg(nick + ": connected", true);
        sendBroadCastMsg(getAllUsersMsg(), false);
    }

    private ChatSocketThread findClientByNick(String nick) {
        for (int i = 0; i < clients.size(); i++) {
            ChatSocketThread client = (ChatSocketThread) clients.get(i);
            if (!client.authorized()) continue;
            if (nick.equals(client.getNick())) return client;
        }
        return null;
    }

    private String getAllUsersMsg(){
        StringBuilder sb = new StringBuilder(Cmd.USERS);
        for (int i = 0; i < clients.size(); i++) {
            ChatSocketThread client = (ChatSocketThread) clients.get(i);
            if(!client.authorized()) continue;
            sb.append(Cmd.DELIMITER).append(client.getNick());
        }
        return sb.toString();
    }

    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss: ");

    private void sendBroadCastMsg(String msg, boolean addTime) {
        if (addTime) {
            msg = dateFormat.format(System.currentTimeMillis()) + msg;
        }
        for (int i = 0; i < clients.size(); i++) {
            ChatSocketThread client = (ChatSocketThread) clients.get(i);
            if (client.authorized()) client.sendMsg(msg);
        }
    }

    @Override
    public synchronized void onException(SocketThread socketThread, Socket socket, Exception e) {
        e.printStackTrace();
    }
}
