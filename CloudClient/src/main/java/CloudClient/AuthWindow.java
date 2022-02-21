package CloudClient;

import CloudMessage.AuthMessage;
import CloudMessage.AuthResultMessage;
import CloudMessage.CloudMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.Socket;


public class AuthWindow {
    @FXML
    TextField login;
    @FXML
    PasswordField password;
    @FXML
    Button authButton;
    @FXML
    Label authMessage;

    private AuthResultMessage message;
    private static ObjectDecoderInputStream is;
    private static ObjectEncoderOutputStream os;
    private Socket socket;

    private static final int AUTH_OK = 0;
    private static final int AUTH_FAIL = 1;
    private static final int USER_LOCKED = 2;
    private static final int NO_USER = 3;


    public void initializeConnection() {
        try {
            socket = new Socket("localhost", 8189);
            System.out.println("Network created...");
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void tryToAuth() {
        try {
            initializeConnection();
            os.writeObject(new AuthMessage(login.getText(), password.getText().hashCode()));
            login.clear();
            password.clear();
            message=(AuthResultMessage) is.readObject();
            AuthResultMessage authResult = message;
            switch (((AuthResultMessage) message).getResult()) {
                case AUTH_OK:
                    FXMLLoader loader = new FXMLLoader(AuthWindow.class.getResource("/layout.fxml"));
                    Stage stage = (Stage) authButton.getScene().getWindow();
                    stage.setScene(new Scene((Pane) loader.load()));
                    break;
                case AUTH_FAIL:
                    authMessage.setText("Неверный пароль");
                    socket.close();
                    break;
                case USER_LOCKED:
                    authMessage.setText("Учётная запись временно заблокирована");
                    socket.close();
                    break;
                case NO_USER:
                    authMessage.setText("Введена несуществующая учётная запись");
                    socket.close();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static ObjectDecoderInputStream getIs() {
        return is;
    }

    public static ObjectEncoderOutputStream getOs() {
        return os;
    }
}
