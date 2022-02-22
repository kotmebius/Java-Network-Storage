package CloudClient;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ResourceBundle;

import CloudMessage.CloudMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import static java.nio.file.Files.isDirectory;

import CloudMessage.*;

@Slf4j
public class Client implements Initializable {

    private final int DELETE = 0;
    private final int RENAME = 1;
    private final int MAKE_DIR = 2;

    public ListView<String> clientView;
    public ListView<String> serverView;
    public Label clientCurrentDir;
    public Button clientDirUp;
    public Label serverCurrentDir;
    public TextField newName;
    public Button nameOk;
    public Button nameCancel;
    public AnchorPane namePane;
    public Label nameTitle;
    private String serverCurrentDirSt;
    public Button serverDirUp;
    private Path clientDir;
    private Path dirToDown;
    private CloudMessage message;
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;

    // read from network

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            is = AuthWindow.getIs();
            os = AuthWindow.getOs();
            clientDir = Paths.get(System.getProperty("user.home"));
            initMouseListeners();
            updateClientView();
            Thread readThread = new Thread(this::readLoop);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readLoop() {
        try {
            while (true) {
                message = (CloudMessage) is.readObject();
                log.info("received: {}", message);

                switch (message.getType()) {
                    case LIST:
                        serverCurrentDirSt = ((ListMessage) message).getServerDir();
                        Platform.runLater(() -> {
                            serverView.getItems().clear();
                            serverCurrentDir.setText(serverCurrentDirSt);
                            serverView.getItems().addAll(((ListMessage) message).getFiles());
                        });
                        break;
                    case FILE:
                        if (!(((FileMessage) message).isDir())) {
                            Files.write(clientDir.resolve(((FileMessage) message).getFileName()), ((FileMessage) message).getBytes());
                        } else {
                            Files.createDirectory(clientDir.resolve(((FileMessage) message).getFileName()));
                        }
                        updateClientView();
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Platform.runLater(() -> {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AuthLayout.fxml"));
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
        Platform.runLater(() -> {
            try {
                clientView.getItems().clear();
                Files.list(clientDir)
                        .map(p -> p.getFileName().toString())
                        .forEach(f -> clientView.getItems().add(f));
                clientCurrentDir.setText(clientDir.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    @FXML
    public void uploadButton(ActionEvent actionEvent) throws IOException {
        if (!clientView.getSelectionModel().isEmpty()) {
            String fileName = clientView.getSelectionModel().getSelectedItem();
            upload(clientDir.resolve(fileName));
        }
    }

    private void upload(Path pathToUpload) throws IOException {
        os.writeObject(new FileMessage(pathToUpload, clientDir));
        if (isDirectory(pathToUpload)) {
            Files.newDirectoryStream(pathToUpload)
                    .forEach(f -> {
                        if (Files.isDirectory(f)) {
                            try {
                                upload(f);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                os.writeObject(new FileMessage(f, clientDir));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }


    @FXML
    public void downloadButton(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        os.writeObject(new FileRequest(fileName));


    }

    private void initMouseListeners() {

        clientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                dirToDown = clientDir.resolve(clientView.getSelectionModel().getSelectedItem());
                if (isDirectory(dirToDown)) {
                    clientDir = dirToDown;
                    updateClientView();
                }
            }
        });

        serverView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                try {
                    os.writeObject(new ListRequest(false, serverView.getSelectionModel().getSelectedItem()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }


    @FXML
    private void clientDirUpper() {
        if (!clientDir.equals(clientDir.getRoot())) {
            clientDir = clientDir.getParent();
            updateClientView();
        }
    }


    @FXML
    private void serverDirUpper() throws IOException {
        os.writeObject (new ListRequest(true));
    }

    @FXML
    public void renameClient(ActionEvent actionEvent) {
        if (!clientView.getSelectionModel().isEmpty()) {
            String oldName = clientView.getSelectionModel().getSelectedItem();
            Platform.runLater(() -> {
                nameTitle.setText("Enter new name");
                newName.setText(oldName);
                namePane.setVisible(true);
            });
            nameOk.setOnMouseClicked(e -> {
                if (!newName.getText().equals("")) {
                    try {
                        rename(clientDir.resolve(oldName), clientDir.resolve(newName.getText()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    namePane.setVisible(false);
                    updateClientView();
                }
            });
            nameCancel.setOnMouseClicked(e -> {
                namePane.setVisible(false);
            });
        }
    }

    private void rename(Path oldName, Path newName) throws IOException {
        Files.copy(oldName, newName);
        if (isDirectory(oldName)) {
            Files.newDirectoryStream(oldName)
                    .forEach(f -> {
                        if (Files.isDirectory(f)) {
                            try {
                                rename(oldName.resolve(f.getFileName()), newName.resolve(f.getFileName()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                Files.copy(oldName.resolve(f.getFileName()), newName.resolve(f.getFileName()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
        delete(oldName);
    }

    @FXML
    public void mkDirClient(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            newName.setText("");
            nameTitle.setText("Enter name for new dir");
            namePane.setVisible(true);
        });
        nameOk.setOnMouseClicked(e -> {
            if (!newName.getText().equals("")) {
                try {
                    Files.createDirectory(clientDir.resolve(newName.getText()));
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
                namePane.setVisible(false);
                updateClientView();
            }
        });
        nameCancel.setOnMouseClicked(e -> {
            namePane.setVisible(false);
        });
    }


    @FXML
    public void deleteClient(ActionEvent actionEvent) {
        if (!clientView.getSelectionModel().isEmpty()) {
            Platform.runLater(() -> {
                newName.setVisible(false);
                nameTitle.setText("Are you shure");
                namePane.setVisible(true);
            });
            nameOk.setOnMouseClicked(e -> {
                delete(clientDir.resolve(clientView.getSelectionModel().getSelectedItem()));
                namePane.setVisible(false);
                newName.setVisible(true);
                updateClientView();
            });
            nameCancel.setOnMouseClicked(e -> {
                namePane.setVisible(false);
                newName.setVisible(true);
            });
        }
    }

    private void delete(Path pathToDelete) {
        try {
            Files.walkFileTree(pathToDelete, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    @FXML
    public void renameSrv(ActionEvent actionEvent) throws IOException {
        if (!serverView.getSelectionModel().isEmpty()) {
            String oldName = serverView.getSelectionModel().getSelectedItem();
            Platform.runLater(() -> {
                nameTitle.setText("Enter new name");
                newName.setText(oldName);
                namePane.setVisible(true);
            });
            nameOk.setOnMouseClicked(e -> {
                if (!newName.getText().equals("")) {
                    try {
                        os.writeObject (new ChangeStructureMessage(newName.getText(), oldName, RENAME));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    namePane.setVisible(false);
                }
            });
            nameCancel.setOnMouseClicked(e -> {
                namePane.setVisible(false);
            });
        }
    }

    public void mkDirSrv(ActionEvent actionEvent) throws IOException {
        Platform.runLater(() -> {
            newName.setText("");
            nameTitle.setText("Enter name for new dir");
            namePane.setVisible(true);
        });
        nameOk.setOnMouseClicked(e -> {
            if (!newName.getText().equals("")) {
                try {
                    os.writeObject(new ChangeStructureMessage(newName.getText(), MAKE_DIR));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                namePane.setVisible(false);
            }
        });
        nameCancel.setOnMouseClicked(e -> {
            namePane.setVisible(false);
        });
    }

    public void deleteSrv(ActionEvent actionEvent) throws IOException {
        if (!serverView.getSelectionModel().isEmpty()) {
            Platform.runLater(() -> {
                newName.setVisible(false);
                nameTitle.setText("Are you shure");
                namePane.setVisible(true);
            });
            nameOk.setOnMouseClicked(e -> {
                try {
                    os.writeObject(new ChangeStructureMessage(serverView.getSelectionModel().getSelectedItem(), DELETE));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                namePane.setVisible(false);
                newName.setVisible(true);
            });
            nameCancel.setOnMouseClicked(e -> {
                namePane.setVisible(false);
                newName.setVisible(true);
            });
        }
    }
}
