package app.chat.server.core;

import java.sql.*;

class SQLClient {

    private static Connection connection;
    private static Statement statement;

    synchronized static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat_db.sqlite");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static String getNick(String login, String pass){
        String request = "SELECT nickname FROM users WHERE login='" +
                login + "' AND password='" + pass + "'";
        try(ResultSet resultSet = statement.executeQuery(request);){
            if(resultSet.next()){
                return resultSet.getString(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
