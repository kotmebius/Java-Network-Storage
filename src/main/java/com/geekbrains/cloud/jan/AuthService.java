package com.geekbrains.cloud.jan;

import java.sql.*;
import java.text.SimpleDateFormat;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    private static final int AUTH_OK = 0;
    private static final int AUTH_FAIL = 1;
    private static final int USER_LOCKED = 2;
    private static final int NO_USER = 3;
    private static final int TIME_TO_LOCK = 30000;
    private static final int MAX_TRY_TO_AUTH = 3;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addUser(String login, String pass, String nick) {
        try {
            String query = "INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, pass.hashCode());
            ps.setString(3, nick);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int isAuth(String login, int hash) {
        try {
            int result;
            boolean isSuccess = false;
            boolean isInBlocklist = false;
            long timeLastFail=0;
            int countOfTry=0;
            long currentTime;
            ResultSet selectUser = stmt.executeQuery("SELECT id, password FROM users WHERE login = '" + login + "'");
            PreparedStatement ps = connection.prepareStatement("INSERT into authlog (date,login,result) values (?, ?, ?)");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String ts = sdf.format(timestamp);
            currentTime=timestamp.getTime();
            ps.setString(1, ts);
            if (selectUser.next()) {
                int userId = selectUser.getInt(1);
                int dbHash = selectUser.getInt(2);
                ResultSet selectBlock = stmt.executeQuery("SELECT timestamp, count FROM lockedusers WHERE user_id = " + userId + ";");
                if (selectBlock.next()) {
                    timeLastFail = selectBlock.getLong(1);
                    countOfTry = selectBlock.getInt(2);
                    isInBlocklist = true;
                    if (currentTime - timeLastFail < TIME_TO_LOCK && countOfTry == MAX_TRY_TO_AUTH) {
                        result = USER_LOCKED;
                        return result;
                    }
                }
                isSuccess = (hash == dbHash);
                ps.setInt(2, userId);
                if (!isSuccess){
                    if (!isInBlocklist){
                        stmt.executeUpdate("INSERT INTO lockedusers (user_id, timestamp, count) VALUES ("+userId+", "+currentTime+", "+"1);");
                    } else if (currentTime - timeLastFail < TIME_TO_LOCK){
                        stmt.executeUpdate("UPDATE lockedusers SET timestamp="+currentTime+", count="+(++countOfTry)+" WHERE user_id="+userId+";");
                    } else {
                        stmt.executeUpdate("UPDATE lockedusers SET timestamp="+currentTime+", count=1 WHERE user_id="+userId+";");
                    }
                    result=AUTH_FAIL;
                } else{
                    result=AUTH_OK;
                }
            } else {
                ps.setInt(2, 0);
                result = NO_USER;
            }

            ps.setBoolean(3, isSuccess);
            ps.executeUpdate();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return (AUTH_FAIL);
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}