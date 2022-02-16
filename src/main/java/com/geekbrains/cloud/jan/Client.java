package com.geekbrains.cloud.jan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;

public class Client implements Initializable {

    private static final int SIZE = 256;
    public ListView<String> clientView;
    public ListView<String> serverView;
    public Label clientCurrentDir;
    public Button clientDirUp;
    public Label serverCurrentDir;
    private String serverCurrentDirSt;
    public Button serverDirUp;
    private Path clientDir;
    private Path dirToDown;
    private byte[] buf;
    private DataInputStream is;
    private DataOutputStream os;

    // read from network

    private void readLoop() {
        try {
            while (true) {
                is = AuthWindow.getIs();
                os = AuthWindow.getOs();
                String command = is.readUTF();
                System.out.println("received: " + command);// wait message
                if (command.equals("#list#")) {
                    Platform.runLater(() -> serverView.getItems().clear());
                    serverCurrentDirSt = is.readUTF();
                    System.out.println(serverCurrentDirSt);
                    Platform.runLater(() -> serverCurrentDir.setText(serverCurrentDirSt));
                    int filesCount = is.readInt();
                    for (int i = 0; i < filesCount; i++) {
                        String fileName = is.readUTF();
                        Platform.runLater(() -> serverView.getItems().add(fileName));
                    }
                } else if (command.equals("#file#")) {
                    Sender.getFile(is, clientDir, SIZE, buf);
                    Platform.runLater(this::updateClientView);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Platform.runLater(() -> {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("AuthLayout.fxml"));
                Stage stage = (Stage) clientView.getScene().getWindow();
                try {
                    stage.setScene(new Scene((Pane) loader.load()));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
        }
    }

    private void updateClientView() {
        try {
            clientView.getItems().clear();
            Files.list(clientDir)
                    .map(p -> p.getFileName().toString())
                    .forEach(f -> clientView.getItems().add(f));
            clientCurrentDir.setText(clientDir.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            buf = new byte[SIZE];
            clientDir = Paths.get(System.getProperty("user.home"));
            updateClientView();
            System.out.println("Network created...");
            Thread readThread = new Thread(this::readLoop);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        Sender.sendFile(fileName, os, clientDir);
    }


    public void download(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        os.writeUTF("#get_file#");
        os.writeUTF(fileName);
        os.flush();
    }

    @FXML
    private void clientDirUpper() {
        if (!clientDir.equals(clientDir.getRoot())) {
            clientDir = clientDir.getParent();
            updateClientView();
        }
    }

    @FXML
    private void clientDirDown() {
        dirToDown = clientDir.resolve(clientView.getSelectionModel().getSelectedItem());
        if (isDirectory(dirToDown)) {
            clientDir = dirToDown;
            updateClientView();
        }
    }

    @FXML
    private void serverDirUpper() {
        try {
            os.writeUTF("#dirUp#");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void serverDirDown() {
        try {
            os.writeUTF("#dirDown#");
            os.writeUTF(serverView.getSelectionModel().getSelectedItem());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
