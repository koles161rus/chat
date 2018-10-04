package app.chat.client;

import app.chat.Cmd;
import app.network.SocketThread;
import app.network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientGUI();
            }
        });
    }

    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 300;
    private static final String TITLE = "Chat client";

    private final JTextArea log = new JTextArea();

    private final JPanel upperPanel = new JPanel(new GridLayout(2, 3));
    private final JTextField fieldIPAddr = new JTextField("127.0.0.1");
    private final JTextField fieldPort = new JTextField("8189");
    private final JCheckBox chkAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextField fieldLogin = new JTextField("login_1");
    private final JPasswordField fieldPass = new JPasswordField("pass_1");
    private final JButton btnLogin = new JButton("Login");

    private final JPanel bottomPanel = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("Disconnect");
    private final JTextField fieldInput = new JTextField();
    private final JButton btnSend = new JButton("Send");

    private final JList<String> usersList = new JList<>();

    private ClientGUI(){
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setTitle(TITLE);
        setLocationRelativeTo(null);
        setResizable(false);

        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
//        scrollLog.setAutoscrolls(true);
        add(scrollLog, BorderLayout.CENTER);

        btnSend.addActionListener(this);
        btnDisconnect.addActionListener(this);
        fieldInput.addActionListener(this);
        btnLogin.addActionListener(this);
        chkAlwaysOnTop.addActionListener(this);

        upperPanel.add(fieldIPAddr);
        upperPanel.add(fieldPort);
        upperPanel.add(chkAlwaysOnTop);
        upperPanel.add(fieldLogin);
        upperPanel.add(fieldPass);
        upperPanel.add(btnLogin);
        add(upperPanel, BorderLayout.NORTH);

        bottomPanel.add(btnDisconnect, BorderLayout.WEST);
        bottomPanel.add(fieldInput, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);
        bottomPanel.setVisible(false);
        add(bottomPanel, BorderLayout.SOUTH);

        JScrollPane scrollUsers = new JScrollPane(usersList);
        scrollUsers.setPreferredSize(new Dimension(150, 0));
        add(scrollUsers, BorderLayout.EAST);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if(src == chkAlwaysOnTop) {
            setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        } else if(src == btnLogin) {
            connect();
        } else if(src == btnSend || src == fieldInput){
            sendMsg();
        } else if(src == btnDisconnect){
            socketThread.close();
        } else {
            throw new RuntimeException("Неизвестный src = " + src);
        }
    }

    private void sendMsg(){
        String msg = fieldInput.getText();
        fieldInput.setText(null);
        fieldInput.grabFocus();
        socketThread.sendMsg(msg);
//        try(FileWriter out = new FileWriter("log.txt", true)){
//            out.write(msg);
//            out.flush();
//        } catch (IOException e){
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        String msg;
        if(stackTraceElements.length == 0){
            msg = "Пустой StackTrace";
        } else {
            msg = e.getClass().getCanonicalName() + ": " + e.getMessage() + "\n" +
                    stackTraceElements[0].toString();
        }
        JOptionPane.showMessageDialog(null, msg, "Exception: ", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private SocketThread socketThread;

    private void connect(){
        try {
            Socket socket = new Socket(fieldIPAddr.getText(), Integer.parseInt(fieldPort.getText()));
            socketThread = new SocketThread("SocketThread", this, socket);
        } catch (IOException e) {
            log.append("Exception: " + e.getMessage() + "\n");
        }
    }

    //События сокета в потоке сокета
    @Override
    public void onStartSocketThread(SocketThread socketThread, Socket socket) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("SocketThread started.\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    @Override
    public void onStopSocketThread(SocketThread socketThread, Socket socket) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Connection lost.\n");
                log.setCaretPosition(log.getDocument().getLength());
                upperPanel.setVisible(true);
                bottomPanel.setVisible(false);
                usersList.setListData(EMPTY);
                setTitle(TITLE);
            }
        });
    }

    private final String[] EMPTY = new String[0];

    @Override
    public void onSocketIsReady(SocketThread socketThread, Socket socket) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Connection established.\n");
                log.setCaretPosition(log.getDocument().getLength());
                upperPanel.setVisible(false);
                bottomPanel.setVisible(true);
                String request = Cmd.AUTH + Cmd.DELIMITER +
                        fieldLogin.getText() + Cmd.DELIMITER +
                        new String(fieldPass.getPassword());
                socketThread.sendMsg(request);
            }
        });
    }

    @Override
    public void onReceiveString(SocketThread socketThread, Socket socket, String value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(handleMessage(value)) return;
                log.append(value + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private boolean handleMessage(String value){
        if(value.length() < 2 || value.charAt(0) != '/') return false;
        String[] arr = value.split(Cmd.DELIMITER);
        switch (arr[0]){
            case Cmd.NICK:
                setTitle(TITLE + " Login as: " + arr[1]);
                return true;
            case Cmd.USERS:
                String users = value.substring(Cmd.USERS.length() + Cmd.DELIMITER.length());
                String[] arrUsers = users.split(Cmd.DELIMITER);
                Arrays.sort(arrUsers);
                usersList.setListData(arrUsers);
                return true;
            default:
                throw new RuntimeException("Неизвестное служебное сообщение: " + value);
        }
    }

    @Override
    public synchronized void onException(SocketThread socketThread, Socket socket, Exception e){
        e.printStackTrace();
    }
}
