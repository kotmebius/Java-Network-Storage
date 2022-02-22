package CloudServer;

import CloudMessage.CloudMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


import CloudMessage.*;
import lombok.extern.slf4j.Slf4j;

import static java.nio.file.Files.isDirectory;

@Slf4j
@ChannelHandler.Sharable
public class CloudServerNettyHandler extends SimpleChannelInboundHandler<CloudMessage> {

    //    Константы результата авторизации
    private static final int AUTH_OK = 0;
    private static final int AUTH_FAIL = 1;
    private static final int USER_LOCKED = 2;
    private static final int NO_USER = 3;
    //    Константы для смены структуры
    private final int DELETE = 0;
    private final int RENAME = 1;
    private final int MAKE_DIR = 2;

    private int authResult;
    private Path clientDir;
    private Path rootDir;
    private String login;
    private int hash;
    boolean isAuth = false;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // init client dir
        log.info("Клиент подключился:");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                CloudMessage cloudMessage) throws Exception {
        log.info(cloudMessage.getType().toString());
        switch (cloudMessage.getType()) {
            case AUTH:
                processAuthMessage((AuthMessage) cloudMessage, ctx);
                break;
            case FILE_REQUEST:
                if (isAuth) {
                    processFileRequest((FileRequest) cloudMessage, ctx);
                }
                break;
            case FILE:
                if (isAuth) {
                    processFileMessage((FileMessage) cloudMessage);
                    sendList(ctx);
                }
                break;
            case LIST_REQUEST:
                if (isAuth){
                    processListRequest((ListRequest) cloudMessage, ctx);
                }
                break;
            case CHANGE_STRUCTURE:
                if (isAuth){
                    processChangeStructureRequest((ChangeStructureMessage) cloudMessage, ctx);
                }
                break;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // init client dir
        log.info("Клиент отключился:");
        ctx.close();
    }


    private void sendList(ChannelHandlerContext ctx) throws IOException {
        ctx.writeAndFlush(new ListMessage(clientDir));
    }

    private void processAuthMessage(AuthMessage cloudMessage, ChannelHandlerContext ctx) throws IOException {
        login = cloudMessage.getLogin();
        hash = cloudMessage.getHash();
        AuthService.connect();
        authResult = AuthService.isAuth(login, hash);
        System.out.println("Authentication result=" + authResult);
        ctx.writeAndFlush(new AuthResultMessage(authResult));
        if (authResult == AUTH_OK) {
            isAuth = true;
            rootDir = Paths.get("data").resolve(login);
            if (!rootDir.toFile().exists()) {
                rootDir.toFile().mkdir();
            }
            clientDir = rootDir;
            sendList(ctx);
        }
        AuthService.disconnect();
    }


    private void processFileMessage(FileMessage cloudMessage) throws IOException {
        if (!cloudMessage.isDir()) {
            Files.write(clientDir.resolve(cloudMessage.getFileName()), cloudMessage.getBytes(),StandardOpenOption.CREATE_NEW);
        } else {
            Files.createDirectory(clientDir.resolve(cloudMessage.getFileName()));
        }
    }



    private void processFileRequest(FileRequest cloudMessage, ChannelHandlerContext ctx) throws IOException {
        Path path = clientDir.resolve(cloudMessage.getFileName());
        upload(path, ctx);
    }

    private void upload(Path pathToUpload, ChannelHandlerContext ctx) throws IOException {
        ctx.writeAndFlush(new FileMessage(pathToUpload, clientDir));
        if (isDirectory(pathToUpload)) {
            Files.newDirectoryStream(pathToUpload)
                    .forEach(f -> {
                        if (Files.isDirectory(f)) {
                            try {
                                upload(f, ctx);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                ctx.writeAndFlush(new FileMessage(f, clientDir));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    private void processListRequest(ListRequest cloudMessage, ChannelHandlerContext ctx) throws IOException {
        if (cloudMessage.isUpper() && !clientDir.equals(rootDir)) {
            clientDir = clientDir.getParent();
            sendList(ctx);
        } else if (isDirectory(clientDir.resolve(cloudMessage.getDirName()))) {
            clientDir = clientDir.resolve(cloudMessage.getDirName());
            sendList(ctx);
        }
    }

    private void processChangeStructureRequest(ChangeStructureMessage cloudMessage, ChannelHandlerContext ctx) throws IOException {
        switch (cloudMessage.getOperation()) {
            case RENAME:
                try {
                    rename(clientDir.resolve(cloudMessage.getOldName()), clientDir.resolve(cloudMessage.getMainName()));
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
                break;
            case MAKE_DIR:
                try {
                    Files.createDirectory(clientDir.resolve(cloudMessage.getMainName()));
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
                break;
            case DELETE:
                delete(clientDir.resolve(cloudMessage.getMainName()));
                break;
        }
        sendList(ctx);
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
}
