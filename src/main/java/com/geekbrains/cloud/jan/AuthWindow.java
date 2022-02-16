package com.geekbrains.cloud.jan;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static javafx.fxml.FXMLLoader.load;

public class AuthWindow {
    public TextField login;
    public PasswordField password;
    public Button authButton;
    private static DataInputStream is;
    private static DataOutputStream os;
    private Socket socket;


    public void initializeConnection() {
        try {
            socket = new Socket("localhost", 8189);
            System.out.println("Network created...");
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void tryToAuth() {
        try {
            initializeConnection();
            //отсылаем флаг начала авторизации
            os.writeUTF("#auth#");
            //Преобразуем в массивы байт логин и хэш пароля
            os.writeUTF(login.getText());
            int passHash = password.getText().hashCode();
            os.writeInt(passHash);
            login.clear();
            password.clear();
            if (is.readUTF().equals("#authOk#")){
                FXMLLoader loader = new FXMLLoader(getClass().getResource("layout.fxml"));
                Stage stage = (Stage) authButton.getScene().getWindow();
                stage.setScene(new Scene((Pane) loader.load()));
            } else {
                System.out.println("Auth is not Ok");
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DataInputStream getIs() {
        return is;
    }

    public static DataOutputStream getOs() {
        return os;
    }
}
