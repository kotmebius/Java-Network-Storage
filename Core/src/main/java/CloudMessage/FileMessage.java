package CloudMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Data;

@Data
public class FileMessage implements CloudMessage {

    private final String fileName;
    private final byte[] bytes;
    private final boolean isDir;

    public FileMessage(Path path) throws IOException {
        fileName = path.getFileName().toString();
        isDir=Files.isDirectory(path);
        if (!isDir) {
            bytes = Files.readAllBytes(path);
        }  else {
            bytes=new byte[0];
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE;
    }
}
